// 1. 首先创建一个新的包装类
package org.example.cpt202music.model.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class MusicFilePlaylistsVO {
    private Map<String, List<MusicFileVO>> playlists;
    
    public MusicFilePlaylistsVO(Map<String, List<MusicFileVO>> playlists) {
        this.playlists = playlists;
    }
}