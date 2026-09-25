package com.securerank.repository;

import com.securerank.entity.KeyRequest;
import com.securerank.entity.RequestStatus;
import com.securerank.entity.UploadedFile;
import com.securerank.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KeyRequestRepository extends JpaRepository<KeyRequest, Long> {

    List<KeyRequest> findByConsumerOrderByRequestedAtDesc(User consumer);

    List<KeyRequest> findByStatusOrderByRequestedAtDesc(RequestStatus status);

    Optional<KeyRequest> findByFileAndConsumer(UploadedFile file, User consumer);

    boolean existsByFileAndConsumerAndStatus(UploadedFile file, User consumer, RequestStatus status);
}
