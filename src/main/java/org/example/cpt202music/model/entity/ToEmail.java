package org.example.cpt202music.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class ToEmail implements Serializable {

    //Email accept

    private String tos;

    //Email theme

    private String subject;




    private String content;
}
