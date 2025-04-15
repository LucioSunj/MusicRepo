package org.example.cpt202music.model.vo;


import cn.hutool.json.JSONUtil;
import io.github.classgraph.json.JSONUtils;
import lombok.Data;
import org.example.cpt202music.model.entity.MusicFile;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class MusicFileVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * url
     */
    private String url;

    /**
     * name
     */
    private String name;

    /**
     * artist
     */
    private String artist;

    /**
     * album
     */
    private String album;

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

    /**
     * fileSize
     */
    private Long fileSize;

    /**
     * duration
     */
    private Long duration;

    /**
     * bitRate
     */
    private Integer bitRate;

    /**
     * fileFormat
     */
    private String fileFormat;

    /**
     * 封面URL
     */
    private String coverUrl;

    /**
     * userId
     */
    private Long userId;

    /**
     * createTime
     */
    private Date createTime;

    /**
     * editTime
     */
    private Date editTime;


    /**
     * updateTime
     */
    private Date updateTime;

    /**
     * 创建用户信息
     */
    private UserVO user;


    private static final long serialVersionUID = 1L;


    /**
     * 封装类转对象
     */
    public static MusicFile voToObj(MusicFileVO musicFileVo) {
        if (musicFileVo == null){
            return null;
        }
        MusicFile musicFile = new MusicFile();
        BeanUtils.copyProperties(musicFileVo, musicFile);
        // 类型不同，需要转换  ????
        musicFile.setTags(JSONUtil.toJsonStr(musicFileVo.getTags()));
        return musicFile;
    }

    /**
     * 对象转封装类
     */
    public static MusicFileVO objToVo(MusicFile musicFile) {
        if (musicFile == null) {
            return null;
        }
        
        MusicFileVO musicFileVO = new MusicFileVO();
        musicFileVO.setId(musicFile.getId());
        musicFileVO.setName(musicFile.getName());
        musicFileVO.setUrl(musicFile.getUrl());
        musicFileVO.setArtist(musicFile.getArtist());
        musicFileVO.setAlbum(musicFile.getAlbum());
        musicFileVO.setIntroduction(musicFile.getIntroduction());
        musicFileVO.setFileSize(musicFile.getFileSize());
        musicFileVO.setDuration(Long.valueOf(musicFile.getDuration()));
        musicFileVO.setBitRate(musicFile.getBitRate());
        musicFileVO.setFileFormat(musicFile.getFileFormat());
        musicFileVO.setUserId(musicFile.getUserId());
        musicFileVO.setCreateTime(musicFile.getCreateTime());
        musicFileVO.setUpdateTime(musicFile.getUpdateTime());
        musicFileVO.setEditTime(musicFile.getEditTime());
        musicFileVO.setCoverUrl(musicFile.getCoverUrl());  // 设置coverUrl
        
        return musicFileVO;
    }
}
