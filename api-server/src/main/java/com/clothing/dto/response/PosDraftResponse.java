package com.clothing.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class PosDraftResponse {

    private String terminalId;
    private JsonNode payload;
    private LocalDateTime updatedAt;
}

