package com.clothing.service;

import com.clothing.dto.request.PosDraftUpsertRequest;
import com.clothing.dto.response.PosDraftResponse;

public interface PosDraftService {

    PosDraftResponse saveDraft(String adminUsername, PosDraftUpsertRequest request);

    PosDraftResponse getDraft(String adminUsername, String terminalId);

    void deleteDraft(String adminUsername, String terminalId);
}

