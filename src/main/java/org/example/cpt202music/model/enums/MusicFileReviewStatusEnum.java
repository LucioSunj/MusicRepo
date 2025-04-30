package org.example.cpt202music.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum MusicFileReviewStatusEnum {
    REVIEWING("Reviewing", 0),
    PASS("Pass", 1),
    REJECT("Rejected", 2);
  
    private final String text;  
    private final int value;

    MusicFileReviewStatusEnum(String text, int value) {
        this.text = text;  
        this.value = value;  
    }  
  

    public static MusicFileReviewStatusEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;  
        }
        for (MusicFileReviewStatusEnum musicFileReviewStatusEnum : MusicFileReviewStatusEnum.values()) {
            if (musicFileReviewStatusEnum.value == value) {
                return musicFileReviewStatusEnum;
            }  
        }  
        return null;  
    }  
}
