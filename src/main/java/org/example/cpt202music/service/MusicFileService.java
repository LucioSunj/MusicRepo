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
* @description Database operation Service for table【music_file】
* @createDate 2025-04-12 18:57:42
*/
public interface MusicFileService extends IService<MusicFile> {



    /**
     * Upload music file
     *
     * @param multipartFile
     * @param musicFileUploadRequest
     * @param loginUser
     * @return
     */
    MusicFileVO uploadMusicFile(MultipartFile multipartFile, MusicFileUploadRequest musicFileUploadRequest, User loginUser, MultipartFile coverFile);

    /**
     * Get query wrapper
     *
     * @param MusicFileQueryRequest
     * @return
     */
    QueryWrapper<MusicFile> getQueryWrapper(MusicFileQueryRequest MusicFileQueryRequest);

    /**
     * Get single query object
     * @param musicFile
     * @param request
     * @return
     */
    MusicFileVO getMusicFileVO(MusicFile musicFile, HttpServletRequest request);


    /**
     * Get paginated music file wrappers
     */
    Page<MusicFileVO> getMusicFileVOPage(Page<MusicFile> musicFilePage, HttpServletRequest request);

    /**
     * Validate music file information
     * @param musicFile
     */
    void validMusicFile(MusicFile musicFile);


    /**
     * Stream music file
     *
     * @param id Music file ID
     * @param response HTTP response
     */
    void streamMusicFile(Long id, HttpServletResponse response);

    /**
     * Get playlist by category
     *
     * @param category Category
     * @param request HTTP request
     * @return Music list
     */
    List<MusicFileVO> getPlaylistByCategory(String category, HttpServletRequest request);


    /**
     * Review music file
     *
     * @param musicFileReviewRequest
     * @param loginUser
     */
    void doMusicFileReview(MusicFileReviewRequest musicFileReviewRequest, User loginUser);


    /**
     * Fill review parameters
     * @param musicFile
     * @param loginUser
     */
    void fillReviewParams(MusicFile musicFile, User loginUser);

    /**
     * Get query wrapper for approved music files
     *
     * @return Query wrapper for approved music files
     */
    QueryWrapper<MusicFile> getApprovedMusicQueryWrapper();

    /**
     * Get query wrapper for fuzzy search
     * This method is used for fuzzy searching across multiple fields (tags, categories, song names, artists, etc.)
     *
     * @param musicFileQueryRequest Query request object containing search text
     * @return Query wrapper configured with fuzzy search conditions
     */
    QueryWrapper<MusicFile> getFuzzySearchQueryWrapper(MusicFileQueryRequest musicFileQueryRequest);
}
