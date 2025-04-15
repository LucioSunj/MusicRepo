package org.example.cpt202music.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum MusicFileReviewStatusEnum {
    REVIEWING("待审核", 0),  
    PASS("通过", 1),  
    REJECT("拒绝", 2);  
  
    private final String text;  
    private final int value;

    MusicFileReviewStatusEnum(String text, int value) {
        this.text = text;  
        this.value = value;  
    }  
  
    /**  
     * 根据 value 获取枚举  
     */  
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
