# Notification Service

A Spring Boot microservice for handling various types of notifications (Email, SMS, and In-App) with Kafka integration and retry mechanism.

## Features

- Multiple notification types support (Email, SMS, In-App)
- Asynchronous processing using Apache Kafka
- Persistent storage with MongoDB
- Automatic retry mechanism for failed notifications
- Email notifications using Gmail SMTP
- RESTful API endpoints
- Docker support for easy deployment

## Technologies Used

- Java 17
- Spring Boot 3.5.0
- Apache Kafka
- MongoDB
- Docker & Docker Compose
- Spring Mail

## Prerequisites

Before running the application, make sure you have the following installed:
- Java 17 or higher
- Docker and Docker Compose
- Maven (or use the included Maven wrapper)

## Setup and Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd notification-service
```

2. Start the required services using Docker Compose:
```bash
docker-compose up -d
```

This will start:
- MongoDB on port 27017
- Zookeeper on port 2181
- Kafka on port 9092

3. Run the application:
```bash
./mvnw spring-boot:run
```

The service will start on port 8080.

## API Endpoints

### 1. Send a Notification
```
POST /api/v1/notifications

Request Body:
{
    "userId": "user123",
    "recipientEmail": "recipient@example.com",
    "message": "Your notification message",
    "type": "EMAIL"
}

Response:
{
    "id": "...",
    "userId": "user123",
    "recipientEmail": "recipient@example.com",
    "message": "Your notification message",
    "type": "EMAIL",
    "status": "PENDING",
    "createdAt": "...",
    "updatedAt": "...",
    "retryCount": 0
}
```

### 2. Get User Notifications
```
GET /api/v1/users/{userId}/notifications

Response:
[
    {
        "id": "...",
        "userId": "user123",
        "recipientEmail": "recipient@example.com",
        "message": "...",
        "type": "...",
        "status": "...",
        "createdAt": "...",
        "updatedAt": "...",
        "retryCount": 0
    }
]
```

## Notification Types

1. **Email Notifications**
   - Sends emails using Gmail SMTP
   - Supports HTML content
   - Automatic retries on failure

2. **SMS Notifications** (Placeholder for future implementation)
   - Structure ready for SMS gateway integration

3. **In-App Notifications** (Placeholder for future implementation)
   - Structure ready for real-time notifications

## Configuration

Key configurations in `application.properties`:

```properties
# Server Configuration
server.port=8080

# MongoDB Configuration
spring.data.mongodb.uri=mongodb://localhost:27017/notifications

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=notification-group

# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

## Architecture

- **Controller Layer**: Handles HTTP requests and responses
- **Service Layer**: Contains business logic and notification processing
- **Consumer Layer**: Processes messages from Kafka queue
- **Model Layer**: Data models and entities
- **MongoDB**: Persistent storage for notifications
- **Kafka**: Message queue for asynchronous processing

## Error Handling

- Failed notifications are automatically retried up to 3 times
- Each retry attempt is logged and tracked
- Final failure status is recorded in MongoDB
- Comprehensive error logging for debugging

## Security Notes

- Email credentials should be secured using environment variables
- SMTP configuration supports TLS
- MongoDB connection can be secured with authentication

## Future Enhancements

1. Implement SMS notification functionality
2. Add real-time in-app notifications
3. Add authentication and authorization
4. Add notification templates
5. Implement rate limiting
6. Add notification scheduling
7. Implement batch notification processing

## Troubleshooting

1. **Email Not Sending**
   - Verify SMTP credentials
   - Check email configuration in application.properties
   - Ensure valid recipient email address

2. **Kafka Issues**
   - Verify Kafka is running: `docker ps`
   - Check Kafka logs: `docker logs kafka-service`
   - Ensure topic exists and is accessible

3. **MongoDB Issues**
   - Verify MongoDB is running: `docker ps`
   - Check MongoDB connection string
   - Ensure database permissions are correct

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
