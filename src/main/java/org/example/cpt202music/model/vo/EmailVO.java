package org.example.cpt202music.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 邮箱验证码视图对象
 */
@Data
public class EmailVO implements Serializable {

    /**
     * 邮箱地址
     */
    private String email;

    /**
     * 验证码
     */
    private String code;

    /**
     * 验证码状态：0-未使用，1-已使用
     */
    private Integer status;

    /**
     * 验证码过期时间
     */
    private Date expireTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
} 