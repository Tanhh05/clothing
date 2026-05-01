package com.clothing.service.impl;

import com.clothing.config.R2Properties;
import com.clothing.dto.request.UploadPresignRequest;
import com.clothing.dto.response.UploadPresignResponse;
import com.clothing.exception.BusinessException;
import com.clothing.service.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class UploadServiceImpl implements UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadServiceImpl.class);
    private static final DateTimeFormatter PATH_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final R2Properties r2Properties;

    public UploadServiceImpl(S3Presigner s3Presigner, S3Client s3Client, R2Properties r2Properties) {
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
        this.r2Properties = r2Properties;
    }

    @Override
    public UploadPresignResponse createPresignedUploadUrl(UploadPresignRequest request) {
        validateR2Configuration();
        validateRequest(request);

        String cleanFolder = sanitizePathSegment(request.getFolder());
        String extension = extractExtension(request.getFileName());
        String objectKey = buildObjectKey(cleanFolder, extension);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(r2Properties.getBucket())
                .key(objectKey)
                .contentType(request.getContentType().toLowerCase(Locale.ROOT))
                .cacheControl(r2Properties.getImageCacheControl())
                .build();

        long expiresInSeconds = r2Properties.getPresignExpirationMinutes() * 60;
        PutObjectPresignRequest putObjectPresignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(putObjectRequest)
                .signatureDuration(Duration.ofMinutes(r2Properties.getPresignExpirationMinutes()))
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(putObjectPresignRequest);

        return UploadPresignResponse.builder()
                .objectKey(objectKey)
                .uploadUrl(presignedRequest.url().toString())
                .fileUrl(buildPublicFileUrl(objectKey))
                .method("PUT")
                .expiresInSeconds(expiresInSeconds)
                .requiredHeaders(Map.of(
                        "Content-Type", request.getContentType().toLowerCase(Locale.ROOT),
                        "Cache-Control", r2Properties.getImageCacheControl()
                ))
                .build();
    }

    @Override
    public List<String> uploadProductFiles(List<MultipartFile> files) {
        return uploadFiles(files, "products");
    }

    @Override
    public List<String> uploadReviewFiles(List<MultipartFile> files) {
        return uploadFiles(files, "reviews");
    }

    @Override
    public List<String> uploadPublicFiles(List<MultipartFile> files, String folder) {
        String cleanFolder = sanitizePathSegment(folder);
        return uploadFiles(files, cleanFolder);
    }

    private List<String> uploadFiles(List<MultipartFile> files, String folder) {
        validateR2Configuration();
        if (files == null || files.isEmpty()) {
            throw new BusinessException("At least one file is required", HttpStatus.BAD_REQUEST);
        }

        List<String> fileUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            validateFile(file);
            String objectKey = buildObjectKey(folder, extractExtension(file.getOriginalFilename()));
            String contentType = normalizeContentType(file.getContentType());

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(r2Properties.getBucket())
                    .key(objectKey)
                    .contentType(contentType)
                    .cacheControl(r2Properties.getImageCacheControl())
                    .build();
            try {
                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            } catch (IOException ex) {
                throw new BusinessException("Failed to read uploaded file", HttpStatus.BAD_REQUEST);
            } catch (S3Exception ex) {
                String errorCode = ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorCode() : "S3_ERROR";
                String errorMessage = ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorMessage() : ex.getMessage();
                log.error("R2 upload failed: bucket={}, key={}, status={}, code={}, message={}",
                        r2Properties.getBucket(), objectKey, ex.statusCode(), errorCode, errorMessage, ex);
                throw new BusinessException(
                        "Failed to upload file to cloud storage: " + errorCode + " - " + errorMessage,
                        HttpStatus.BAD_GATEWAY
                );
            } catch (SdkClientException ex) {
                log.error("R2 upload client error: bucket={}, key={}, message={}",
                        r2Properties.getBucket(), objectKey, ex.getMessage(), ex);
                throw new BusinessException(
                        "Failed to upload file to cloud storage: CLIENT_ERROR - " + ex.getMessage(),
                        HttpStatus.BAD_GATEWAY
                );
            } catch (Exception ex) {
                log.error("R2 upload unexpected error: bucket={}, key={}, message={}",
                        r2Properties.getBucket(), objectKey, ex.getMessage(), ex);
                throw new BusinessException("Failed to upload file to cloud storage", HttpStatus.BAD_GATEWAY);
            }

            fileUrls.add(buildPublicFileUrl(objectKey));
        }

        if (fileUrls.isEmpty()) {
            throw new BusinessException("No valid files to upload", HttpStatus.BAD_REQUEST);
        }
        return fileUrls;
    }

    private void validateRequest(UploadPresignRequest request) {
        if (request.getFileSize() > r2Properties.getMaxFileSizeBytes()) {
            throw new BusinessException("File exceeds max allowed size", HttpStatus.BAD_REQUEST);
        }

        String normalizedType = normalizeContentType(request.getContentType());
        if (!r2Properties.allowedContentTypeList().contains(normalizedType)) {
            throw new BusinessException("Content type is not allowed", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > r2Properties.getMaxFileSizeBytes()) {
            throw new BusinessException("File exceeds max allowed size: " + file.getOriginalFilename(), HttpStatus.BAD_REQUEST);
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!r2Properties.allowedContentTypeList().contains(contentType)) {
            throw new BusinessException("Content type is not allowed: " + contentType, HttpStatus.BAD_REQUEST);
        }
    }

    private String buildObjectKey(String folder, String extension) {
        String datePath = LocalDate.now().format(PATH_DATE_FORMAT);
        String fileName = UUID.randomUUID().toString().replace("-", "");
        String basePath = folder == null || folder.isBlank() ? "uploads" : folder;
        return basePath + "/" + datePath + "/" + fileName + extension;
    }

    private String buildPublicFileUrl(String objectKey) {
        String base = r2Properties.getPublicBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + objectKey;
    }

    private String extractExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return ext.isBlank() ? "" : "." + ext;
    }

    private String sanitizePathSegment(String folder) {
        if (folder == null || folder.isBlank()) {
            return "uploads";
        }
        return folder.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9/_-]", "")
                .replaceAll("/{2,}", "/")
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "";
        }
        return contentType.toLowerCase(Locale.ROOT);
    }

    private void validateR2Configuration() {
        if (isPlaceholder(r2Properties.getAccountId())
                || isPlaceholder(r2Properties.getAccessKey())
                || isPlaceholder(r2Properties.getSecretKey())
                || isPlaceholder(r2Properties.getBucket())
                || isPlaceholder(r2Properties.getEndpoint())) {
            throw new BusinessException(
                    "R2 chưa cấu hình đúng. Cập nhật R2_ACCOUNT_ID, R2_ACCESS_KEY, R2_SECRET_KEY, R2_ENDPOINT trong backend/.env.local rồi restart backend.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private boolean isPlaceholder(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("replace-with")
                || normalized.contains("your-account-id")
                || normalized.contains("your-access-key")
                || normalized.contains("your-secret-key")
                || normalized.contains("cdn.example.com");
    }
}
