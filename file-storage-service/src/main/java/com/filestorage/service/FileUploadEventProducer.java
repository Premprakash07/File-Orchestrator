package com.filestorage.service;

import com.filestorage.dto.FileUploadCompletedEvent;
import com.filestorage.model.FileMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class FileUploadEventProducer {

    private static final Logger logger = LogManager.getLogger(FileUploadEventProducer.class);

    private final KafkaTemplate<String, FileUploadCompletedEvent> kafkaTemplate;
    private final String fileUploadCompletedTopic;

    public FileUploadEventProducer(
            KafkaTemplate<String, FileUploadCompletedEvent> kafkaTemplate,
            @Value("${app.kafka.topic.file-upload-completed}") String fileUploadCompletedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.fileUploadCompletedTopic = fileUploadCompletedTopic;
    }

    public void publishUploadCompleted(FileMetadata fileMetadata) {
        FileUploadCompletedEvent event = new FileUploadCompletedEvent();
        event.setFileId(fileMetadata.getId());
        event.setUserId(fileMetadata.getUser().getId());
        event.setFolderId(fileMetadata.getFolder() != null ? fileMetadata.getFolder().getId() : null);
        event.setFileName(fileMetadata.getFileName());
        event.setFileType(fileMetadata.getFileType());
        event.setFileSize(fileMetadata.getFileSize());
        event.setStorageType(fileMetadata.getStorageType());
        event.setFilePath(fileMetadata.getFilePath());
        event.setS3Key(fileMetadata.getS3Key());
        event.setUploadedAt(fileMetadata.getUploadedAt());

        String messageKey = String.valueOf(fileMetadata.getUser().getId());
        kafkaTemplate.send(fileUploadCompletedTopic, messageKey, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to publish file upload completion event for file ID: {}",
                                fileMetadata.getId(), ex);
                        return;
                    }

                    if (result != null && result.getRecordMetadata() != null) {
                        logger.info(
                                "Published file upload completion event for file ID: {} to topic: {}, partition: {}, offset: {}",
                                fileMetadata.getId(),
                                fileUploadCompletedTopic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        logger.info("Published file upload completion event for file ID: {} to topic: {}",
                                fileMetadata.getId(), fileUploadCompletedTopic);
                    }
                });
    }
}
