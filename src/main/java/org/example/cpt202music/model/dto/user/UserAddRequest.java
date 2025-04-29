package org.example.cpt202music.model.dto.user;

import lombok.Data;

import java.io.Serializable;


/**
 * 用户创建请求，这个是给管理员使用
 */
@Data
public class UserAddRequest implements Serializable {


    private String userName;

    private String userAccount;

    private String userAvatar;


    private String userProfile;


    private String userRole;

    private static final long serialVersionUID = 1L;
}
