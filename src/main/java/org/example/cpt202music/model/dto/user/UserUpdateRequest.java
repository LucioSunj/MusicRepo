package org.example.cpt202music.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新用户信息，这里也是给管理员的，删除请求类在common中，通用删除请求
 */
@Data
public class UserUpdateRequest implements Serializable {


    private Long id;


    private String userName;


    private String userPassword;


    private String userAvatar;


    private String userProfile;


    private String userRole;


    private Integer user_status;

    private static final long serialVersionUID = 1L;
}
