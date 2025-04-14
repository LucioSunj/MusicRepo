package org.example.cpt202music.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.cpt202music.model.dto.user.UserQueryRequest;
import org.example.cpt202music.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.cpt202music.model.vo.LoginUserVO;
import org.example.cpt202music.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author XLW200420
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-03-27 20:29:34
*/
public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String email, String code);
    String getEncryptPassword(String userPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息，很多数据不用返回给前端
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);


    // 获取当前用户，返回给后端的部分
    User getLoginUser(HttpServletRequest request);


    /**
     * 获取脱敏后的用户登录信息
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);


    /**
     * 获取脱敏后的用户信息
     * @param user
     * @return
     */
    UserVO getUserVO(User user);



    /**
     * 获取脱敏后的用户信息列表
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);



    /**
     * 用户登录态注销
     * @param request
     * @return  脱敏后的用户列表
     */
    boolean userLogout(HttpServletRequest request);


    /**
     * 获取查询条件
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
}
