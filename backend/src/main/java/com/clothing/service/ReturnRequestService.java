package com.clothing.service;

import com.clothing.dto.response.ReturnRequestResponse;

import java.util.List;

public interface ReturnRequestService {

    List<ReturnRequestResponse> getAll(String status);

    ReturnRequestResponse updateStatus(Long id, String status);
}
