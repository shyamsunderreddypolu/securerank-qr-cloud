package com.securerank.repository;

import com.securerank.entity.FileKey;
import com.securerank.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileKeyRepository extends JpaRepository<FileKey, Long> {

    Optional<FileKey> findByFile(UploadedFile file);

    Optional<FileKey> findByFileId(Long fileId);
}
