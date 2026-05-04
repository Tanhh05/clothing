package com.clothing.service;

import com.clothing.dto.request.VoucherUpsertRequest;
import com.clothing.dto.response.VoucherBestResponse;
import com.clothing.dto.response.VoucherResponse;

import java.util.List;

public interface VoucherService {

    List<VoucherResponse> getAll();

    VoucherResponse create(VoucherUpsertRequest request);

    VoucherResponse update(Long id, VoucherUpsertRequest request);

    void delete(Long id);

    List<VoucherResponse> getPublicActive();

    VoucherBestResponse suggestBest(Long subTotal);
}
