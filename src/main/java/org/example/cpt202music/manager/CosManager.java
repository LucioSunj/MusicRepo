package org.example.cpt202music.manager;


import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaInfoRequest;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaInfoResponse;
import org.example.cpt202music.config.CosClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;


import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;


    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }


    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }




    /**
     * 上传对象（附带音频信息）
     *
     * @param key  唯一键
     * @param file 音频文件
     */
    public PutObjectResult putAudioObject(String key, File file) {
        // 创建元数据对象
        ObjectMetadata metadata = new ObjectMetadata();

        // 使用jaudiotagger获取音频文件信息
        try {
            // 解析音频文件
            AudioFile audioFile = AudioFileIO.read(file);
            AudioHeader audioHeader = audioFile.getAudioHeader();
            Tag tag = audioFile.getTag();

            // 获取音频基本信息
            int bitRate = (int) audioHeader.getBitRateAsNumber(); // 比特率
            int sampleRate = audioHeader.getSampleRateAsNumber(); // 采样率
            int length = audioHeader.getTrackLength(); // 时长(秒)
            String format = audioHeader.getFormat(); // 格式
            String encoding = audioHeader.getEncodingType(); // 编码类型

            // 获取音频标签信息
            String title = tag != null ? tag.getFirst("TITLE") : "";
            String artist = tag != null ? tag.getFirst("ARTIST") : "";
            String album = tag != null ? tag.getFirst("ALBUM") : "";

            // 设置用户自定义元数据
            Map<String, String> userMetadata = new HashMap<>();
            userMetadata.put("audio-bitrate", String.valueOf(bitRate));
            userMetadata.put("audio-duration", String.valueOf(length));
            userMetadata.put("audio-format", format);
            userMetadata.put("audio-encoding", encoding);
            userMetadata.put("audio-title", title);
            userMetadata.put("audio-artist", artist);
            userMetadata.put("audio-album", album);

            metadata.setUserMetadata(userMetadata);

            // 打印音频信息
            System.out.println("音频信息: 格式=" + format + ", 时长=" + length + "秒, 比特率=" + bitRate + "kbps");

        } catch (Exception e) {
            // 处理异常但不阻止上传
            System.out.println("获取音频信息失败: " + e.getMessage());
        }

        // 创建带有元数据的上传请求
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        putObjectRequest.withMetadata(metadata);  // 使用withMetadata方法设置元数据

        // 上传文件
        return cosClient.putObject(putObjectRequest);
    }




}
