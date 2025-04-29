package org.example.cpt202music.model.dto.file;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.util.Date;


/**
 * File creation result
 */
@Data
public class UploadMusicFileResult {
    /**
     * Music file url
     */
    private String url;

    /**
     * Music name
     */
    private String name;

    /**
     * Artist
     */
    private String artist;

    /**
     * Album
     */
    private String album;

    /**
     * Introduction
     */
    private String introduction;

    /**
     * File size
     */
    private Long fileSize;

    /**
     * Duration (seconds)
     */
    private Integer duration;

    /**
     * Bit rate
     */
    private Integer bitRate;

    /**
     * File format
     */
    private String fileFormat;



}
