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
     * 名字
     */
    private String name;

    /**
     * 介绍
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 时长（秒）
     */
    private Integer duration;

    /**
     * 文件格式
     */
    private String fileFormat;

    /**
     * 封面图片 id
     */
    private Long coverId;


    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 比特率
     */
    private Integer bitRate;

    /**
     * 搜索文本(同时搜索名称，简介等)
     */
    private String searchText;



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

    private static final long serialVersionUID = 1L;
}
