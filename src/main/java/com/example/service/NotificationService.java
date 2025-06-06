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
        notification.setRetryCount(0);
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
        logger.info("Processing notification attempt #{} for notification ID: {}",
            notification.getRetryCount() + 1, notification.getId());

        try {
            if (notification.getRetryCount() < 2) {
                notification.incrementRetryCount();
                mongoTemplate.save(notification);
                throw new RuntimeException("Simulated failure for testing retry mechanism - Attempt: "
                    + notification.getRetryCount());
            }

            emailService.sendEmail(notification.getRecipientEmail(), "Notification", notification.getMessage());
            notification.setStatus(NotificationStatus.SENT);
            logger.info("Successfully processed notification after {} attempts. ID: {}",
                notification.getRetryCount() + 1, notification.getId());
            mongoTemplate.save(notification);
        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    private void handleNotificationFailure(Notification notification, Exception e) {
        logger.error("Failed to process notification: {} - Error: {}", notification.getId(), e.getMessage());

        if (notification.getRetryCount() >= MAX_RETRY_ATTEMPTS) {
            notification.setStatus(NotificationStatus.FAILED);
            logger.error("Max retry attempts reached for notification: {}", notification.getId());
        } else {
            notification.setStatus(NotificationStatus.PENDING);
            try {
                String notificationJson = objectMapper.writeValueAsString(notification);
                kafkaTemplate.send(notificationTopic, notification.getId(), notificationJson);
                logger.info("Notification requeued for retry: {}. Current retry count: {}",
                    notification.getId(), notification.getRetryCount());
            } catch (Exception ex) {
                logger.error("Error requeuing notification: {}", ex.getMessage());
                notification.setStatus(NotificationStatus.FAILED);
            }
        }
        mongoTemplate.save(notification);
    }
}
