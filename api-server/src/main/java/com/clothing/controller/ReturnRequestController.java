package com.clothing.controller;

import com.clothing.dto.request.CreateReturnRequest;
import com.clothing.dto.response.ReturnRequestResponse;
import com.clothing.service.ReturnRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/returns")
public class ReturnRequestController {

    private final ReturnRequestService returnRequestService;

    public ReturnRequestController(ReturnRequestService returnRequestService) {
        this.returnRequestService = returnRequestService;
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReturnRequestResponse>> getMyRequests(
            Authentication authentication,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(returnRequestService.getMyRequests(authentication.getName(), status));
    }

    @PostMapping
    public ResponseEntity<ReturnRequestResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateReturnRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(returnRequestService.create(authentication.getName(), request));
    }
}
