package com.filestorage.service;

import com.filestorage.dto.FileUploadCompletedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class FileUploadEventConsumer {

    private static final Logger logger = LogManager.getLogger(FileUploadEventConsumer.class);

    @KafkaListener(
        topics = "${app.kafka.topic.file-upload-completed}",
        groupId = "${spring.kafka.consumer.group-id:file-upload-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeFileUploadEvent(FileUploadCompletedEvent event) {
        try {
            logger.info(
                "Received file upload completion event - File ID: {}, User ID: {}, File Name: {}, File Size: {}",
                event.getFileId(),
                event.getUserId(),
                event.getFileName(),
                event.getFileSize()
            );

            // Process the event - add your business logic here
            handleFileUploadCompleted(event);

            logger.info("Successfully processed file upload completion event for file ID: {}", event.getFileId());
        } catch (Exception e) {
            logger.error("Error processing file upload completion event for file ID: {}", event.getFileId(), e);
            // Consider throwing exception to trigger retry or dead-letter queue
            throw new RuntimeException("Failed to process file upload event", e);
        }
    }

    /**
     * Handle the file upload completion event.
     * Add your business logic here (e.g., update file status, trigger notifications, etc.)
     */
    private void handleFileUploadCompleted(FileUploadCompletedEvent event) {
        logger.debug(
            "Processing file upload: File ID={}, Folder ID={}, Storage Type={}, S3 Key={}",
            event.getFileId(),
            event.getFolderId(),
            event.getStorageType(),
            event.getS3Key()
        );

        // TODO: Add business logic here
        // Examples:
        // - Update file status in database
        // - Send notifications to users
        // - Update folder metadata
        // - Trigger analytics or indexing
        // - Update cache
    }
}
