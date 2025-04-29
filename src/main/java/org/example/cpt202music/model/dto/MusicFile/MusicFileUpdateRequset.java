package org.example.cpt202music.model.dto.MusicFile;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Update Request
 */
@Data
public class MusicFileUpdateRequset implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * name
     */
    private String name;

    private String artist;

    /**
     * introduction
     */
    private String introduction;

    /**
     * category
     */
    private String category;

    /**
     * tags
     */
    private List<String> tags;

    private static final long serialVersionUID = 1L;
}
