package com.filestorage.service;

import com.filestorage.dto.PresignedUrlResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;

@Service
public class S3Service {

    private static final Logger logger = LogManager.getLogger(S3Service.class);

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Value("${aws.s3.presigned-url-expiration}")
    private Long presignedUrlExpirationMinutes;

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @PostConstruct
    public void initialize() {
        logger.info("Initializing S3 service with region: {} and bucket: {}", region, bucketName);

        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

        s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();

        s3Presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();

        logger.info("S3 service initialized successfully");
    }

    public PresignedUrlResponse generatePresignedUploadUrl(String fileName, String contentType, Long userId) {
        logger.debug("Generating presigned URL for file: {} by user ID: {}", fileName, userId);

        // Generate unique S3 key
        String s3Key = String.format("%d/%s_%s", userId, UUID.randomUUID().toString(), fileName);
        HashMap<String, String> metaData = new HashMap<>();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpirationMinutes))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String presignedUrl = presignedRequest.url().toString();

        logger.info("Presigned URL generated successfully for S3 key: {}", s3Key);

        return new PresignedUrlResponse(presignedUrl, s3Key, presignedUrlExpirationMinutes);
    }

    public void deleteFile(String s3Key) {
        logger.info("Deleting file from S3: {}", s3Key);

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            logger.info("File deleted successfully from S3: {}", s3Key);
        } catch (Exception e) {
            logger.error("Failed to delete file from S3: {}. Error: {}", s3Key, e.getMessage(), e);
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }

    public String getFileUrl(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
    }
}
