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
     * 上传音频文件
     *
     * @param multipartFile    音频文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传结果
     */
    public UploadMusicFileResult uploadAudio(MultipartFile multipartFile, String uploadPathPrefix) {
        // 校验音频文件
        validAudio(multipartFile);
        
        // 音频上传地址
        String uuid = RandomUtil.randomString(16);
        String originFilename = multipartFile.getOriginalFilename();
        String originFileExtension = FileUtil.getSuffix(originFilename);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, originFileExtension);
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        
        // 创建结果对象并设置默认值
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
            // 不使用createTempFile(会自动添加.tmp后缀)，而是手动创建临时目录中的文件
            String tempDir = System.getProperty("java.io.tmpdir");
            String tempFilePath = tempDir + File.separator + "music_temp_" + uuid + "." + originFileExtension;
            file = new File(tempFilePath);
            multipartFile.transferTo(file);
            
            log.info("临时文件创建成功: {}, 大小: {}, 文件格式: {}", file.getAbsolutePath(), FileUtil.size(file), originFileExtension);
            uploadMusicFileResult.setFileSize(FileUtil.size(file));
            
            // 先上传文件
            try {
                cosManager.putObject(uploadPath, file);
                uploadMusicFileResult.setUrl(cosClientConfig.getHost() + uploadPath);
                log.info("文件已上传到: {}", uploadMusicFileResult.getUrl());
            } catch (Exception e) {
                log.error("文件上传失败: {}", e.getMessage(), e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败: " + e.getMessage());
            }
            
            // 然后尝试提取音频文件的元数据
            try {
                log.info("开始提取音频元数据...");
                extractAudioMetadata(file, uploadMusicFileResult);
                log.info("音频信息: 格式={}, 时长={}秒, 比特率={}kbps", 
                    uploadMusicFileResult.getFileFormat(), 
                    uploadMusicFileResult.getDuration(),
                    uploadMusicFileResult.getBitRate());
            } catch (Exception e) {
                log.warn("获取音频信息失败，使用默认值: {}", e.getMessage());
                // 元数据提取失败不影响整体上传流程，已经有默认值了
            }
            
            return uploadMusicFileResult;
        } catch (Exception e) {
            log.error("音频上传处理失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败: " + e.getMessage());
        } finally {
            deleteTempFile(file);
        }
    }

    /**
     * 提取音频文件元数据
     *
     * @param file 音频文件
     * @param result 上传结果对象（已包含默认值）
     */
    private void extractAudioMetadata(File file, UploadMusicFileResult result) {
        try {
            String extension = FileUtil.getSuffix(file.getName()).toLowerCase();
            log.info("开始读取音频文件: {}, 文件格式: {}", file.getAbsolutePath(), extension);
            
            // 如果是mgg格式或其他非标准格式，尝试重命名为ogg后处理
            File tempFile = null;
            boolean needToDeleteTempFile = false;
            
            if (!"mp3".equals(extension) && !"wav".equals(extension) && !"flac".equals(extension) 
                    && !"ogg".equals(extension) && !"m4a".equals(extension) && !"aac".equals(extension)) {
                log.info("检测到非标准格式: {}, 尝试作为ogg格式处理", extension);
                tempFile = new File(file.getParentFile(), file.getName().replace("." + extension, ".ogg"));
                FileUtil.copy(file, tempFile, true);
                file = tempFile;
                needToDeleteTempFile = true;
                extension = "ogg";
            }
            
            try {
                // 尝试使用AudioFileIO读取文件
                AudioFile audioFile = AudioFileIO.read(file);
                log.info("成功读取音频文件");
                
                AudioHeader audioHeader = audioFile.getAudioHeader();
                
                // 获取音频时长
                try {
                    int trackLength = audioHeader.getTrackLength();
                    log.info("音频时长: {}秒", trackLength);
                    result.setDuration(Integer.valueOf(trackLength));
                } catch (Exception e) {
                    log.warn("获取音频时长失败: {}", e.getMessage());
                }
                
                // 获取比特率
                try {
                    String bitRateStr = audioHeader.getBitRate();
                    log.info("原始比特率字符串: {}", bitRateStr);
                    
                    // 提取比特率数字部分
                    int bitRate = Integer.parseInt(bitRateStr.replaceAll("[^0-9]", ""));
                    log.info("解析后比特率: {}", bitRate);
                    result.setBitRate(bitRate);
                } catch (Exception e) {
                    log.warn("获取比特率失败: {}", e.getMessage());
                }
                
                // 获取文件格式
                try {
                    String format = audioHeader.getFormat();
                    log.info("音频格式: {}", format);
                    if (StringUtils.hasText(format)) {
                        result.setFileFormat(format.toLowerCase());
                    }
                } catch (Exception e) {
                    log.warn("获取音频格式失败: {}", e.getMessage());
                }
                
                // 获取音频标签信息
                if (audioFile.getTag() != null) {
                    Tag tag = audioFile.getTag();
                    log.info("成功获取音频标签信息");
                    
                    // 获取标题
                    try {
                        String title = tag.getFirst(FieldKey.TITLE);
                        if (StringUtils.hasText(title)) {
                            log.info("音频标题: {}", title);
                            result.setName(title);
                        }
                    } catch (Exception e) {
                        log.warn("获取标题失败: {}", e.getMessage());
                    }
                    
                    // 获取艺术家
                    try {
                        String artist = tag.getFirst(FieldKey.ARTIST);
                        if (StringUtils.hasText(artist)) {
                            log.info("艺术家: {}", artist);
                            result.setArtist(artist);
                        }
                    } catch (Exception e) {
                        log.warn("获取艺术家失败: {}", e.getMessage());
                    }
                    
                    // 获取专辑
                    try {
                        String album = tag.getFirst(FieldKey.ALBUM);
                        if (StringUtils.hasText(album)) {
                            log.info("专辑: {}", album);
                            result.setAlbum(album);
                        }
                    } catch (Exception e) {
                        log.warn("获取专辑失败: {}", e.getMessage());
                    }
                    
                    // 获取注释/简介
                    try {
                        String comment = tag.getFirst(FieldKey.COMMENT);
                        if (StringUtils.hasText(comment)) {
                            log.info("注释: {}", comment);
                            result.setIntroduction(comment);
                        }
                    } catch (Exception e) {
                        log.warn("获取注释失败: {}", e.getMessage());
                    }
                } else {
                    log.info("音频文件没有标签信息");
                }
                
            } finally {
                // 如果创建了临时文件，删除它
                if (needToDeleteTempFile && tempFile != null && tempFile.exists()) {
                    try {
                        boolean deleted = tempFile.delete();
                        if (!deleted) {
                            log.warn("临时文件删除失败: {}", tempFile.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        log.warn("删除临时文件失败: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("音频元数据提取失败: {}", e.getMessage(), e);
            // 不抛出异常，使用默认值
        }
    }

    /**
     * 校验音频文件
     *
     * @param multipartFile multipart 文件
     */
    public void validAudio(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 1. 校验文件大小
        long fileSize = multipartFile.getSize();
        final long TEN_M = 20 * 1024 * 1024L;
        ThrowUtils.throwIf(fileSize > 2 * TEN_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 20M");
        // 2. 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的音频文件后缀
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("mp3", "wav", "flac", "m4a", "aac", "ogg", "mgg");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "音频文件类型错误");
    }

    /**
     * 删除临时文件
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        // 删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }


    /**
     * 上传图片
     *
     * @param multipartFile 文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 图片URL
     */
    public String uploadImage(MultipartFile multipartFile, String uploadPathPrefix) {
        // 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originFilename = multipartFile.getOriginalFilename();
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, 
                FileUtil.getSuffix(originFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        
        try {
            // 创建临时文件
            file = File.createTempFile("temp_", "." + FileUtil.getSuffix(originFilename));
            multipartFile.transferTo(file);
            
            // 上传图片
            cosManager.putObject(uploadPath, file);
            return cosClientConfig.getHost() + uploadPath;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            this.deleteTempFile(file);
        }
    }

    /**
     * 校验图片文件
     */
    public void validImage(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 校验文件大小
        long fileSize = multipartFile.getSize();
        final long FIVE_M = 5 * 1024 * 1024L;
        ThrowUtils.throwIf(fileSize > FIVE_M, ErrorCode.PARAMS_ERROR, "图片大小不能超过 5M");
        // 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的图片文件后缀
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix),
                ErrorCode.PARAMS_ERROR, "图片文件类型错误");
    }
}
