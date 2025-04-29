package org.example.cpt202music.model.dto.MusicFile;

import lombok.Data;

import java.io.Serializable;


@Data
public class MusicFileReviewRequest implements Serializable {


    /**
     * id
     */
    private long id;

    /**
     * Status: 0-Pending review, 1-Approved, 2-Rejected
     */
    private Integer reviewStatus;


    /**
     * Review message
     */
    private String reviewMessage;
}
