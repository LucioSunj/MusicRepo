package org.example.cpt202music.model.dto.email;

import lombok.Data;

@Data
public class EmailRequest {
    /**
     * 邮箱
     */
    private String email;

    /**
     * 验证码
     */
    private String code;
} 