package org.example.cpt202music.model.dto.MusicFile;

import lombok.Data;

import java.io.Serializable;
import java.util.List;


/**
 * Edit request
 * Generally used by users
 *
 * @author cpt202
 * @description Edit request
 * @date 2022/05/05
 */
@Data
public class MusicFileEditRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private String artist;

    /**
     * Name
     */
    private String name;

    /**
     * Introduction
     */
    private String introduction;

    /**
     * Category
     */
    private String category;

    /**
     * Tags
     */
    private List<String> tags;

    private static final long serialVersionUID = 1L;
}
