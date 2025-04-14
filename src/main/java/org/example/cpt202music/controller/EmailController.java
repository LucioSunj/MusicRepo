package org.example.cpt202music.controller;


import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import org.example.cpt202music.model.vo.EmailVO;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.cpt202music.annotation.AuthCheck;
import org.example.cpt202music.common.BaseResponse;
import org.example.cpt202music.common.DeleteRequest;
import org.example.cpt202music.common.ResultUtils;
import org.example.cpt202music.constant.UserConstant;
import org.example.cpt202music.exception.BusinessException;
import org.example.cpt202music.exception.ErrorCode;
import org.example.cpt202music.exception.ThrowUtils;
import org.example.cpt202music.model.dto.email.EmailRequest;
import org.example.cpt202music.model.vo.EmailVO;
import org.example.cpt202music.service.EmailService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/*@author LucioSun
 * version 1.0
 */
@RestController
@RequestMapping("/email")
public class EmailController {

    @Resource
    private EmailService emailService;

    @PostMapping("/verification-code/send")
    public BaseResponse<EmailVO> sendVerificationCode(@RequestBody EmailRequest emailRequest) {
        ThrowUtils.throwIf(emailRequest == null || emailRequest.getEmail() == null, 
            ErrorCode.PARAMS_ERROR, "Email cannot be null");
        
        EmailVO emailVO = new EmailVO();
        boolean success = emailService.sendVerificationCode(emailRequest.getEmail(), emailVO);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "Failed to send verification code");
        
        return ResultUtils.success(emailVO);
    }

    @PostMapping("/verification-code/check")
    public BaseResponse<Boolean> checkVerificationCode(@RequestBody EmailRequest emailRequest) {
        ThrowUtils.throwIf(emailRequest == null || emailRequest.getEmail() == null || emailRequest.getCode() == null,
            ErrorCode.PARAMS_ERROR, "Email or code cannot be null");

        boolean success = emailService.checkVerificationCode(emailRequest.getEmail(), emailRequest.getCode());
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "Failed to check verification code");
        return ResultUtils.success(success);
    }
}