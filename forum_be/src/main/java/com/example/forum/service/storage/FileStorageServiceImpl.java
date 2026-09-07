package com.example.forum.service.storage;

import org.springframework.web.multipart.MultipartFile;
import com.example.forum.dto.FileRequestDto;

public interface FileStorageServiceImpl {
    FileRequestDto storeFile(MultipartFile file);
    void deleteFile(String storedName);
}
