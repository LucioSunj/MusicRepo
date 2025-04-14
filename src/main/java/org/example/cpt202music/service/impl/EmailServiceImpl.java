package org.example.cpt202music.service.impl;

import cn.hutool.core.util.RandomUtil;
import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.cpt202music.constant.RedisConstants;
import org.example.cpt202music.model.vo.EmailVO;
import org.example.cpt202music.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Resource
    private JavaMailSenderImpl javaMailSender;

    @Value("${spring.mail.username}")
    private String sendMailer;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Sends a verification code to the specified email address.
     * Generates a code, stores it in Redis, and sends the email.
     *
     * @param receiveEmail The recipient's email address.
     * @return true if the email was sent successfully, false otherwise.
     */
    @Override
    public boolean sendVerificationCode(String receiveEmail, EmailVO emilVO) {
        if (StringUtils.isEmpty(receiveEmail)) {
             log.error("Recipient email cannot be empty.");
             // Consider throwing a custom exception or returning a specific error response
             return false;
        }

        String subject = "Your Verification Code";
        // Generate 6-digit verification code using Hutool
        String verifyCode = RandomUtil.randomNumbers(6);
        String emailMsg = "Dear User,\n\nYour verification code is: " + verifyCode
                + "\nThis code is valid for 5 minutes. Please enter it promptly. (Do not share this code)\n\n"
                + "If you did not request this, please ignore this email.\n(This is an automated message, please do not reply directly)";

        // Store the verification code in Redis with a 5-minute expiry
        String redisKey = RedisConstants.VERIFY_CODE_KEY + receiveEmail;
        try {
            stringRedisTemplate.opsForValue().set(redisKey, verifyCode, RedisConstants.VERIFY_CODE_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("Verification code for {} stored in Redis. Key: {}", receiveEmail, redisKey);
        } catch (Exception e) {
            log.error("Failed to store verification code in Redis for email {}: {}", receiveEmail, e.getMessage(), e);
            return false; // Fail if Redis storage fails
        }
        emilVO.setCode(verifyCode);
        emilVO.setCreateTime(new Date());
        emilVO.setEmail(receiveEmail);
        // Send the email
        return sendTextMail(receiveEmail, subject, emailMsg);

    }

   


    /**
     * Sends a plain text email.
     * @param receiveEmail Recipient(s), comma-separated for multiple.
     * @param subject Subject of the email.
     * @param emailMsg Content of the email.
     * @return true if sending was successful, false otherwise.
     */
    private boolean sendTextMail(String receiveEmail, String subject, String emailMsg) {
        // Parameter check (basic validation)
        if (!isValidEmailInput(receiveEmail, subject, emailMsg)) {
            return false;
        }

        try {
            // Use MimeMessageHelper for richer email features if needed, but okay for text.
            // true indicates multipart message (allows attachments, HTML), but also works for plain text.
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true); // true for multipart support

            // Set email details
            mimeMessageHelper.setFrom(sendMailer);
            mimeMessageHelper.setTo(receiveEmail.split(",")); // Handles single or multiple recipients
            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setText(emailMsg); // Set content as plain text
            mimeMessageHelper.setSentDate(new Date());

            // Send the email
            javaMailSender.send(mimeMessage);
            log.info("Email sent successfully from {} to {}", sendMailer, receiveEmail);
            return true;
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", receiveEmail, e.getMessage(), e);
            return false;
        } catch (Exception e) {
             log.error("An unexpected error occurred while sending email to {}: {}", receiveEmail, e.getMessage(), e);
             return false;
        }
    }

    /**
     * Validates basic email parameters.
     */
    private boolean isValidEmailInput(String receiveEmail, String subject, String emailMsg) {
        if (StringUtils.isBlank(receiveEmail)) {
            log.error("Email recipient cannot be empty.");
            return false;
        }
        if (StringUtils.isBlank(subject)) {
            log.error("Email subject cannot be empty.");
            return false;
        }
        if (StringUtils.isBlank(emailMsg)) {
           log.error("Email content cannot be empty.");
           return false;
        }
        // Add more validation if needed (e.g., basic email format check)
        return true;
    }

    @Override
    public boolean checkVerificationCode(String receiveEmail, String code) {
        if (StringUtils.isEmpty(receiveEmail) || StringUtils.isEmpty(code)) {
            log.error("Email or verification code cannot be empty");
            return false;
        }

        // Get the stored verification code from Redis
        String redisKey = RedisConstants.VERIFY_CODE_KEY + receiveEmail;
        log.info("Attempting to get verification code from Redis with key: {}", redisKey);
        
        try {
            String storedCode = stringRedisTemplate.opsForValue().get(redisKey);
            log.info("Retrieved code from Redis: {}", storedCode);
            
            if (storedCode == null) {
                log.error("No verification code found for email: {}", receiveEmail);
                return false;
            }

            // Compare the codes
            boolean isValid = storedCode.equals(code);
            
            if (isValid) {
                // Delete the verification code from Redis after successful verification
                stringRedisTemplate.delete(redisKey);
                log.info("Verification code validated successfully for email: {}", receiveEmail);
            } else {
                log.error("Invalid verification code for email: {}. Expected: {}, Got: {}", receiveEmail, storedCode, code);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error while checking verification code: {}", e.getMessage(), e);
            return false;
        }
    }

    // Generates a 6-digit verification code
    private String creatMailCode() {
        StringBuilder sb = new StringBuilder();
        Random rand = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(rand.nextInt(10));
        }
        return sb.toString();
    }
} 