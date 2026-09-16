package com.example.forum.dto;

import org.springframework.core.io.Resource;

public record FileDownloadDto(Resource resource, String originalName, String contentType) {}