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
     * 状态： 0-待审核，1-审核通过，2-审核不通过
     */
    private Integer reviewStatus;


    /**
     * 审核信息
     */
    private String reviewMessage;
}
