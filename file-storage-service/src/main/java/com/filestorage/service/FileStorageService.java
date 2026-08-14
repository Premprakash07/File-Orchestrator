package com.filestorage.service;

import com.filestorage.dto.FileDTO;
import com.filestorage.dto.UploadConfirmationRequest;
import com.filestorage.model.FileMetadata;
import com.filestorage.model.Folder;
import com.filestorage.model.User;
import com.filestorage.repository.FileMetadataRepository;
import com.filestorage.repository.FolderRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private static final Logger logger = LogManager.getLogger(FileStorageService.class);

    @Value("${file.storage.location}")
    private String storageLocation;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private S3Service s3Service;

     @Autowired
     private FileUploadEventProducer fileUploadEventProducer;

    public FileDTO storeFile(MultipartFile file, User user, Long folderId) throws IOException {
        logger.debug("Storing file: {} for user ID: {} in folder ID: {}", file.getOriginalFilename(), user.getId(),
                folderId);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID().toString() + "_" + originalFilename;

        Path userStoragePath = Paths.get(storageLocation, user.getId().toString());
        Files.createDirectories(userStoragePath);
        logger.debug("User storage path created/verified: {}", userStoragePath);

        Path targetLocation = userStoragePath.resolve(storedFileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        logger.debug("File copied to: {}", targetLocation);

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setFileName(originalFilename);
        fileMetadata.setStoredFileName(storedFileName);
        fileMetadata.setFileType(file.getContentType());
        fileMetadata.setStorageType("LOCAL");
        fileMetadata.setFileSize(file.getSize());
        fileMetadata.setFilePath(targetLocation.toString());
        fileMetadata.setUser(user);

        if (folderId != null) {
            Folder folder = folderRepository.findByIdAndUser(folderId, user)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));
            fileMetadata.setFolder(folder);
        }

        fileMetadata = fileMetadataRepository.save(fileMetadata);
        logger.info("File stored successfully: {} with metadata ID: {}", originalFilename, fileMetadata.getId());

         fileUploadEventProducer.publishUploadCompleted(fileMetadata);

        return convertToDTO(fileMetadata);
    }

    public FileDTO saveS3FileMetadata(UploadConfirmationRequest request, User user) {
        logger.debug("Saving S3 file metadata: {} for user ID: {}", request.getFileName(), user.getId());

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setFileName(request.getFileName());
        fileMetadata.setStoredFileName(request.getFileName());
        fileMetadata.setFileType(request.getFileType());
        fileMetadata.setFileSize(request.getFileSize());
        fileMetadata.setFilePath(s3Service.getFileUrl(request.getS3Key()));
        fileMetadata.setS3Key(request.getS3Key());
        fileMetadata.setStorageType("S3");
        fileMetadata.setUser(user);

        if (request.getFolderId() != null) {
            Folder folder = folderRepository.findByIdAndUser(request.getFolderId(), user)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));
            fileMetadata.setFolder(folder);
        }

        fileMetadata = fileMetadataRepository.save(fileMetadata);
        logger.info("S3 file metadata saved successfully: {} with ID: {}", request.getFileName(), fileMetadata.getId());

        // fileUploadEventProducer.publishUploadCompleted(fileMetadata);

        return convertToDTO(fileMetadata);
    }

    public Resource loadFileAsResource(Long fileId, User user) throws MalformedURLException {
        logger.debug("Loading file as resource: ID {} for user ID: {}", fileId, user.getId());

        FileMetadata fileMetadata = fileMetadataRepository.findByIdAndUser(fileId, user)
                .orElseThrow(() -> {
                    logger.warn("File not found: ID {} for user ID: {}", fileId, user.getId());
                    return new RuntimeException("File not found");
                });

        if ("S3".equals(fileMetadata.getStorageType())) {
            // For S3, get the file URL (should be a presigned URL or public URL)
            String s3Url = fileMetadata.getFilePath();
            Resource resource = new UrlResource(s3Url);
            // Optionally, you could check if the URL is reachable, but UrlResource.exists()
            // may not work for presigned URLs
            logger.debug("S3 resource URL loaded: {}", s3Url);
            return resource;
        } else {
            Path filePath = Paths.get(fileMetadata.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                logger.debug("Resource loaded successfully: {}", fileMetadata.getFileName());
                return resource;
            } else {
                logger.error("File not found on disk: {}", filePath);
                throw new RuntimeException("File not found");
            }
        }
    }

    public List<FileDTO> getUserFiles(User user, Long folderId) {
        logger.debug("Fetching files for user ID: {} in folder ID: {}", user.getId(), folderId);

        List<FileMetadata> files;
        if (folderId == null) {
            files = fileMetadataRepository.findByUserAndFolderIsNull(user);
        } else {
            Folder folder = folderRepository.findByIdAndUser(folderId, user)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));
            files = fileMetadataRepository.findByUserAndFolder(user, folder);
        }

        logger.debug("Found {} files for user ID: {}", files.size(), user.getId());
        return files.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public void deleteFile(Long fileId, User user) throws IOException {
        logger.info("Deleting file: ID {} for user ID: {}", fileId, user.getId());

        FileMetadata fileMetadata = fileMetadataRepository.findByIdAndUser(fileId, user)
                .orElseThrow(() -> {
                    logger.warn("File not found for deletion: ID {} for user ID: {}", fileId, user.getId());
                    return new RuntimeException("File not found");
                });

        // Check storage type and delete accordingly
        if ("S3".equals(fileMetadata.getStorageType()) && fileMetadata.getS3Key() != null) {
            // Delete from S3
            try {
                s3Service.deleteFile(fileMetadata.getS3Key());
                logger.debug("S3 file deleted: {}", fileMetadata.getS3Key());
            } catch (Exception e) {
                logger.error("Failed to delete S3 file: {}. Error: {}", fileMetadata.getS3Key(), e.getMessage(), e);
                // Continue to delete metadata even if S3 deletion fails
            }
        } else {
            // Delete local file
            Path filePath = Paths.get(fileMetadata.getFilePath());
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                logger.debug("Physical file deleted: {}", filePath);
            } else {
                logger.warn("Physical file not found for deletion: {}", filePath);
            }
        }

        fileMetadataRepository.delete(fileMetadata);
        logger.info("File metadata deleted successfully: ID {}", fileId);
    }

    private FileDTO convertToDTO(FileMetadata fileMetadata) {
        FileDTO dto = new FileDTO();
        dto.setId(fileMetadata.getId());
        dto.setFileName(fileMetadata.getFileName());
        dto.setFileType(fileMetadata.getFileType());
        dto.setFileSize(fileMetadata.getFileSize());
        dto.setFolderId(fileMetadata.getFolder() != null ? fileMetadata.getFolder().getId() : null);
        dto.setUploadedAt(fileMetadata.getUploadedAt());
        return dto;
    }
}
