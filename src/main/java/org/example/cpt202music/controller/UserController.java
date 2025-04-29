package org.example.cpt202music.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.cpt202music.annotation.AuthCheck;
import org.example.cpt202music.common.BaseResponse;
import org.example.cpt202music.common.DeleteRequest;
import org.example.cpt202music.common.ResultUtils;
import org.example.cpt202music.constant.UserConstant;
import org.example.cpt202music.exception.BusinessException;
import org.example.cpt202music.exception.ErrorCode;
import org.example.cpt202music.exception.ThrowUtils;
import org.example.cpt202music.manager.FileManager;
import org.example.cpt202music.model.dto.user.*;
import org.example.cpt202music.model.entity.User;
import org.example.cpt202music.model.vo.LoginUserVO;
import org.example.cpt202music.model.vo.UserVO;
import org.example.cpt202music.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private FileManager fileManager;

    /**
     * User Register
     */
    @PostMapping("/register")
    public BaseResponse<UserVO> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // This is your own utility class
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String email = userRegisterRequest.getEmail();
        String code = userRegisterRequest.getCode();
        
        // It does not include the upload of avatars, and the upload is null
        long userId = userService.userRegister(userAccount, userPassword, checkPassword, email, code, null);
        User user = userService.getById(userId);
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * Upload a user profile picture
     */
    @PostMapping("/upload/avatar")
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        
        try {
            // Obtain the current logged-in user
            String avatarUrl = fileManager.uploadImage(file, "avatar/" + loginUser.getUserAccount());
            
            // Update the user's avatar
            User user = new User();
            user.setId(loginUser.getId());
            user.setUserAvatar(avatarUrl);
            boolean result = userService.updateById(user);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新头像失败");
            
            return ResultUtils.success(avatarUrl);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传失败");
        }
    }

    /**
     * User Login
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // This is your own utility class
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();

        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }


    /**
     * Get the current user to be desensitized
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }


    /**
     * The user is logged in
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR );
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }


    /**
     * Create a user, the logic here is purely very simple, not written in the service, it is very simple to add, delete, modify and check
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * Get users based on ID (admins only)
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        // 添加日志输出banNumber值
        System.out.println("用户 " + id + " 的banNumber: " + user.getBanNumber());
        return ResultUtils.success(user);
    }

    /**
     * Get the wrapper class based on ID for the average user
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        // The logic here is that I call the above to get the user and then desensitize
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * Delete the user
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * Update User
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        // Salt value, obfuscated passwords
        final String SALT = "wyf_da_niu_niu";
        String userPassword = userUpdateRequest.getUserPassword();

        BeanUtils.copyProperties(userUpdateRequest, user);
        if (userPassword != null) {
            user.setUserPassword(DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes()));
        }
//        user.setUserPassword(DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes()));
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * Pagination to get a list of user encapsulations (admins only)
     *
     * @param userQueryRequest 查询请求参数
     */
    // Its practical get request here can also be used, but here it is to receive an object, and it is more standardized to use post, and the range of parameters that can be passed by post is larger
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    // 这里的Page是mybatis为我们封装好的 ??????
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }



    /**
     * Ban a user and log the reason (admins only)
     */
    @PostMapping("/ban/with-reason")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> banUserWithReason(@RequestBody UserBanRequest request) {
        if (request == null || request.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        user.setId(request.getId());
        user.setUser_status(1);
        user.setBanReason(request.getBanReason());
        boolean result = userService.updateById(user);
        return ResultUtils.success(result);
    }

    /**
     * Upload an avatar based on user ID (no login required)
     */
    @PostMapping("/upload/avatar/by-id")
    public BaseResponse<String> uploadAvatarById(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId) {
        
        // Verify the user ID
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "User ID invaild: " + userId);
        
        // Obtain user information
        User user = userService.getById(userId);
        System.out.println("User ID requesting to upload an avatar: " + userId + ", Result: " + (user != null ? "User exist" : "User dont exist"));
        
        // If the user is not present, try querying all users for debugging
        if (user == null) {
            List<User> allUsers = userService.list();
            System.out.println("The user Id all in this system are:  " +
                allUsers.stream().map(User::getId).collect(Collectors.toList()));
        }
        
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "User doesnt exist，ID: " + userId);
        
        try {
            // Upload your profile picture
            String avatarUrl = fileManager.uploadImage(file, "avatar/" + user.getUserAccount());
            
            // Update the user's avatar
            User updateUser = new User();
            updateUser.setId(userId);
            updateUser.setUserAvatar(avatarUrl);
            boolean result = userService.updateById(updateUser);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新头像失败");
            
            return ResultUtils.success(avatarUrl);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传失败：" + e.getMessage());
        }
    }

    /**
     * Upload an avatar based on the user account (no login required)
     */
    @PostMapping("/upload/avatar/by-account")
    public BaseResponse<String> uploadAvatarByAccount(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userAccount") String userAccount) {
        
        // Verify your account
        ThrowUtils.throwIf(userAccount == null || userAccount.isEmpty(), ErrorCode.PARAMS_ERROR);
        
        // Find users based on their accounts
        User user = userService.lambdaQuery().eq(User::getUserAccount, userAccount).one();
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "User doesnt exist");
        
        try {
            // Upload your profile picture
            String avatarUrl = fileManager.uploadImage(file, "avatar/" + userAccount);
            
            // Update the user's avatar
            User updateUser = new User();
            updateUser.setId(user.getId());
            updateUser.setUserAvatar(avatarUrl);
            boolean result = userService.updateById(updateUser);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "Failed to update user's avatar");
            
            return ResultUtils.success(avatarUrl);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to upload user's avatar" + e.getMessage());
        }
    }

    /**
     * 增加用户的被封禁资源数量
     */
    @PostMapping("/increase/ban-number")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> increaseUserBanNumber(@RequestParam("userId") Long userId) {
        // 验证参数
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        
        // 查询用户是否存在
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        
        // 创建更新对象
        User updateUser = new User();
        updateUser.setId(userId);
        
        // 将当前的banNumber值+1
        Integer currentBanNumber = user.getBanNumber() != null ? user.getBanNumber() : 0;
        updateUser.setBanNumber(currentBanNumber + 1);
        
        // 更新用户信息
        boolean result = userService.updateById(updateUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新失败");
        
        return ResultUtils.success(true);
    }
}
