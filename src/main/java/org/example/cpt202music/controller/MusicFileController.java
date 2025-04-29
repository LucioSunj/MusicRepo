package org.example.cpt202music.controller;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.example.cpt202music.annotation.AuthCheck;
import org.example.cpt202music.common.BaseResponse;
import org.example.cpt202music.common.DeleteRequest;
import org.example.cpt202music.common.ResultUtils;
import org.example.cpt202music.constant.UserConstant;
import org.example.cpt202music.exception.BusinessException;
import org.example.cpt202music.exception.ErrorCode;
import org.example.cpt202music.exception.ThrowUtils;
import org.example.cpt202music.manager.CosManager;
import org.example.cpt202music.model.dto.MusicFile.*;
import org.example.cpt202music.model.entity.MusicFile;
import org.example.cpt202music.model.entity.User;
import org.example.cpt202music.model.enums.MusicFileReviewStatusEnum;
import org.example.cpt202music.model.vo.MusicFilePlaylistsVO;
import org.example.cpt202music.model.vo.MusicFileTagCategory;
import org.example.cpt202music.model.vo.MusicFileVO;
import org.example.cpt202music.service.MusicFileService;
import org.example.cpt202music.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/musicfile")
public class MusicFileController {

    @Resource
    private UserService userService;
    @Resource
    private MusicFileService musicFileService;


    @PostMapping("/upload")
    // @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)  Now open for users
    public BaseResponse<MusicFileVO> uploadMusicFile(@RequestPart("file") MultipartFile multipartFile,
                                                MusicFileUploadRequest MusicFileUploadRequest,
                                                     @RequestPart(value = "coverFile", required = false) MultipartFile coverFile,
                                                HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        MusicFileVO musicFileVO = musicFileService.uploadMusicFile(multipartFile,  MusicFileUploadRequest, loginUser, coverFile);
        return ResultUtils.success(musicFileVO);
    }


