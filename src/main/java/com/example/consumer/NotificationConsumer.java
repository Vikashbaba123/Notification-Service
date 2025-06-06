package com.example.consumer;

import com.example.model.Notification;
import com.example.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {
    private static final Logger logger = LoggerFactory.getLogger(NotificationConsumer.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topic.notification}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message, Acknowledgment acknowledgment) {
        try {
            logger.info("Processing notification message: {}", message);
            Notification notification = objectMapper.readValue(message, Notification.class);
            notificationService.processNotification(notification);
            acknowledgment.acknowledge();
            logger.info("Successfully processed and acknowledged notification: {}", notification.getId());
        } catch (JsonProcessingException e) {
            logger.error("Error parsing notification message: {}", e.getMessage());
            throw new RuntimeException("Error processing message", e);
        } catch (Exception e) {
            logger.error("Error processing notification message: {}", e.getMessage());
            throw e;
        }
    }
}
