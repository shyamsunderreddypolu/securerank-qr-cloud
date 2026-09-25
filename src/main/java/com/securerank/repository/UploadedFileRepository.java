package com.securerank.repository;

import com.securerank.entity.UploadedFile;
import com.securerank.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    List<UploadedFile> findByOwnerOrderByUploadedAtDesc(User owner);

    List<UploadedFile> findAllByOrderByUploadedAtDesc();
}
