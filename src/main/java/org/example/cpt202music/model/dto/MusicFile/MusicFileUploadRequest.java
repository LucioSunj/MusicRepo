package org.example.cpt202music.model.dto.MusicFile;

import lombok.Data;

import java.io.Serializable;

@Data
public class MusicFileUploadRequest implements Serializable {
  
    /**  
     * music id（用于修改）
     */  
    private Long id;

    // 添加封面参数，用于修改时传递
    private String coverUrl;
  
    private static final long serialVersionUID = 1L;  
}
