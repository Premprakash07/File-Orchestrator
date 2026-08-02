package com.filestorage.controller;

import com.filestorage.dto.FileDTO;
import com.filestorage.dto.PresignedUrlResponse;
import com.filestorage.dto.UploadConfirmationRequest;
import com.filestorage.model.User;
import com.filestorage.repository.UserRepository;
import com.filestorage.service.FileStorageService;
import com.filestorage.service.S3Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FileController {

    private static final Logger logger = LogManager.getLogger(FileController.class);

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private S3Service s3Service;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) Long folderId,
            @RequestParam(value = "aiSummarize", required = false, defaultValue = "false") Boolean aiSummarize,
            @RequestParam(value = "extractMetadata", required = false, defaultValue = "false") Boolean extractMetadata,
            @RequestParam(value = "generateThumbnail", required = false, defaultValue = "false") Boolean generateThumbnail,
            @RequestParam(value = "tags", required = false) String tags,
            Authentication authentication) {
        logger.info(
                "File upload requested: {} (size: {} bytes) by user: {} with options - AI Summarize: {}, Extract Metadata: {}, Generate Thumbnail: {}, Tags: {}",
                file.getOriginalFilename(), file.getSize(), authentication.getName(),
                aiSummarize, extractMetadata, generateThumbnail, tags);
        try {
            User user = getUserFromAuthentication(authentication);
            // TODO: Implement AI summarization logic when aiSummarize is true
            // TODO: Implement metadata extraction logic when extractMetadata is true
            // TODO: Implement thumbnail generation logic when generateThumbnail is true
            // TODO: Process and store tags
            FileDTO fileDTO = fileStorageService.storeFile(file, user, folderId);
            logger.info("File uploaded successfully: {} with ID: {}", file.getOriginalFilename(), fileDTO.getId());
            return ResponseEntity.ok(fileDTO);
        } catch (IOException e) {
            logger.error("Failed to upload file: {}. Error: {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to upload file: " + e.getMessage());
        }
    }

    @PostMapping("/presigned-url")
    public ResponseEntity<?> getPresignedUrl(
            @RequestParam("fileName") String fileName,
            @RequestParam("contentType") String contentType,
            Authentication authentication) {
        logger.info("Presigned URL requested for file: {} by user: {}", fileName, authentication.getName());
        try {
            User user = getUserFromAuthentication(authentication);
            PresignedUrlResponse response = s3Service.generatePresignedUploadUrl(fileName, contentType, user.getId());
            logger.info("Presigned URL generated successfully for file: {}", fileName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to generate presigned URL for file: {}. Error: {}", fileName, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to generate presigned URL: " + e.getMessage());
        }
    }

    @PostMapping("/confirm-upload")
    public ResponseEntity<?> confirmUpload(
            @RequestBody UploadConfirmationRequest request,
            Authentication authentication) {
        logger.info("Upload confirmation requested for S3 key: {} by user: {}", request.getS3Key(),
                authentication.getName());
        try {
            User user = getUserFromAuthentication(authentication);
            FileDTO fileDTO = fileStorageService.saveS3FileMetadata(request, user);
            logger.info("Upload confirmed successfully for file: {} with ID: {}", request.getFileName(),
                    fileDTO.getId());
            return ResponseEntity.ok(fileDTO);
        } catch (Exception e) {
            logger.error("Failed to confirm upload for S3 key: {}. Error: {}", request.getS3Key(), e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to confirm upload: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<FileDTO>> getFiles(@RequestParam(value = "folderId", required = false) Long folderId,
            Authentication authentication) {
        logger.debug("Fetching files for user: {} in folder: {}", authentication.getName(), folderId);
        User user = getUserFromAuthentication(authentication);
        List<FileDTO> files = fileStorageService.getUserFiles(user, folderId);
        logger.debug("Retrieved {} files for user: {}", files.size(), authentication.getName());
        return ResponseEntity.ok(files);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, Authentication authentication) {
        logger.info("File download requested: ID {} by user: {}", id, authentication.getName());
        try {
            User user = getUserFromAuthentication(authentication);
            Resource resource = fileStorageService.loadFileAsResource(id, user);
            logger.info("File downloaded successfully: {}", resource.getFilename());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (RuntimeException e) {
            logger.error("File not found or access denied - ID: {}. Error: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to download file ID: {}. Error: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable Long id, Authentication authentication) {
        logger.info("File deletion requested: ID {} by user: {}", id, authentication.getName());
        try {
            User user = getUserFromAuthentication(authentication);
            fileStorageService.deleteFile(id, user);
            logger.info("File deleted successfully: ID {}", id);
            return ResponseEntity.ok().body("File deleted successfully");
        } catch (IOException e) {
            logger.error("Failed to delete file ID: {}. Error: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to delete file: " + e.getMessage());
        }
    }

    private User getUserFromAuthentication(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
