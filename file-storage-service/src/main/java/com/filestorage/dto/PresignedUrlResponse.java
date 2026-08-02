package com.filestorage.dto;

public class PresignedUrlResponse {
    private String uploadUrl;
    private String s3Key;
    private Long expirationMinutes;

    public PresignedUrlResponse() {
    }

    public PresignedUrlResponse(String uploadUrl, String s3Key, Long expirationMinutes) {
        this.uploadUrl = uploadUrl;
        this.s3Key = s3Key;
        this.expirationMinutes = expirationMinutes;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public Long getExpirationMinutes() {
        return expirationMinutes;
    }

    public void setExpirationMinutes(Long expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }
}
