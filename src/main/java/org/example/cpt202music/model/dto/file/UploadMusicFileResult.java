package org.example.cpt202music.model.dto.file;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.util.Date;


/**
 * 创建文件结果
 */
@Data
public class UploadMusicFileResult {
    /**
     * 音乐文件 url
     */
    private String url;

    /**
     * 音乐名称
     */
    private String name;

    /**
     * 艺术家
     */
    private String artist;

    /**
     * 专辑
     */
    private String album;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 时长（秒）
     */
    private Integer duration;

    /**
     * 比特率
     */
    private Integer bitRate;

    /**
     * 文件格式
     */
    private String fileFormat;



}
