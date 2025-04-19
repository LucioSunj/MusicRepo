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
* @description 针对表【music_file(音乐文件)】的数据库操作Service实现
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
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 判断是新增还是更新
        Long musicFileID = null;
        if (musicFileUploadRequest.getId() != null) {
            musicFileID = musicFileUploadRequest.getId();
        }

        // 如果是更新，判断文件是否已经存在
        MusicFile oldMusicFile = null;
        if (musicFileID != null) {
            oldMusicFile = this.getById(musicFileID);
            ThrowUtils.throwIf(oldMusicFile == null, ErrorCode.NOT_FOUND_ERROR, "文件不存在");
            // 仅本人或者管理员可更新
            if (!oldMusicFile.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }

        // 上传文件，得到文件信息
        // 按照用户id划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadMusicFileResult uploadMusicFileResult = fileManager.uploadAudio(multipartFile, uploadPathPrefix);

        // 构造要入库的文件信息
        MusicFile musicFile = new MusicFile();
        musicFile.setUrl(uploadMusicFileResult.getUrl());
        musicFile.setName(uploadMusicFileResult.getName());
        musicFile.setArtist(uploadMusicFileResult.getArtist());
        musicFile.setAlbum(uploadMusicFileResult.getAlbum());
        musicFile.setIntroduction(uploadMusicFileResult.getIntroduction());
        musicFile.setFileSize(uploadMusicFileResult.getFileSize());

        // 处理封面图片上传
        if (coverFile != null && !coverFile.isEmpty()) {
            // 上传新封面
            String coverUrl = fileManager.uploadImage(coverFile, uploadPathPrefix);
            musicFile.setCoverUrl(coverUrl);
        } else if (StrUtil.isNotBlank(musicFileUploadRequest.getCoverUrl())) {
            // 如果请求中包含了coverUrl，使用该URL
            musicFile.setCoverUrl(musicFileUploadRequest.getCoverUrl());
        } else if (oldMusicFile != null && StrUtil.isNotBlank(oldMusicFile.getCoverUrl())) {
            // 如果是更新且原音乐有封面，保留原封面
            musicFile.setCoverUrl(oldMusicFile.getCoverUrl());
        } else {
            // 使用默认封面
            musicFile.setCoverUrl("https://tse3-mm.cn.bing.net/th/id/OIP-C.1gt9Vw4SBXSmfCxgfXzOcQHaE8?w=239&h=180&c=7&r=0&o=5&dpr=1.6&pid=1.7");
        }

        // 添加空值检查
        if (uploadMusicFileResult.getDuration() != null) {
            musicFile.setDuration(uploadMusicFileResult.getDuration());
        } else {
            musicFile.setDuration(0);  // 设置默认值
        }

        if (uploadMusicFileResult.getBitRate() != null) {
            musicFile.setBitRate(uploadMusicFileResult.getBitRate());
        } else {
            musicFile.setBitRate(0);  // 设置默认值
        }

        musicFile.setFileFormat(uploadMusicFileResult.getFileFormat());
        musicFile.setUserId(loginUser.getId());

        // 补充审核参数
        this.fillReviewParams(musicFile, loginUser);

        // 操作数据库
        // 如果musicFileId不为空，表示更新，否则为新增
        if (musicFileID != null) {
            musicFile.setId(musicFileID);
            musicFile.setEditTime(new Date());

            // 保留原有数据的其他字段（如果需要）
            if (oldMusicFile != null) {
                // 如果需要保留其他字段，在这里添加
                if (musicFile.getCreateTime() == null) {
                    musicFile.setCreateTime(oldMusicFile.getCreateTime());
                }
            }
        } else {
            musicFile.setCreateTime(new Date());
            musicFile.setUpdateTime(new Date());
        }



        boolean result = this.saveOrUpdate(musicFile);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "文件上传失败，数据库操作失败");

        // 重新从数据库查询完整信息，确保返回的是最新数据
        MusicFile savedMusicFile = this.getById(musicFile.getId());

        return MusicFileVO.objToVo(savedMusicFile);
    }

    @Override
    public QueryWrapper<MusicFile> getQueryWrapper(MusicFileQueryRequest MusicFileQueryRequest) {
        QueryWrapper<MusicFile> queryWrapper = new QueryWrapper<>();
        if (MusicFileQueryRequest == null){
            return queryWrapper;
        }
        // 从对象中取值

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

        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
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
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(fileSize), "fileSize", fileSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(duration), "duration", duration);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);

        // Json 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }

        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }



    @Override
    public MusicFileVO getMusicFileVO(MusicFile musicFile, HttpServletRequest request) {
        // 对象转封装类
        MusicFileVO musicFileVO = MusicFileVO.objToVo(musicFile);
        // 关联查询用户信息
        Long userId = musicFile.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            musicFileVO.setUser(userVO);
        }
        return musicFileVO;
    }


    /**
     * 分页获取图片封装
     */
    @Override
    public Page<MusicFileVO> getMusicFileVOPage(Page<MusicFile> musicFilePage, HttpServletRequest request) {
        List<MusicFile> musicFileList = musicFilePage.getRecords();
        Page<MusicFileVO> musicFileVOPage = new Page<>(musicFilePage.getCurrent(), musicFilePage.getSize(), musicFilePage.getTotal());
        if (CollUtil.isEmpty(musicFileList)) {
            return musicFileVOPage;
        }
        // 对象列表 => 封装对象列表
        List<MusicFileVO> musicFileVOList = musicFileList.stream().map(MusicFileVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = musicFileList.stream().map(MusicFile::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
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
        // 从对象中取值
        Long id = musicFile.getId();
        String url = musicFile.getUrl();
        String introduction = musicFile.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        // 如果传入了url就校验
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        // 如果传入了introduction就校验
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }


    @Override
    public void streamMusicFile(Long id, HttpServletResponse response) {
        // 获取音乐文件信息
        MusicFile musicFile = this.getById(id);
        ThrowUtils.throwIf(musicFile == null, ErrorCode.NOT_FOUND_ERROR, "音乐文件不存在");

        // 从URL中提取文件路径
        String url = musicFile.getUrl();

        // 提取COS路径
        String cosHost = cosClientConfig.getHost();
        String filepath = url;
        if (url.startsWith(cosHost)) {
            filepath = url.substring(cosHost.length());
        }

        // 记录播放日志
        log.error("用户正在播放音乐: ID=" + id + ", 名称=" + musicFile.getName() + ", 文件路径=" + filepath);

        COSObjectInputStream cosObjectInput = null;
        try {
            // 使用cosClient而不是cosManager
            GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), filepath);
            COSObject cosObject = cosClient.getObject(getObjectRequest);
            cosObjectInput = cosObject.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);

            // 设置HTTP头，支持音频播放
            String contentType = "audio/mpeg"; // 默认MIME类型
            String fileFormat = musicFile.getFileFormat().toLowerCase();

            // 根据文件格式设置正确的MIME类型
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

            // 设置响应头
            response.setContentType(contentType);
            response.setContentLength(bytes.length);

            // 设置允许范围请求，对于大音频文件和拖动进度条的功能很重要
            response.setHeader("Accept-Ranges", "bytes");

            // 写入音频数据到响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();

        } catch (Exception e) {
            log.error("音频流传输失败，id = " + id + ", 错误: " + e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "音频播放失败：" + e.getMessage());
        } finally {
            // 关闭流
            if (cosObjectInput != null) {
                try {
                    cosObjectInput.close();
                } catch (IOException e) {
                    log.error("关闭COS输入流失败", e);
                }
            }
        }
    }

    @Override
    public List<MusicFileVO> getPlaylistByCategory(String category, HttpServletRequest request) {
        // 创建查询请求对象
        MusicFileQueryRequest queryRequest = new MusicFileQueryRequest();
        queryRequest.setCategory(category);
        queryRequest.setPageSize(100); // 设置一个合理的上限

        // 查询数据库获取该分类下的音乐
        Page<MusicFile> musicFilePage = this.page(
                new Page<>(1, queryRequest.getPageSize()),
                this.getQueryWrapper(queryRequest)
        );

        // 转换为前端所需的VO对象列表
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
        // 判断是否存在
        MusicFile oldMusicFile = this.getById(id);
        ThrowUtils.throwIf(oldMusicFile == null, ErrorCode.NOT_FOUND_ERROR);
        // 已是该状态
        if (oldMusicFile.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 更新审核状态
        MusicFile updateMusicFile = new MusicFile();
        BeanUtils.copyProperties(musicFileReviewRequest, updateMusicFile);
        updateMusicFile.setReviewerId(loginUser.getId());
        updateMusicFile.setReviewTime(new Date());
        boolean result = this.updateById(updateMusicFile);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }


    /**
     * 填充审核参数
     * @param musicFile
     * @param loginUser
     *
     *
     */
    @Override
    public void fillReviewParams(MusicFile musicFile, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审核
            musicFile.setReviewStatus(MusicFileReviewStatusEnum.PASS.getValue());
            musicFile.setReviewMessage("管理员自动过审核");
            musicFile.setReviewerId(loginUser.getId());
            musicFile.setReviewTime(new Date());
        } else {
            // 非管理员，无论是编辑还是创建，都是需要审核
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
     * 获取用于模糊搜索的查询包装器
     * 该方法允许在多个字段（标签、分类、歌名、艺术家等）中进行模糊搜索
     *
     * @param musicFileQueryRequest 查询请求对象，包含搜索文本
     * @return 配置了模糊搜索条件的查询包装器
     */
    @Override
    public QueryWrapper<MusicFile> getFuzzySearchQueryWrapper(MusicFileQueryRequest musicFileQueryRequest) {
        QueryWrapper<MusicFile> queryWrapper = new QueryWrapper<>();
        if (musicFileQueryRequest == null) {
            return queryWrapper;
        }

        // 获取搜索关键词
        String searchText = musicFileQueryRequest.getSearchText();
        Integer reviewStatus = musicFileQueryRequest.getReviewStatus();

        // 默认只显示已审核通过的音乐
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);

        // 如果搜索文本为空，直接返回
        if (StrUtil.isBlank(searchText)) {
            return queryWrapper;
        }

        // 构建多字段模糊搜索
        queryWrapper.and(wrapper -> {
            // 搜索歌曲名称
            wrapper.like("name", searchText)
                   // 搜索艺术家
                   .or().like("artist", searchText)
                   // 搜索专辑名
                   .or().like("album", searchText)
                   // 搜索简介
                   .or().like("introduction", searchText)
                   // 搜索分类
                   .or().like("category", searchText)
                   // 搜索标签 (标签以JSON格式存储)
                   .or().like("tags", searchText);
        });

        // 应用分页排序条件
        String sortField = musicFileQueryRequest.getSortField();
        String sortOrder = musicFileQueryRequest.getSortOrder();
        if (StrUtil.isNotEmpty(sortField)) {
            queryWrapper.orderBy(true, sortOrder != null && sortOrder.equals("ascend"), sortField);
        } else {
            // 默认按创建时间降序
            queryWrapper.orderByDesc("createTime");
        }

        return queryWrapper;
    }

}