    /**
     * Delete file
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteMusicFile(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        MusicFile oldmusicFile = musicFileService.getById(id);
        ThrowUtils.throwIf(oldmusicFile == null, ErrorCode.NOT_FOUND_ERROR);
        // Only owner or admin can delete
        if (!oldmusicFile.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // Database operation
        boolean result = musicFileService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "File deletion failed, database operation failed");
        return ResultUtils.success(result);
    }


    /**
     * Update file - admin only
     * @param musicFileUpdateRequset
     * @param request
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateMusicFile(@RequestBody MusicFileUpdateRequset musicFileUpdateRequset, HttpServletRequest request) {
        if (musicFileUpdateRequset == null || musicFileUpdateRequset.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // Convert entity and DTO
        MusicFile musicFile = new MusicFile();
        BeanUtils.copyProperties(musicFileUpdateRequset, musicFile);
        // Convert list to string
        musicFile.setTags(JSONUtil.toJsonStr(musicFileUpdateRequset.getTags()));
        // Data validation
        musicFileService.validMusicFile(musicFile);
        // Check if exists
        long id = musicFileUpdateRequset.getId();
        MusicFile oldPicture = musicFileService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // Add review parameters
        User loginUser =userService.getLoginUser(request);
        musicFileService.fillReviewParams(musicFile, loginUser);

        if (StrUtil.isNotBlank(musicFileUpdateRequset.getArtist())) {
            musicFile.setArtist(musicFileUpdateRequset.getArtist());
        }
        // Database operation
        boolean result = musicFileService.updateById(musicFile);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * Get file by id (admin only)
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<MusicFile> getMusicFileById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // Query database
        MusicFile picture = musicFileService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // Get wrapper class
        return ResultUtils.success(picture);
    }

    /**
     * Get file by id (wrapper class)
     */
    @GetMapping("/get/vo")
    public BaseResponse<MusicFileVO> getMusicFileVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // Query database
        MusicFile musicFile = musicFileService.getById(id);
        ThrowUtils.throwIf(musicFile == null, ErrorCode.NOT_FOUND_ERROR);
        // Get wrapper class
        return ResultUtils.success(musicFileService.getMusicFileVO(musicFile, request));
    }

    /**
     * Get paginated file list (admin only)
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<MusicFile>> listMusicFileByPage(@RequestBody MusicFileQueryRequest musicFileQueryRequest) {
        long current = musicFileQueryRequest.getCurrent();
        long size = musicFileQueryRequest.getPageSize();
        // Query database
        Page<MusicFile> musicFilePage = musicFileService.page(new Page<>(current, size),
                musicFileService.getQueryWrapper(musicFileQueryRequest));
        return ResultUtils.success(musicFilePage);
    }


    /**
     * Get paginated music list (wrapper class)
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<MusicFileVO>> listMusicFileVOByPage(@RequestBody MusicFileQueryRequest musicFileQueryRequest,
                                                             HttpServletRequest request) {
        long current = musicFileQueryRequest.getCurrent();
        long size = musicFileQueryRequest.getPageSize();
        // Limit crawler
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // Regular users can only see approved data by default
//        musicFileQueryRequest.setReviewStatus(MusicFileReviewStatusEnum.PASS.getValue());
        // Query database
        Page<MusicFile> MusicFilePage = musicFileService.page(new Page<>(current, size),
                musicFileService.getApprovedMusicQueryWrapper());
        // Get wrapper class
        return ResultUtils.success(musicFileService.getMusicFileVOPage(MusicFilePage, request));
    }

    /**
     * Edit file (for users)
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editMusicFile(@RequestBody MusicFileEditRequest musicFileEditRequest, HttpServletRequest request) {
        if (musicFileEditRequest == null || musicFileEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // Convert entity and DTO
        MusicFile musicFile = new MusicFile();
        BeanUtils.copyProperties(musicFileEditRequest, musicFile);
        // Convert list to string
        musicFile.setTags(JSONUtil.toJsonStr(musicFileEditRequest.getTags()));
        // Set edit time
        musicFile.setEditTime(new Date());
        
        // Process artist information
        if (StrUtil.isNotBlank(musicFileEditRequest.getArtist())) {
            musicFile.setArtist(musicFileEditRequest.getArtist());
        }
        
        // Data validation
        musicFileService.validMusicFile(musicFile);
        User loginUser = userService.getLoginUser(request);
        // Add review parameters
        musicFileService.fillReviewParams(musicFile, loginUser);
        // Check if exists
        long id = musicFileEditRequest.getId();
        MusicFile oldMusicfile = musicFileService.getById(id);
        ThrowUtils.throwIf(oldMusicfile == null, ErrorCode.NOT_FOUND_ERROR);
        // Only owner or admin can edit
        if (!oldMusicfile.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // Database operation
        boolean result = musicFileService.updateById(musicFile);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    @GetMapping("/tag_category")
    public BaseResponse<MusicFileTagCategory> listMusicFileTagCategory() {
        MusicFileTagCategory musicFileTagCategory = new MusicFileTagCategory();
        List<String> tagList = Arrays.asList("Pop", "Rock", "Folk", "Electronic", "Jazz", "Absolute Music", "Rap", "Metal", "Classical");
        List<String> categoryList = Arrays.asList("Pop", "Rock", "Folk", "Electronic", "Jazz", "Absolute Music", "Rap", "Metal", "Classical");
        musicFileTagCategory.setTagList(tagList);
        musicFileTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(musicFileTagCategory);
    }

    /**
     * Audio streaming playback
     *
     * @param id Music file ID
     * @param response HTTP response object
     */
    @GetMapping("/stream/{id}")
    public void streamAudio(@PathVariable("id") Long id, HttpServletResponse response) {
        musicFileService.streamMusicFile(id, response);
    }

    /**
     * Get playlist for a specific category
     *
     * @param category Music category
     * @return Music list
     */
    @GetMapping("/playlist/{category}")
    public BaseResponse<List<MusicFileVO>> getPlaylistByCategory(@PathVariable("category") String category, HttpServletRequest request) {
        List<MusicFileVO> playlist = musicFileService.getPlaylistByCategory(category, request);
        return ResultUtils.success(playlist);
    }

    /**
     * Get playlists for all music categories
     *
     * @return Categories and corresponding music lists
     */
    @GetMapping("/playlists")
    public BaseResponse<MusicFilePlaylistsVO> getAllPlaylists(HttpServletRequest request) {
        // Get all categories
        BaseResponse<MusicFileTagCategory> tagCategoryResponse = listMusicFileTagCategory();
        List<String> categories = tagCategoryResponse.getData().getCategoryList();

        // Get music list for each category
        Map<String, List<MusicFileVO>> playlists = new HashMap<>();
        for (String category : categories) {
            List<MusicFileVO> playlist = musicFileService.getPlaylistByCategory(category, request);
            playlists.put(category, playlist);
        }

        return ResultUtils.success(new MusicFilePlaylistsVO(playlists));
    }

    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> reviewMusicFile(@RequestBody MusicFileReviewRequest musicFileReviewRequest, HttpServletRequest request) {
        if (musicFileReviewRequest == null || musicFileReviewRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        musicFileService.doMusicFileReview(musicFileReviewRequest, loginUser);
        return ResultUtils.success(true);
    }



    /**
     * Get pending music file list (admin)
     */
    @GetMapping("/admin/list/pending")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<MusicFile>> listPendingMusicFiles(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long pageSize) {

        // Create query request object
        MusicFileQueryRequest musicFileQueryRequest = new MusicFileQueryRequest();
        musicFileQueryRequest.setReviewStatus(0);  // Pending review

        // Build query conditions
        QueryWrapper<MusicFile> queryWrapper = musicFileService.getQueryWrapper(musicFileQueryRequest);

        // Execute paginated query
        Page<MusicFile> musicFilePage = musicFileService.page(
                new Page<>(current, pageSize), queryWrapper);

        return ResultUtils.success(musicFilePage);
    }

    /**
     * Get approved music file list (admin)
     */
    @GetMapping("/admin/list/approved")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<MusicFile>> listApprovedMusicFiles(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long pageSize) {

        // Create query request object
        MusicFileQueryRequest musicFileQueryRequest = new MusicFileQueryRequest();
        musicFileQueryRequest.setReviewStatus(1);  // Approved

        // Build query conditions
        QueryWrapper<MusicFile> queryWrapper = musicFileService.getQueryWrapper(musicFileQueryRequest);

        // Execute paginated query
        Page<MusicFile> musicFilePage = musicFileService.page(
                new Page<>(current, pageSize), queryWrapper);

        return ResultUtils.success(musicFilePage);
    }

    /**
     * Get rejected music file list (admin)
     */
    @GetMapping("/admin/list/rejected")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<MusicFile>> listRejectedMusicFiles(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long pageSize) {

        // Create query request object
        MusicFileQueryRequest musicFileQueryRequest = new MusicFileQueryRequest();
        musicFileQueryRequest.setReviewStatus(-1);  // Rejected

        // Build query conditions
        QueryWrapper<MusicFile> queryWrapper = musicFileService.getQueryWrapper(musicFileQueryRequest);

        // Execute paginated query
        Page<MusicFile> musicFilePage = musicFileService.page(
                new Page<>(current, pageSize), queryWrapper);

        return ResultUtils.success(musicFilePage);
    }

    /**
     * Get paginated music list by category (wrapper class)
     *
     * @param category Music category
     * @param current  Current page
     * @param pageSize Page size
     * @param request  HTTP request object
     * @return Paginated music list
     */
    @GetMapping("/list/page/category/{category}")
    public BaseResponse<Page<MusicFileVO>> listMusicFileVOByCategoryPage(
            @PathVariable("category") String category,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long pageSize,
            HttpServletRequest request) {

        // Limit crawler
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);

        // Build query conditions
        MusicFileQueryRequest musicFileQueryRequest = new MusicFileQueryRequest();
        musicFileQueryRequest.setCurrent((int) current);
        musicFileQueryRequest.setPageSize((int) pageSize);
        musicFileQueryRequest.setCategory(category);
        List<String> tags = new ArrayList<>();
        tags.add(category);
        musicFileQueryRequest.setTags(tags);

        // Regular users can only see approved data by default
        musicFileQueryRequest.setReviewStatus(MusicFileReviewStatusEnum.PASS.getValue());

        // Query database
        Page<MusicFile> musicFilePage = musicFileService.page(new Page<>(current, pageSize),
                musicFileService.getQueryWrapper(musicFileQueryRequest));

        // Get wrapper class
        return ResultUtils.success(musicFileService.getMusicFileVOPage(musicFilePage, request));
    }
    
    /**
     * Fuzzy search music files (search by tags, categories, song name, artist, etc.)
     *
     * @param searchText Search keyword
     * @param current Current page
     * @param pageSize Page size
     * @param request HTTP request object
     * @return List of matching music files
     */
    @GetMapping("/search")
    public BaseResponse<Page<MusicFileVO>> searchMusicFiles(
            @RequestParam String searchText,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long pageSize,
            HttpServletRequest request) {
        
        // Limit crawler
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        
        // Build query conditions
        MusicFileQueryRequest musicFileQueryRequest = new MusicFileQueryRequest();
        musicFileQueryRequest.setCurrent((int) current);
        musicFileQueryRequest.setPageSize((int) pageSize);
        musicFileQueryRequest.setSearchText(searchText);
        
        // Regular users can only see approved data by default
        musicFileQueryRequest.setReviewStatus(MusicFileReviewStatusEnum.PASS.getValue());
        
        // Query database
        Page<MusicFile> musicFilePage = musicFileService.page(new Page<>(current, pageSize),
                musicFileService.getFuzzySearchQueryWrapper(musicFileQueryRequest));
        
        // Get wrapper class
        return ResultUtils.success(musicFileService.getMusicFileVOPage(musicFilePage, request));
    }
}


