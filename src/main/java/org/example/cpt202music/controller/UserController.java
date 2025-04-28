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
     * 用户注册
     */
    @PostMapping("/register")
    public BaseResponse<UserVO> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 这里用到的是自己的工具类
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String email = userRegisterRequest.getEmail();
        String code = userRegisterRequest.getCode();
        
        // 不包含头像上传，传null
        long userId = userService.userRegister(userAccount, userPassword, checkPassword, email, code, null);
        User user = userService.getById(userId);
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 上传用户头像
     */
    @PostMapping("/upload/avatar")
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        
        try {
            // 上传头像
            String avatarUrl = fileManager.uploadImage(file, "avatar/" + loginUser.getUserAccount());
            
            // 更新用户头像
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
     * 用户登录
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 这里用到的是自己的工具类
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();

        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }


    /**
     * 获取当前用户经过脱敏
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }


    /**
     * 用户取消登录态
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
     * 创建用户，这里写逻辑纯属是逻辑非常简单，没有写在service里面，就是很简单的增删改查
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
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类 给普通的用户使用
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        // 这里的逻辑是我调用上面的得到了user之后进行脱敏即可
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
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
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        // 盐值，混淆密码
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
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    // 这里其实用get请求也可以，只不过这里是为了接收一个对象，用post更加规范，post能够传的参数范围更大
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
     * 封禁用户并记录原因（仅管理员）
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
     * 根据用户ID上传头像（不需要登录）
     */
    @PostMapping("/upload/avatar/by-id")
    public BaseResponse<String> uploadAvatarById(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId) {
        
        // 验证用户ID
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID无效: " + userId);
        
        // 获取用户信息
        User user = userService.getById(userId);
        System.out.println("请求上传头像的用户ID: " + userId + ", 查询结果: " + (user != null ? "用户存在" : "用户不存在"));
        
        // 如果用户不存在，尝试查询所有用户以进行调试
        if (user == null) {
            List<User> allUsers = userService.list();
            System.out.println("当前系统中的所有用户ID: " + 
                allUsers.stream().map(User::getId).collect(Collectors.toList()));
        }
        
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在，ID: " + userId);
        
        try {
            // 上传头像
            String avatarUrl = fileManager.uploadImage(file, "avatar/" + user.getUserAccount());
            
            // 更新用户头像
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
     * 根据用户账号上传头像（不需要登录）
     */
    @PostMapping("/upload/avatar/by-account")
    public BaseResponse<String> uploadAvatarByAccount(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userAccount") String userAccount) {
        
        // 验证账号
        ThrowUtils.throwIf(userAccount == null || userAccount.isEmpty(), ErrorCode.PARAMS_ERROR);
        
        // 根据账号查找用户
        User user = userService.lambdaQuery().eq(User::getUserAccount, userAccount).one();
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        
        try {
            // 上传头像
            String avatarUrl = fileManager.uploadImage(file, "avatar/" + userAccount);
            
            // 更新用户头像
            User updateUser = new User();
            updateUser.setId(user.getId());
            updateUser.setUserAvatar(avatarUrl);
            boolean result = userService.updateById(updateUser);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新头像失败");
            
            return ResultUtils.success(avatarUrl);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传失败：" + e.getMessage());
        }
    }
}
