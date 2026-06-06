package com.clothing.service;

public record PaymentNotificationResult(
        String responseCode,
        String message,
        boolean paymentSuccessful
) {
    public static PaymentNotificationResult success(boolean paymentSuccessful) {
        return new PaymentNotificationResult("00", "Confirm Success", paymentSuccessful);
    }

    public static PaymentNotificationResult alreadyProcessed(boolean paymentSuccessful) {
        return new PaymentNotificationResult("02", "Order already confirmed", paymentSuccessful);
    }

    public static PaymentNotificationResult notFound() {
        return new PaymentNotificationResult("01", "Order not found", false);
    }

    public static PaymentNotificationResult invalidAmount() {
        return new PaymentNotificationResult("04", "Invalid amount", false);
    }
}
