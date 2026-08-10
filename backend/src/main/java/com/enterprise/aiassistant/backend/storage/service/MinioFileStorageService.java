package com.enterprise.aiassistant.backend.storage.service;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.FileStorageException;
import com.enterprise.aiassistant.backend.storage.dto.response.StoredFileDto;
import com.enterprise.aiassistant.backend.storage.mapper.StoredFileMapper;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static com.enterprise.aiassistant.backend.common.exception.ErrorCode.FILE_STORAGE_READ_FAILED;

@Service
@RequiredArgsConstructor
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;

    private final StoredFileMapper storedFileMapper;

    @Value("${minio.bucket}")
    private String bucket;

    @Override
    public StoredFileDto store(MultipartFile file) {

        validateNotEmptyFile(file);

        try {
            String storedName = UUID.randomUUID() + "-" + file.getOriginalFilename();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(storedName)
                            .stream(
                                    file.getInputStream(),
                                    file.getSize(),
                                    -1
                            )
                            .contentType(file.getContentType())
                            .build()
            );

            return storedFileMapper.toDto(file, storedName, bucket);

        } catch (Exception e) {
            throw new FileStorageException(
                    ErrorCode.FILE_UPLOAD_FAILED,
                    "File upload failed",
                    e
            );
        }

    }

    @Override
    public Resource loadAsResource(String bucketName, String objectKey) {

        try {
            return new InputStreamResource(
                    minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(objectKey)
                                    .build()
                    )
            );
        } catch (Exception e) {
            throw new FileStorageException(
                    FILE_STORAGE_READ_FAILED,
                    e
            );
        }
    }

    private void validateNotEmptyFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }
    }
}
