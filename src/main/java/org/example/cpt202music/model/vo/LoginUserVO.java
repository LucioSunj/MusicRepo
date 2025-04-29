package org.example.cpt202music.model.vo;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 已登录用户视图（已脱敏）
 * @TableName user
 */

@Data
public class LoginUserVO implements Serializable {
    /**
     * id
     */
    private Long id;


    private String userAccount;



    private String userName;


    private String userAvatar;


    private String userProfile;


    private String userRole;


    private Date editTime;

    private Date createTime;

 
    private Date updateTime;




    private static final long serialVersionUID = 1L;
}
