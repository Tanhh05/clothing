package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResponseMeta {

    private String language;
    private String currency;
}
