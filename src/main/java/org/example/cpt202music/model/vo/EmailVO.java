package org.example.cpt202music.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;


@Data
public class EmailVO implements Serializable {


    private String email;


    private String code;

    /**
     * Captcha status: 0 - Unused, 1 - Used
     */
    private Integer status;

    /**
     * The expiration time of the verification code
     */
    private Date expireTime;


    private Date createTime;


    private Date updateTime;

    private static final long serialVersionUID = 1L;
} 