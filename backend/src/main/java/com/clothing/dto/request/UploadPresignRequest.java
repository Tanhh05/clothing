package com.clothing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UploadPresignRequest {

    @NotBlank(message = "fileName is required")
    @Size(max = 255, message = "fileName max length is 255")
    private String fileName;

    @NotBlank(message = "contentType is required")
    @Size(max = 100, message = "contentType max length is 100")
    private String contentType;

    @NotNull(message = "fileSize is required")
    @Positive(message = "fileSize must be > 0")
    private Long fileSize;

    @Size(max = 100, message = "folder max length is 100")
    private String folder;
}
