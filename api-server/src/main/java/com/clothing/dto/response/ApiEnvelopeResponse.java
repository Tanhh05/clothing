package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiEnvelopeResponse<T> {

    private ResponseMeta meta;
    private T data;
}
