package org.example.cpt202music.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新用户信息，这里也是给管理员的，删除请求类在common中，通用删除请求
 */
@Data
public class UserUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户密码
     */
    private String userPassword;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 用户状态：0-正常，1-被封禁
     */
    private Integer user_status;

    private static final long serialVersionUID = 1L;
}
