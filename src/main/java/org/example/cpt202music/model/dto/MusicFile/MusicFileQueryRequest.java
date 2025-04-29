package org.example.cpt202music.model.dto.MusicFile;


import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.cpt202music.common.PageRequest;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class MusicFileQueryRequest  extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * name
     */
    private String name;

    /**
     * intro
     */
    private String introduction;

    /**
     * category
     */
    private String category;

    /**
     * tag
     */
    private List<String> tags;

    /**
     * filesize
     */
    private Long fileSize;

    /**
     * duration
     */
    private Integer duration;

    /**
     * fileFormat
     */
    private String fileFormat;

    /**
     * cover id
     */
    private Long coverId;


    /**
     * userId
     */
    private Long userId;

    /**
     * bitrate
     */
    private Integer bitRate;

    /**
     * Search for text (while searching for name, description, etc.)
     */
    private String searchText;



    /**
     * Status: 0-Pending review; 1-Passed; 2-Rejected
     */
    private Integer reviewStatus;

    /**
     * Audit information
     */
    private String reviewMessage;

    /**
     * reviewer id
     */
    private Long reviewerId;

    /**
     * reviewTime
     */
    private Date reviewTime;

    private static final long serialVersionUID = 1L;
}
