package com.filestorage.controller;

import com.filestorage.dto.FolderDTO;
import com.filestorage.model.User;
import com.filestorage.repository.UserRepository;
import com.filestorage.service.FolderService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/folders")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FolderController {

    private static final Logger logger = LogManager.getLogger(FolderController.class);

    @Autowired
    private FolderService folderService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<FolderDTO> createFolder(@RequestBody Map<String, Object> request,
            Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        String name = (String) request.get("name");
        Long parentFolderId = request.get("parentFolderId") != null
                ? Long.parseLong(request.get("parentFolderId").toString())
                : null;

        logger.info("Creating folder '{}' for user: {} in parent folder: {}", name, authentication.getName(),
                parentFolderId);
        FolderDTO folder = folderService.createFolder(name, user, parentFolderId);
        logger.info("Folder created successfully with ID: {}", folder.getId());
        return ResponseEntity.ok(folder);
    }

    @GetMapping
    public ResponseEntity<List<FolderDTO>> getFolders(
            @RequestParam(value = "parentFolderId", required = false) Long parentFolderId,
            Authentication authentication) {
        logger.debug("Fetching folders for user: {} in parent folder: {}", authentication.getName(), parentFolderId);
        User user = getUserFromAuthentication(authentication);
        List<FolderDTO> folders = folderService.getUserFolders(user, parentFolderId);
        logger.debug("Retrieved {} folders for user: {}", folders.size(), authentication.getName());
        return ResponseEntity.ok(folders);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FolderDTO> updateFolder(@PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        String newName = request.get("name");
        logger.info("Updating folder ID: {} to new name: '{}' by user: {}", id, newName, authentication.getName());
        FolderDTO folder = folderService.updateFolder(id, newName, user);
        logger.info("Folder updated successfully: ID {}", id);
        return ResponseEntity.ok(folder);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFolder(@PathVariable Long id, Authentication authentication) {
        logger.info("Deleting folder ID: {} by user: {}", id, authentication.getName());
        User user = getUserFromAuthentication(authentication);
        folderService.deleteFolder(id, user);
        logger.info("Folder deleted successfully: ID {}", id);
        return ResponseEntity.ok().body("Folder deleted successfully");
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFolder(@PathVariable Long id, Authentication authentication) {
        logger.info("Folder download requested: ID {} by user: {}", id, authentication.getName());
        try {
            User user = getUserFromAuthentication(authentication);
            Resource resource = folderService.downloadFolderAsZip(id, user);

            // Get folder name for the zip filename
            String filename = "folder_" + id + ".zip";
            logger.info("Folder downloaded successfully as: {}", filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Failed to download folder ID: {}. Error: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    private User getUserFromAuthentication(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
