package com.filestorage.repository;

import com.filestorage.model.Folder;
import com.filestorage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findByUserAndParentFolderIsNull(User user);

    List<Folder> findByUserAndParentFolder(User user, Folder parentFolder);

    Optional<Folder> findByIdAndUser(Long id, User user);

    List<Folder> findByUser(User user);
}
