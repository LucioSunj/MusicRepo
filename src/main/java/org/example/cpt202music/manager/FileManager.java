package org.example.cpt202music.manager;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.example.cpt202music.config.CosClientConfig;
import org.example.cpt202music.exception.BusinessException;
import org.example.cpt202music.exception.ErrorCode;
import org.example.cpt202music.exception.ThrowUtils;
import org.example.cpt202music.model.dto.file.UploadMusicFileResult;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.springframework.util.StringUtils;


@Slf4j
@Service
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * Upload audio file
     *
     * @param multipartFile    Audio file
     * @param uploadPathPrefix Upload path prefix
     * @return Upload result
     */
    public UploadMusicFileResult uploadAudio(MultipartFile multipartFile, String uploadPathPrefix) {
        // Validate audio file
        validAudio(multipartFile);
        
        // Audio upload path
        String uuid = RandomUtil.randomString(16);
        String originFilename = multipartFile.getOriginalFilename();
        String originFileExtension = FileUtil.getSuffix(originFilename);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, originFileExtension);
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        
        // Create result object and set default values
        UploadMusicFileResult uploadMusicFileResult = new UploadMusicFileResult();
        uploadMusicFileResult.setName(FileUtil.mainName(originFilename));
        uploadMusicFileResult.setFileFormat(originFileExtension);
        uploadMusicFileResult.setArtist("");
        uploadMusicFileResult.setAlbum("");
        uploadMusicFileResult.setIntroduction("");
        uploadMusicFileResult.setDuration(0);
        uploadMusicFileResult.setBitRate(0);
        
        File file = null;
        try {
            // Don't use createTempFile (which automatically adds .tmp suffix), instead manually create file in temp directory
            String tempDir = System.getProperty("java.io.tmpdir");
            String tempFilePath = tempDir + File.separator + "music_temp_" + uuid + "." + originFileExtension;
            file = new File(tempFilePath);
            multipartFile.transferTo(file);
            
            log.info("Temporary file created successfully: {}, size: {}, file format: {}", file.getAbsolutePath(), FileUtil.size(file), originFileExtension);
            uploadMusicFileResult.setFileSize(FileUtil.size(file));
            
            // First upload the file
            try {
                cosManager.putObject(uploadPath, file);
                uploadMusicFileResult.setUrl(cosClientConfig.getHost() + uploadPath);
                log.info("File uploaded to: {}", uploadMusicFileResult.getUrl());
            } catch (Exception e) {
                log.error("File upload failed: {}", e.getMessage(), e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "File upload failed: " + e.getMessage());
            }
            
            // Then try to extract metadata from the audio file
            try {
                log.info("Starting to extract audio metadata...");
                extractAudioMetadata(file, uploadMusicFileResult);
                log.info("Audio information: format={}, duration={}s, bit rate={}kbps", 
                    uploadMusicFileResult.getFileFormat(), 
                    uploadMusicFileResult.getDuration(),
                    uploadMusicFileResult.getBitRate());
            } catch (Exception e) {
                log.warn("Failed to get audio information, using default values: {}", e.getMessage());
                // Metadata extraction failure doesn't affect the overall upload process, default values are already set
            }
            
            return uploadMusicFileResult;
        } catch (Exception e) {
            log.error("Audio upload processing failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Upload failed: " + e.getMessage());
        } finally {
            deleteTempFile(file);
        }
    }

    /**
     * Extract audio file metadata
     *
     * @param file Audio file
     * @param result Upload result object (already contains default values)
     */
    private void extractAudioMetadata(File file, UploadMusicFileResult result) {
        try {
            String extension = FileUtil.getSuffix(file.getName()).toLowerCase();
            log.info("Starting to read audio file: {}, file format: {}", file.getAbsolutePath(), extension);
            
            // If it's mgg format or other non-standard format, try renaming to ogg for processing
            File tempFile = null;
            boolean needToDeleteTempFile = false;
            
            if (!"mp3".equals(extension) && !"wav".equals(extension) && !"flac".equals(extension) 
                    && !"ogg".equals(extension) && !"m4a".equals(extension) && !"aac".equals(extension)) {
                log.info("Non-standard format detected: {}, trying to process as ogg format", extension);
                tempFile = new File(file.getParentFile(), file.getName().replace("." + extension, ".ogg"));
                FileUtil.copy(file, tempFile, true);
                file = tempFile;
                needToDeleteTempFile = true;
                extension = "ogg";
            }
            
            try {
                // Try using AudioFileIO to read the file
                AudioFile audioFile = AudioFileIO.read(file);
                log.info("Successfully read audio file");
                
                AudioHeader audioHeader = audioFile.getAudioHeader();
                
                // Get audio duration
                try {
                    int trackLength = audioHeader.getTrackLength();
                    log.info("Audio duration: {} seconds", trackLength);
                    result.setDuration(Integer.valueOf(trackLength));
                } catch (Exception e) {
                    log.warn("Failed to get audio duration: {}", e.getMessage());
                }
                
                // Get bit rate
                try {
                    String bitRateStr = audioHeader.getBitRate();
                    log.info("Original bit rate string: {}", bitRateStr);
                    
                    // Extract numeric part of bit rate
                    int bitRate = Integer.parseInt(bitRateStr.replaceAll("[^0-9]", ""));
                    log.info("Parsed bit rate: {}", bitRate);
                    result.setBitRate(bitRate);
                } catch (Exception e) {
                    log.warn("Failed to get bit rate: {}", e.getMessage());
                }
                
                // Get file format
                try {
                    String format = audioHeader.getFormat();
                    log.info("Audio format: {}", format);
                    if (StringUtils.hasText(format)) {
                        result.setFileFormat(format.toLowerCase());
                    }
                } catch (Exception e) {
                    log.warn("Failed to get audio format: {}", e.getMessage());
                }
                
                // Get audio tag information
                if (audioFile.getTag() != null) {
                    Tag tag = audioFile.getTag();
                    log.info("Successfully retrieved audio tag information");
                    
                    // Get title
                    try {
                        String title = tag.getFirst(FieldKey.TITLE);
                        if (StringUtils.hasText(title)) {
                            log.info("Audio title: {}", title);
                            result.setName(title);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to get title: {}", e.getMessage());
                    }
                    
                    // Get artist
                    try {
                        String artist = tag.getFirst(FieldKey.ARTIST);
                        if (StringUtils.hasText(artist)) {
                            log.info("Artist: {}", artist);
                            result.setArtist(artist);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to get artist: {}", e.getMessage());
                    }
                    
                    // Get album
                    try {
                        String album = tag.getFirst(FieldKey.ALBUM);
                        if (StringUtils.hasText(album)) {
                            log.info("Album: {}", album);
                            result.setAlbum(album);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to get album: {}", e.getMessage());
                    }
                    
                    // Get comment/introduction
                    try {
                        String comment = tag.getFirst(FieldKey.COMMENT);
                        if (StringUtils.hasText(comment)) {
                            log.info("Comment: {}", comment);
                            result.setIntroduction(comment);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to get comment: {}", e.getMessage());
                    }
                } else {
                    log.info("Audio file has no tag information");
                }
                
            } finally {
                // If a temporary file was created, delete it
                if (needToDeleteTempFile && tempFile != null && tempFile.exists()) {
                    try {
                        boolean deleted = tempFile.delete();
                        if (!deleted) {
                            log.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to delete temporary file: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Audio metadata extraction failed: {}", e.getMessage(), e);
            // Don't throw exception, use default values
        }
    }

    /**
     * Validate audio file
     *
     * @param multipartFile multipart file
     */
    public void validAudio(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "File cannot be empty");
        // 1. Check file size
        long fileSize = multipartFile.getSize();
        final long TEN_M = 20 * 1024 * 1024L;
        ThrowUtils.throwIf(fileSize > 2 * TEN_M, ErrorCode.PARAMS_ERROR, "File size cannot exceed 20M");
        // 2. Check file extension
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // Allowed audio file extensions
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("mp3", "wav", "flac", "m4a", "aac", "ogg", "mgg");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "Audio file type error");
    }

    /**
     * Delete temporary file
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        // Delete temporary file
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }


    /**
     * Upload image
     *
     * @param multipartFile File
     * @param uploadPathPrefix Upload path prefix
     * @return Image URL
     */
    public String uploadImage(MultipartFile multipartFile, String uploadPathPrefix) {
        // Image upload path
        String uuid = RandomUtil.randomString(16);
        String originFilename = multipartFile.getOriginalFilename();
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, 
                FileUtil.getSuffix(originFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        
        try {
            // Create temporary file
            file = File.createTempFile("temp_", "." + FileUtil.getSuffix(originFilename));
            multipartFile.transferTo(file);
            
            // Upload image
            cosManager.putObject(uploadPath, file);
            return cosClientConfig.getHost() + uploadPath;
        } catch (Exception e) {
            log.error("Image upload to object storage failed", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Upload failed");
        } finally {
            this.deleteTempFile(file);
        }
    }

    /**
     * Validate image file
     */
    public void validImage(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "File cannot be empty");
        // Check file size
        long fileSize = multipartFile.getSize();
        final long FIVE_M = 5 * 1024 * 1024L;
        ThrowUtils.throwIf(fileSize > FIVE_M, ErrorCode.PARAMS_ERROR, "Image size cannot exceed 5M");
        // Check file extension
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // Allowed image file extensions
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix),
                ErrorCode.PARAMS_ERROR, "Image file type error");
    }
}
