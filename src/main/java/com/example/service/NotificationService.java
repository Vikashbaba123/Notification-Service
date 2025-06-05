package com.example.service;

import com.example.model.Notification;
import com.example.model.Notification.NotificationStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailService emailService;

    @Value("${kafka.topic.notification}")
    private String notificationTopic;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public Notification createNotification(Notification notification) {
        notification.setStatus(NotificationStatus.PENDING);
        Notification savedNotification = mongoTemplate.save(notification);
        try {
            String notificationJson = objectMapper.writeValueAsString(savedNotification);
            kafkaTemplate.send(notificationTopic, savedNotification.getId(), notificationJson);
            logger.info("Notification queued successfully: {}", savedNotification.getId());
        } catch (Exception e) {
            logger.error("Error queuing notification: {}", e.getMessage());
            savedNotification.setStatus(NotificationStatus.FAILED);
            mongoTemplate.save(savedNotification);
        }
        return savedNotification;
    }

    public List<Notification> getUserNotifications(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        return mongoTemplate.find(query, Notification.class);
    }

    public void processNotification(Notification notification) {
        try {
            switch (notification.getType()) {
                case EMAIL:
                    sendEmail(notification);
                    break;
                case SMS:
                    sendSMS(notification);
                    break;
                case IN_APP:
                    sendInAppNotification(notification);
                    break;
            }
            notification.setStatus(NotificationStatus.SENT);
            mongoTemplate.save(notification);
            logger.info("Notification processed successfully: {}", notification.getId());
        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    private void handleNotificationFailure(Notification notification, Exception e) {
        logger.error("Failed to process notification: {} - Error: {}", notification.getId(), e.getMessage());
        notification.incrementRetryCount();

        if (notification.getRetryCount() >= MAX_RETRY_ATTEMPTS) {
            notification.setStatus(NotificationStatus.FAILED);
            logger.error("Max retry attempts reached for notification: {}", notification.getId());
        } else {
            notification.setStatus(NotificationStatus.PENDING);
            try {
                String notificationJson = objectMapper.writeValueAsString(notification);
                kafkaTemplate.send(notificationTopic, notification.getId(), notificationJson);
                logger.info("Notification requeued for retry: {}", notification.getId());
            } catch (Exception ex) {
                logger.error("Error requeuing notification: {}", ex.getMessage());
                notification.setStatus(NotificationStatus.FAILED);
            }
        }
        mongoTemplate.save(notification);
    }

    private void sendEmail(Notification notification) {
        try {
            String recipientEmail = notification.getRecipientEmail() != null ?
                notification.getRecipientEmail() : notification.getUserId();

            emailService.sendEmail(
                recipientEmail,
                "Important Notification",
                notification.getMessage()
            );
            logger.info("Email sent successfully for notification: {} to recipient: {}",
                notification.getId(), recipientEmail);
        } catch (Exception e) {
            logger.error("Failed to send email for notification {}: {}",
                notification.getId(), e.getMessage());
            throw e;
        }
    }

    private void sendSMS(Notification notification) {
        logger.info("Sending SMS notification: {}", notification.getId());
    }

    private void sendInAppNotification(Notification notification) {
        logger.info("Sending in-app notification: {}", notification.getId());
    }
}
