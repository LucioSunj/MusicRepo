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
     * Upload object
     *
     * @param key  Unique key
     * @param file File
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }


    /**
     * Download object
     *
     * @param key Unique key
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }




    /**
     * Upload object (with audio information)
     *
     * @param key  Unique key
     * @param file Audio file
     */
    public PutObjectResult putAudioObject(String key, File file) {
        // Create metadata object
        ObjectMetadata metadata = new ObjectMetadata();

        // Use jaudiotagger to get audio file information
        try {
            // Parse audio file
            AudioFile audioFile = AudioFileIO.read(file);
            AudioHeader audioHeader = audioFile.getAudioHeader();
            Tag tag = audioFile.getTag();

            // Get basic audio information
            int bitRate = (int) audioHeader.getBitRateAsNumber(); // Bit rate
            int sampleRate = audioHeader.getSampleRateAsNumber(); // Sample rate
            int length = audioHeader.getTrackLength(); // Duration (seconds)
            String format = audioHeader.getFormat(); // Format
            String encoding = audioHeader.getEncodingType(); // Encoding type

            // Get audio tag information
            String title = tag != null ? tag.getFirst("TITLE") : "";
            String artist = tag != null ? tag.getFirst("ARTIST") : "";
            String album = tag != null ? tag.getFirst("ALBUM") : "";

            // Set user-defined metadata
            Map<String, String> userMetadata = new HashMap<>();
            userMetadata.put("audio-bitrate", String.valueOf(bitRate));
            userMetadata.put("audio-duration", String.valueOf(length));
            userMetadata.put("audio-format", format);
            userMetadata.put("audio-encoding", encoding);
            userMetadata.put("audio-title", title);
            userMetadata.put("audio-artist", artist);
            userMetadata.put("audio-album", album);

            metadata.setUserMetadata(userMetadata);

            // Print audio information
            System.out.println("Audio information: Format=" + format + ", Duration=" + length + "s, Bit rate=" + bitRate + "kbps");

        } catch (Exception e) {
            // Handle exception but don't block the upload
            System.out.println("Failed to get audio information: " + e.getMessage());
        }

        // Create upload request with metadata
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        putObjectRequest.withMetadata(metadata);  // Use withMetadata method to set metadata

        // Upload file
        return cosClient.putObject(putObjectRequest);
    }




}
