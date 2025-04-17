package org.example.cpt202music.controller;


import cn.hutool.json.JSONUtil;
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
    // @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)  现在开放给用户用了
    public BaseResponse<MusicFileVO> uploadMusicFile(@RequestPart("file") MultipartFile multipartFile,
                                                MusicFileUploadRequest MusicFileUploadRequest,
                                                     @RequestPart(value = "coverFile", required = false) MultipartFile coverFile,
                                                HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        MusicFileVO musicFileVO = musicFileService.uploadMusicFile(multipartFile,  MusicFileUploadRequest, loginUser, coverFile);
        return ResultUtils.success(musicFileVO);
    }


    /**
     * 删除图片
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
        // 仅本人或者管理员可删除
        if (!oldmusicFile.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = musicFileService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "文件删除失败，数据库操作失败");
        return ResultUtils.success(result);
    }


    /**
     * 更新图片 仅管理员
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
        // 将实体类和 DTO 进行转换
        MusicFile musicFile = new MusicFile();
        BeanUtils.copyProperties(musicFileUpdateRequset, musicFile);
        // 注意将 list 转为 string
        musicFile.setTags(JSONUtil.toJsonStr(musicFileUpdateRequset.getTags()));
        // 数据校验
        musicFileService.validMusicFile(musicFile);
        // 判断是否存在
        long id = musicFileUpdateRequset.getId();
        MusicFile oldPicture = musicFileService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 补充审核参数
        User loginUser =userService.getLoginUser(request);
        musicFileService.fillReviewParams(musicFile, loginUser);
        // 操作数据库
        boolean result = musicFileService.updateById(musicFile);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<MusicFile> getMusicFileById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        MusicFile picture = musicFileService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<MusicFileVO> getMusicFileVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        MusicFile musicFile = musicFileService.getById(id);
        ThrowUtils.throwIf(musicFile == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(musicFileService.getMusicFileVO(musicFile, request));
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<MusicFile>> listMusicFileByPage(@RequestBody MusicFileQueryRequest musicFileQueryRequest) {
        long current = musicFileQueryRequest.getCurrent();
        long size = musicFileQueryRequest.getPageSize();
        // 查询数据库
        Page<MusicFile> musicFilePage = musicFileService.page(new Page<>(current, size),
                musicFileService.getQueryWrapper(musicFileQueryRequest));
        return ResultUtils.success(musicFilePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<MusicFileVO>> listMusicFileVOByPage(@RequestBody MusicFileQueryRequest musicFileQueryRequest,
                                                             HttpServletRequest request) {
        long current = musicFileQueryRequest.getCurrent();
        long size = musicFileQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 普通用户默认只能看到审核通过的数据
        musicFileQueryRequest.setReviewStatus(MusicFileReviewStatusEnum.PASS.getValue());
        // 查询数据库
        Page<MusicFile> MusicFilePage = musicFileService.page(new Page<>(current, size),
                musicFileService.getQueryWrapper(musicFileQueryRequest));
        // 获取封装类
        return ResultUtils.success(musicFileService.getMusicFileVOPage(MusicFilePage, request));
    }

    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editMusicFile(@RequestBody MusicFileEditRequest musicFileEditRequest, HttpServletRequest request) {
        if (musicFileEditRequest == null || musicFileEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 在此处将实体类和 DTO 进行转换
        MusicFile musicFile = new MusicFile();
        BeanUtils.copyProperties(musicFileEditRequest, musicFile);
        // 注意将 list 转为 string
        musicFile.setTags(JSONUtil.toJsonStr(musicFileEditRequest.getTags()));
        // 设置编辑时间
        musicFile.setEditTime(new Date());
        // 数据校验
        musicFileService.validMusicFile(musicFile);
        User loginUser = userService.getLoginUser(request);
        // 补充审核参数
        musicFileService.fillReviewParams(musicFile, loginUser);
        // 判断是否存在
        long id = musicFileEditRequest.getId();
        MusicFile oldMusicfile = musicFileService.getById(id);
        ThrowUtils.throwIf(oldMusicfile == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldMusicfile.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
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
     * 音频流式播放
     *
     * @param id 音乐文件ID
     * @param response HTTP响应对象
     */
    @GetMapping("/stream/{id}")
    public void streamAudio(@PathVariable("id") Long id, HttpServletResponse response) {
        musicFileService.streamMusicFile(id, response);
    }

    /**
     * 获取指定分类的播放列表
     *
     * @param category 音乐分类
     * @return 音乐列表
     */
    @GetMapping("/playlist/{category}")
    public BaseResponse<List<MusicFileVO>> getPlaylistByCategory(@PathVariable("category") String category, HttpServletRequest request) {
        List<MusicFileVO> playlist = musicFileService.getPlaylistByCategory(category, request);
        return ResultUtils.success(playlist);
    }

    /**
     * 获取所有音乐分类下的播放列表
     *
     * @return 分类及对应的音乐列表
     */
    @GetMapping("/playlists")
    public BaseResponse<MusicFilePlaylistsVO> getAllPlaylists(HttpServletRequest request) {
        // 获取所有分类
        BaseResponse<MusicFileTagCategory> tagCategoryResponse = listMusicFileTagCategory();
        List<String> categories = tagCategoryResponse.getData().getCategoryList();

        // 为每个分类获取音乐列表
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
}


