package org.example.cpt202music.model.dto.email;

import lombok.Data;

@Data
public class EmailRequest {
    /**
     * Email
     */
    private String email;

    /**
     * verify code
     */
    private String code;
} 