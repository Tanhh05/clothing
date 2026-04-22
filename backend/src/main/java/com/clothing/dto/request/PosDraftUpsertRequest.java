package com.clothing.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PosDraftUpsertRequest {

    @NotBlank(message = "terminalId is required")
    @Size(max = 100, message = "terminalId max length is 100")
    private String terminalId;

    @NotNull(message = "payload is required")
    private JsonNode payload;
}

