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
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private String tags;

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

    /**
     * 封面图片 id
     */
    private String coverUrl;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;


    /**
     * 状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人 id
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private Date reviewTime;




    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;
}