package org.example.cpt202music.model.dto.email;

import lombok.Data;

@Data
public class EmailRequest {
    private String email;
    private String code;
} 