package com.filestorage.dto;

import java.time.LocalDateTime;

public class FolderDTO {
    private Long id;
    private String name;
    private Long parentFolderId;
    private LocalDateTime createdAt;

    public FolderDTO() {
    }

    public FolderDTO(Long id, String name, Long parentFolderId, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.parentFolderId = parentFolderId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(Long parentFolderId) {
        this.parentFolderId = parentFolderId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
