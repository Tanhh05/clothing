package com.clothing.service;

import com.clothing.dto.request.CreateReturnRequest;
import com.clothing.dto.response.ReturnRequestResponse;

import java.util.List;

public interface ReturnRequestService {

    List<ReturnRequestResponse> getAll(String status);

    List<ReturnRequestResponse> getMyRequests(String username, String status);

    ReturnRequestResponse create(String username, CreateReturnRequest request);

    ReturnRequestResponse updateStatus(Long id, String status, String note);
}
