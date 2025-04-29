package org.example.cpt202music.model.entity;



import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    private String userAccount;


    private String userPassword;


    private String email;


    private String userName;


    private String userAvatar;

    private String userProfile;


    private String userRole;


    private Date editTime;


    private Date createTime;


    private Date updateTime;


    @TableLogic
    private Integer isDelete;



    private Integer user_status;


    private String banReason;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
