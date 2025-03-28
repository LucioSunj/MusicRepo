package org.example.cpt202music.model.vo;


import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户视图（已脱敏），给管理员看的
 * @TableName user
 */

@Data
public class UserVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;



    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;


    /**
     * 创建时间
     */
    private Date createTime;





    private static final long serialVersionUID = 1L;
}
