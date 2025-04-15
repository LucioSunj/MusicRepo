package org.example.cpt202music.model.dto.MusicFile;

import lombok.Data;

import java.io.Serializable;
import java.util.List;


/**
 * 编辑请求
 * 一般是给用户使用
 *
 * @author cpt202
 * @description 编辑请求
 * @date 2022/05/05
 */
@Data
public class MusicFileEditRequest implements Serializable {

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

    private static final long serialVersionUID = 1L;
}
