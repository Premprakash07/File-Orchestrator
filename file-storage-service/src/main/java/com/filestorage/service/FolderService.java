package com.filestorage.service;

import com.filestorage.dto.FolderDTO;
import com.filestorage.model.FileMetadata;
import com.filestorage.model.Folder;
import com.filestorage.model.User;
import com.filestorage.repository.FolderRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FolderService {

    private static final Logger logger = LogManager.getLogger(FolderService.class);

    @Autowired
    private FolderRepository folderRepository;

    public FolderDTO createFolder(String name, User user, Long parentFolderId) {
        logger.info("Creating folder '{}' for user ID: {} with parent folder ID: {}", name, user.getId(),
                parentFolderId);

        Folder folder = new Folder();
        folder.setName(name);
        folder.setUser(user);

        if (parentFolderId != null) {
            Folder parentFolder = folderRepository.findByIdAndUser(parentFolderId, user)
                    .orElseThrow(() -> {
                        logger.warn("Parent folder not found: ID {} for user ID: {}", parentFolderId, user.getId());
                        return new RuntimeException("Parent folder not found");
                    });
            folder.setParentFolder(parentFolder);
        }

        folder = folderRepository.save(folder);
        logger.info("Folder created successfully with ID: {}", folder.getId());
        return convertToDTO(folder);
    }

    public List<FolderDTO> getUserFolders(User user, Long parentFolderId) {
        logger.debug("Fetching folders for user ID: {} with parent folder ID: {}", user.getId(), parentFolderId);

        List<Folder> folders;
        if (parentFolderId == null) {
            folders = folderRepository.findByUserAndParentFolderIsNull(user);
        } else {
            Folder parentFolder = folderRepository.findByIdAndUser(parentFolderId, user)
                    .orElseThrow(() -> new RuntimeException("Parent folder not found"));
            folders = folderRepository.findByUserAndParentFolder(user, parentFolder);
        }

        logger.debug("Found {} folders for user ID: {}", folders.size(), user.getId());
        return folders.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public void deleteFolder(Long folderId, User user) {
        logger.info("Deleting folder: ID {} for user ID: {}", folderId, user.getId());

        Folder folder = folderRepository.findByIdAndUser(folderId, user)
                .orElseThrow(() -> {
                    logger.warn("Folder not found for deletion: ID {} for user ID: {}", folderId, user.getId());
                    return new RuntimeException("Folder not found");
                });

        folderRepository.delete(folder);
        logger.info("Folder deleted successfully: ID {}", folderId);
    }

    public FolderDTO updateFolder(Long folderId, String newName, User user) {
        logger.info("Updating folder: ID {} to new name '{}' for user ID: {}", folderId, newName, user.getId());

        Folder folder = folderRepository.findByIdAndUser(folderId, user)
                .orElseThrow(() -> {
                    logger.warn("Folder not found for update: ID {} for user ID: {}", folderId, user.getId());
                    return new RuntimeException("Folder not found");
                });

        folder.setName(newName);
        folder = folderRepository.save(folder);
        logger.info("Folder updated successfully: ID {}", folderId);
        return convertToDTO(folder);
    }

    public Resource downloadFolderAsZip(Long folderId, User user) throws IOException {
        logger.info("Downloading folder as ZIP: ID {} for user ID: {}", folderId, user.getId());

        Folder folder = folderRepository.findByIdAndUser(folderId, user)
                .orElseThrow(() -> {
                    logger.warn("Folder not found for download: ID {} for user ID: {}", folderId, user.getId());
                    return new RuntimeException("Folder not found");
                });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            addFolderToZip(folder, "", zos);
            logger.info("ZIP created successfully for folder: {} (ID: {})", folder.getName(), folderId);
        }

        return new ByteArrayResource(baos.toByteArray());
    }

    private void addFolderToZip(Folder folder, String parentPath, ZipOutputStream zos) throws IOException {
        String currentPath = parentPath.isEmpty() ? folder.getName() : parentPath + "/" + folder.getName();
        logger.debug("Adding folder to ZIP: {}", currentPath);

        // Add files in current folder
        for (FileMetadata file : folder.getFiles()) {
            Path filePath = Paths.get(file.getFilePath());
            if (Files.exists(filePath)) {
                String zipEntryName = currentPath + "/" + file.getFileName();
                logger.debug("Adding file to ZIP: {}", zipEntryName);

                ZipEntry zipEntry = new ZipEntry(zipEntryName);
                zos.putNextEntry(zipEntry);
                Files.copy(filePath, zos);
                zos.closeEntry();
            } else {
                logger.warn("File not found on disk, skipping: {}", filePath);
            }
        }

        // Recursively add subfolders
        for (Folder subFolder : folder.getSubFolders()) {
            addFolderToZip(subFolder, currentPath, zos);
        }
    }

    private FolderDTO convertToDTO(Folder folder) {
        FolderDTO dto = new FolderDTO();
        dto.setId(folder.getId());
        dto.setName(folder.getName());
        dto.setParentFolderId(folder.getParentFolder() != null ? folder.getParentFolder().getId() : null);
        dto.setCreatedAt(folder.getCreatedAt());
        return dto;
    }
}
