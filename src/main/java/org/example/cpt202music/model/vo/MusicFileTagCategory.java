package org.example.cpt202music.model.vo;


import lombok.Data;

import java.util.List;

@Data
public class MusicFileTagCategory {
    private List<String> tagList;

    private List<String> categoryList;
}
