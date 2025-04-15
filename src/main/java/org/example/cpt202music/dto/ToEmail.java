package org.example.cpt202music.dto;

import lombok.Data;

@Data
public class ToEmail {
    /**
     * Email recipients, separated by commas for multiple recipients.
     */
    private String tos;
    private String subject;
    private String content;
} 