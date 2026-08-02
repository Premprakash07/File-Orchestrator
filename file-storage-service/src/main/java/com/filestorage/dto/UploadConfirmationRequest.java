package com.filestorage.dto;

public class UploadConfirmationRequest {
    private String s3Key;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Long folderId;

    public UploadConfirmationRequest() {
    }

    public UploadConfirmationRequest(String s3Key, String fileName, String fileType, Long fileSize, Long folderId) {
        this.s3Key = s3Key;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.folderId = folderId;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Long getFolderId() {
        return folderId;
    }

    public void setFolderId(Long folderId) {
        this.folderId = folderId;
    }
}
