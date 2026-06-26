package com.swp391.horseracing.service;

public interface EmailService {
    void sendOtp(String toEmail, String otpCode);
}
