package org.example.cpt202music.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.cpt202music.model.dto.MusicFile.MusicFileQueryRequest;
import org.example.cpt202music.model.dto.MusicFile.MusicFileReviewRequest;
import org.example.cpt202music.model.dto.MusicFile.MusicFileUploadRequest;
import org.example.cpt202music.model.entity.MusicFile ;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.cpt202music.model.entity.User;
import org.example.cpt202music.model.vo.MusicFileVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
* @author XLW200420
* @description 针对表【music_file(音乐文件)】的数据库操作Service
* @createDate 2025-04-12 18:57:42
*/
public interface MusicFileService extends IService<MusicFile> {



    /**
     * 上传图片
     *
     * @param multipartFile
     * @param musicFileUploadRequest
     * @param loginUser
     * @return
     */
    MusicFileVO uploadMusicFile(MultipartFile multipartFile, MusicFileUploadRequest musicFileUploadRequest, User loginUser, MultipartFile coverFile);

    /**
     * 获取查询对象
     *
     * @param MusicFileQueryRequest
     * @return
     */
    QueryWrapper<MusicFile> getQueryWrapper(MusicFileQueryRequest MusicFileQueryRequest);

    /**
     * 获取查询对象(单个)
     * @param musicFile
     * @param request
     * @return
     */
    MusicFileVO getMusicFileVO(MusicFile musicFile, HttpServletRequest request);


    /**
     * 分页获取图片封装
     */
    Page<MusicFileVO> getMusicFileVOPage(Page<MusicFile> musicFilePage, HttpServletRequest request);

    /**
     * 校验图片信息
     * @param musicFile
     */
    void validMusicFile(MusicFile musicFile);


    /**
     * 获取音乐文件流
     *
     * @param id 音乐文件ID
     * @param response HTTP响应
     */
    void streamMusicFile(Long id, HttpServletResponse response);

    /**
     * 获取指定分类的播放列表
     *
     * @param category 分类
     * @param request HTTP请求
     * @return 音乐列表
     */
    List<MusicFileVO> getPlaylistByCategory(String category, HttpServletRequest request);


    /**
     * 图片审核
     *
     * @param musicFileReviewRequest
     * @param loginUser
     */
    void doMusicFileReview(MusicFileReviewRequest musicFileReviewRequest, User loginUser);


    /**
     * 填充审核参数
     * @param musicFile
     * @param loginUser
     */
    void fillReviewParams(MusicFile musicFile, User loginUser);

    /**
     * 获取已审核的音乐文件查询包装器
     *
     * @return 已审核的音乐文件查询包装器
     */
    QueryWrapper<MusicFile> getApprovedMusicQueryWrapper();
}
