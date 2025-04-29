package org.example.cpt202music.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 音乐文件
 * @TableName music_file
 */
@TableName(value ="music_file")
@Data
public class MusicFile {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    private String url;

    private String name;


    private String artist;


    private String album;


    private String introduction;


    private String category;


    private String tags;

    private Long fileSize;


    private Integer duration;


    private Integer bitRate;


    private String fileFormat;

    
    private String coverUrl;


    private Long userId;


    private Date createTime;


    private Date editTime;


    private Date updateTime;



    private Integer reviewStatus;

    private String reviewMessage;


    private Long reviewerId;


    private Date reviewTime;





    @TableLogic
    private Integer isDelete;
}