package org.example.cpt202music.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserBanRequest implements Serializable {
    
    /**
     * 用户id
     */
    private Long id;
    
    /**
     * 封禁原因
     */
    private String banReason;
    
    private static final long serialVersionUID = 1L;
}