package com.enterprise.aiassistant.backend.storage.controller;

import com.enterprise.aiassistant.backend.storage.dto.response.StoredFileDto;
import com.enterprise.aiassistant.backend.storage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("${api.prefix}/test/storage")
@RequiredArgsConstructor
public class FileTestController {


    private final FileStorageService fileStorageService;


    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file
    ) {

        StoredFileDto result =
                fileStorageService.store(file);


        return ResponseEntity.ok(result);
    }
}