package org.example.cpt202music.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.GetObjectRequest;
import org.example.cpt202music.config.CosClientConfig;
import org.example.cpt202music.exception.ErrorCode;
import org.example.cpt202music.exception.ThrowUtils;
import org.example.cpt202music.manager.FileManager;
import org.example.cpt202music.model.dto.MusicFile.MusicFileQueryRequest;
import org.example.cpt202music.model.dto.MusicFile.MusicFileReviewRequest;
import org.example.cpt202music.model.dto.MusicFile.MusicFileUploadRequest;
import org.example.cpt202music.model.dto.file.UploadMusicFileResult;
import org.example.cpt202music.model.entity.MusicFile;
import org.example.cpt202music.model.entity.User;
import org.example.cpt202music.model.enums.MusicFileReviewStatusEnum;
import org.example.cpt202music.model.vo.MusicFileVO;
import org.example.cpt202music.model.vo.UserVO;
import org.example.cpt202music.service.MusicFileService;
import org.example.cpt202music.mapper.MusicFileMapper;
import org.example.cpt202music.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import org.example.cpt202music.exception.BusinessException;

import java.io.IOException;


/**
* @author XLW200420
* @description Service implementation for database operations on table【music_file】
* @createDate 2025-04-12 18:57:42
*/
@Service
public class MusicFileServiceImpl extends ServiceImpl<MusicFileMapper, MusicFile>
    implements MusicFileService{

    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;


    @Override
    public MusicFileVO uploadMusicFile(MultipartFile multipartFile, MusicFileUploadRequest musicFileUploadRequest, User loginUser, MultipartFile coverFile) {
        // Validate parameters
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // Determine if it's a new file or an update
        Long musicFileID = null;
        if (musicFileUploadRequest.getId() != null) {
            musicFileID = musicFileUploadRequest.getId();
        }

        // If it's an update, check if the file already exists
        MusicFile oldMusicFile = null;
        if (musicFileID != null) {
            oldMusicFile = this.getById(musicFileID);
            ThrowUtils.throwIf(oldMusicFile == null, ErrorCode.NOT_FOUND_ERROR, "File does not exist");
            // Only the owner or admin can update
            if (!oldMusicFile.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }

        // Upload file and get file information
        // Organize directory by user id
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadMusicFileResult uploadMusicFileResult = fileManager.uploadAudio(multipartFile, uploadPathPrefix);

        // Construct file information for database
        MusicFile musicFile = new MusicFile();
        musicFile.setUrl(uploadMusicFileResult.getUrl());
        musicFile.setName(uploadMusicFileResult.getName());
        musicFile.setArtist(uploadMusicFileResult.getArtist());
        musicFile.setAlbum(uploadMusicFileResult.getAlbum());
        musicFile.setIntroduction(uploadMusicFileResult.getIntroduction());
        musicFile.setFileSize(uploadMusicFileResult.getFileSize());

        // Handle cover image upload
        if (coverFile != null && !coverFile.isEmpty()) {
            // Upload new cover
            String coverUrl = fileManager.uploadImage(coverFile, uploadPathPrefix);
            musicFile.setCoverUrl(coverUrl);
        } else if (StrUtil.isNotBlank(musicFileUploadRequest.getCoverUrl())) {
            // If request contains coverUrl, use that URL
            musicFile.setCoverUrl(musicFileUploadRequest.getCoverUrl());
        } else if (oldMusicFile != null && StrUtil.isNotBlank(oldMusicFile.getCoverUrl())) {
            // If updating and original music has a cover, keep the original cover
            musicFile.setCoverUrl(oldMusicFile.getCoverUrl());
        } else {
            // Use default cover
            musicFile.setCoverUrl("https://tse3-mm.cn.bing.net/th/id/OIP-C.1gt9Vw4SBXSmfCxgfXzOcQHaE8?w=239&h=180&c=7&r=0&o=5&dpr=1.6&pid=1.7");
        }

        // Add null checks
        if (uploadMusicFileResult.getDuration() != null) {
            musicFile.setDuration(uploadMusicFileResult.getDuration());
        } else {
            musicFile.setDuration(0);  // Set default value
        }

        if (uploadMusicFileResult.getBitRate() != null) {
            musicFile.setBitRate(uploadMusicFileResult.getBitRate());
        } else {
            musicFile.setBitRate(0);  // Set default value
        }

        musicFile.setFileFormat(uploadMusicFileResult.getFileFormat());
        musicFile.setUserId(loginUser.getId());

        // Add review parameters
        this.fillReviewParams(musicFile, loginUser);

        // Database operation
        // If musicFileId is not null, it's an update; otherwise, it's a new addition
        if (musicFileID != null) {
            musicFile.setId(musicFileID);
            musicFile.setEditTime(new Date());

            // Retain original data for other fields (if needed)
            if (oldMusicFile != null) {
                // Add other fields to retain here
                if (musicFile.getCreateTime() == null) {
                    musicFile.setCreateTime(oldMusicFile.getCreateTime());
                }
            }
        } else {
            musicFile.setCreateTime(new Date());
            musicFile.setUpdateTime(new Date());
        }



        boolean result = this.saveOrUpdate(musicFile);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "File upload failed, database operation failed");

        // Query database again for complete information to ensure returning the latest data
        MusicFile savedMusicFile = this.getById(musicFile.getId());

        return MusicFileVO.objToVo(savedMusicFile);
    }

    @Override
    public QueryWrapper<MusicFile> getQueryWrapper(MusicFileQueryRequest MusicFileQueryRequest) {
        QueryWrapper<MusicFile> queryWrapper = new QueryWrapper<>();
        if (MusicFileQueryRequest == null){
            return queryWrapper;
        }
        // Get values from object

        Long id = MusicFileQueryRequest.getId();
        String name = MusicFileQueryRequest.getName();
        String introduction = MusicFileQueryRequest.getIntroduction();
        String category = MusicFileQueryRequest.getCategory();
        List<String> tags = MusicFileQueryRequest.getTags();
        Long fileSize = MusicFileQueryRequest.getFileSize();
        Integer duration = MusicFileQueryRequest.getDuration();
        String fileFormat = MusicFileQueryRequest.getFileFormat();
        Long userId = MusicFileQueryRequest.getUserId();
        Integer bitRate = MusicFileQueryRequest.getBitRate();
        String searchText = MusicFileQueryRequest.getSearchText();
        Integer reviewStatus = MusicFileQueryRequest.getReviewStatus();
        String reviewMessage = MusicFileQueryRequest.getReviewMessage();
        Long reviewerId = MusicFileQueryRequest.getReviewerId();

        String sortField = MusicFileQueryRequest.getSortField();
        String sortOrder = MusicFileQueryRequest.getSortOrder();


        // Search from multiple fields
        if (StrUtil.isNotBlank(searchText)) {
            // Need to join query conditions
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(fileFormat), "fileFormat", fileFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);

        queryWrapper.eq(ObjUtil.isNotEmpty(fileSize), "fileSize", fileSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(duration), "duration", duration);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);


        // Use OR condition to connect category and tags
        if (StrUtil.isNotBlank(category)) {
            queryWrapper.eq("category", category);
        }

        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.or();
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }

        // Sorting
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;

    }



    @Override
    public MusicFileVO getMusicFileVO(MusicFile musicFile, HttpServletRequest request) {
        // Convert object to wrapper class
        MusicFileVO musicFileVO = MusicFileVO.objToVo(musicFile);
        // Associate user information
        Long userId = musicFile.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            musicFileVO.setUser(userVO);
        }
        return musicFileVO;
    }


    /**
     * Get paginated image wrapper
     */
    @Override
    public Page<MusicFileVO> getMusicFileVOPage(Page<MusicFile> musicFilePage, HttpServletRequest request) {
        List<MusicFile> musicFileList = musicFilePage.getRecords();
        Page<MusicFileVO> musicFileVOPage = new Page<>(musicFilePage.getCurrent(), musicFilePage.getSize(), musicFilePage.getTotal());
        if (CollUtil.isEmpty(musicFileList)) {
            return musicFileVOPage;
        }
        // Object list => Wrapper object list
        List<MusicFileVO> musicFileVOList = musicFileList.stream().map(MusicFileVO::objToVo).collect(Collectors.toList());
        // 1. Associate user information
        Set<Long> userIdSet = musicFileList.stream().map(MusicFile::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. Fill in information
        musicFileVOList.forEach(musicFileVO -> {
            Long userId = musicFileVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            musicFileVO.setUser(userService.getUserVO(user));
        });
        musicFileVOPage.setRecords(musicFileVOList);
        return musicFileVOPage;
    }



    @Override
    public void validMusicFile(MusicFile musicFile) {
        ThrowUtils.throwIf(musicFile == null, ErrorCode.PARAMS_ERROR);
        // Get values from object
        Long id = musicFile.getId();
        String url = musicFile.getUrl();
        String introduction = musicFile.getIntroduction();
        // When modifying data, id cannot be empty; validate parameters if present
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id cannot be empty");
        // Validate if url is provided
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url is too long");
        }
        // Validate if introduction is provided
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "introduction is too long");
        }
    }


    @Override
    public void streamMusicFile(Long id, HttpServletResponse response) {
        // Get music file information
        MusicFile musicFile = this.getById(id);
        ThrowUtils.throwIf(musicFile == null, ErrorCode.NOT_FOUND_ERROR, "Music file does not exist");

        // Extract file path from URL
        String url = musicFile.getUrl();

        // Extract COS path
        String cosHost = cosClientConfig.getHost();
        String filepath = url;
        if (url.startsWith(cosHost)) {
            filepath = url.substring(cosHost.length());
        }

        // Log playback
        log.error("User is playing music: ID=" + id + ", Name=" + musicFile.getName() + ", File path=" + filepath);

        COSObjectInputStream cosObjectInput = null;
        try {
            // Use cosClient instead of cosManager
            GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), filepath);
            COSObject cosObject = cosClient.getObject(getObjectRequest);
            cosObjectInput = cosObject.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);

            // Set HTTP headers to support audio playback
            String contentType = "audio/mpeg"; // Default MIME type
            String fileFormat = musicFile.getFileFormat().toLowerCase();

            // Set correct MIME type based on file format
            switch (fileFormat) {
                case "mp3":
                    contentType = "audio/mpeg";
                    break;
                case "wav":
                    contentType = "audio/wav";
                    break;
                case "ogg":
                    contentType = "audio/ogg";
                    break;
                case "m4a":
                case "aac":
                    contentType = "audio/aac";
                    break;
                case "flac":
                    contentType = "audio/flac";
                    break;
            }

            // Set response headers
            response.setContentType(contentType);
            response.setContentLength(bytes.length);

            // Set allow range requests, important for large audio files and progress bar dragging functionality
            response.setHeader("Accept-Ranges", "bytes");

            // Write audio data to response
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();

        } catch (Exception e) {
            log.error("Audio stream transmission failed, id = " + id + ", Error: " + e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Audio playback failed: " + e.getMessage());
        } finally {
            // Close stream
            if (cosObjectInput != null) {
                try {
                    cosObjectInput.close();
                } catch (IOException e) {
                    log.error("Failed to close COS input stream", e);
                }
            }
        }
    }

    @Override
    public List<MusicFileVO> getPlaylistByCategory(String category, HttpServletRequest request) {
        // Create query request object
        MusicFileQueryRequest queryRequest = new MusicFileQueryRequest();
        queryRequest.setCategory(category);
        queryRequest.setPageSize(100); // Set a reasonable upper limit

        // Query database to get music in this category
        Page<MusicFile> musicFilePage = this.page(
                new Page<>(1, queryRequest.getPageSize()),
                this.getQueryWrapper(queryRequest)
        );

        // Convert to VO object list needed by frontend
        return musicFilePage.getRecords().stream()
                .map(musicFile -> this.getMusicFileVO(musicFile, request))
                .collect(Collectors.toList());
    }

    @Override
    public void doMusicFileReview(MusicFileReviewRequest musicFileReviewRequest, User loginUser) {
        Long id = musicFileReviewRequest.getId();
        Integer reviewStatus = musicFileReviewRequest.getReviewStatus();
        MusicFileReviewStatusEnum reviewStatusEnum = MusicFileReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null || MusicFileReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // Check if exists
        MusicFile oldMusicFile = this.getById(id);
        ThrowUtils.throwIf(oldMusicFile == null, ErrorCode.NOT_FOUND_ERROR);
        // Already in this state
        if (oldMusicFile.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Do not review repeatedly");
        }
        // Update review status
        MusicFile updateMusicFile = new MusicFile();
        BeanUtils.copyProperties(musicFileReviewRequest, updateMusicFile);
        updateMusicFile.setReviewerId(loginUser.getId());
        updateMusicFile.setReviewTime(new Date());
        boolean result = this.updateById(updateMusicFile);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }


    /**
     * Fill in review parameters
     * @param musicFile
     * @param loginUser
     *
     *
     */
    @Override
    public void fillReviewParams(MusicFile musicFile, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // Admin automatically passes review
            musicFile.setReviewStatus(MusicFileReviewStatusEnum.PASS.getValue());
            musicFile.setReviewMessage("Admin automatic approval");
            musicFile.setReviewerId(loginUser.getId());
            musicFile.setReviewTime(new Date());
        } else {
            // Non-admin, whether editing or creating, requires review
            musicFile.setReviewStatus(MusicFileReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * Get QueryWrapper for approved music files (reviewStatus == 1).
     *
     * @return QueryWrapper<MusicFile>
     */
    @Override
    public QueryWrapper<MusicFile> getApprovedMusicQueryWrapper() {
        QueryWrapper<MusicFile> queryWrapper = new QueryWrapper<>();
        // Filter for approved music files (reviewStatus == 1, assuming PASS value is 1)
        queryWrapper.eq("reviewStatus", MusicFileReviewStatusEnum.PASS.getValue());
        return queryWrapper;
    }

    /**
     * Get query wrapper for fuzzy search
     * This method allows fuzzy search across multiple fields (tags, categories, song names, artists, etc.)
     *
     * @param musicFileQueryRequest Query request object containing search text
     * @return Query wrapper configured with fuzzy search conditions
     */
    @Override
    public QueryWrapper<MusicFile> getFuzzySearchQueryWrapper(MusicFileQueryRequest musicFileQueryRequest) {
        QueryWrapper<MusicFile> queryWrapper = new QueryWrapper<>();
        if (musicFileQueryRequest == null) {
            return queryWrapper;
        }

        // Get search keywords
        String searchText = musicFileQueryRequest.getSearchText();
        Integer reviewStatus = musicFileQueryRequest.getReviewStatus();

        // By default, only show approved music
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);

        // If search text is empty, return directly
        if (StrUtil.isBlank(searchText)) {
            return queryWrapper;
        }

        // Build multi-field fuzzy search
        queryWrapper.and(wrapper -> {
            // Search song name
            wrapper.like("name", searchText)
                   // Search artist
                   .or().like("artist", searchText)
                   // Search album name
                   .or().like("album", searchText)
                   // Search introduction
                   .or().like("introduction", searchText)
                   // Search category
                   .or().like("category", searchText)
                   // Search tags (tags stored in JSON format)
                   .or().like("tags", searchText);
        });

        // Apply pagination sorting conditions
        String sortField = musicFileQueryRequest.getSortField();
        String sortOrder = musicFileQueryRequest.getSortOrder();
        if (StrUtil.isNotEmpty(sortField)) {
            queryWrapper.orderBy(true, sortOrder != null && sortOrder.equals("ascend"), sortField);
        } else {
            // Default sort by creation time descending
            queryWrapper.orderByDesc("createTime");
        }

        return queryWrapper;
    }

}




