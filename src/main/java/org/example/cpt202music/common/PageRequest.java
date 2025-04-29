package org.example.cpt202music.common;

import lombok.Data;


// A generic pagination request class
@Data
public class PageRequest {


    private int current = 1;


    private int pageSize = 10;


    private String sortField;


    private String sortOrder = "descend";
}
