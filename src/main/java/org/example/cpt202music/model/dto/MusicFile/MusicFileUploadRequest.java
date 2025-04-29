package org.example.cpt202music.model.dto.MusicFile;

import lombok.Data;

import java.io.Serializable;

@Data
public class MusicFileUploadRequest implements Serializable {
  
    /**  
     * music id
     */  
    private Long id;


    private String coverUrl;
  
    private static final long serialVersionUID = 1L;  
}
