package com.clothing.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BannerUpsertRequest {

    @Size(max = 255, message = "title max length is 255")
    private String title;

    @Size(max = 1000, message = "imageUrl max length is 1000")
    private String imageUrl;

    @Size(max = 1000, message = "linkUrl max length is 1000")
    private String linkUrl;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Size(max = 20, message = "status max length is 20")
    private String status;
}
