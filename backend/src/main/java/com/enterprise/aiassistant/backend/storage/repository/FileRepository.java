package com.enterprise.aiassistant.backend.storage.repository;

import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

}
