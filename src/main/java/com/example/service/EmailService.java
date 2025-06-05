package com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    @Autowired
    private JavaMailSender emailSender;

    public void sendEmail(String to, String subject, String text) {
        if (!isValidEmail(to)) {
            logger.error("Invalid email address: {}", to);
            throw new IllegalArgumentException("Invalid email address: " + to);
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            emailSender.send(message);
            logger.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
            throw e;
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
}
