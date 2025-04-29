package org.example.cpt202music.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserBanRequest implements Serializable {
    

    private Long id;
    

    private String banReason;
    
    private static final long serialVersionUID = 1L;
}