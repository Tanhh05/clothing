package com.clothing.service;

import com.clothing.dto.request.UploadPresignRequest;
import com.clothing.dto.response.UploadPresignResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UploadService {

    UploadPresignResponse createPresignedUploadUrl(UploadPresignRequest request);

    List<String> uploadProductFiles(List<MultipartFile> files);

    List<String> uploadReviewFiles(List<MultipartFile> files);
}
