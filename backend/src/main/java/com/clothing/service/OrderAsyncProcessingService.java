package com.clothing.service;

public interface OrderAsyncProcessingService {

    void processOrderCreated(Long orderId);
}
