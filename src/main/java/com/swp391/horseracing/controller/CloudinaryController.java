package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.service.CloudinaryService;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/images")
public class CloudinaryController {

    CloudinaryService cloudinaryService;

    @PostMapping("/upload")
    public ApiResponse<String> uploadImage(@RequestParam("file") @NotNull MultipartFile file) {
        try {
            String imageUrl = cloudinaryService.uploadImage(file, "spring_boot_uploads");
            return ApiResponse.<String>builder()
                    .result(imageUrl)
                    .build();
        } catch (IOException e) {
            return ApiResponse.<String>builder()
                    .code(500)
                    .message("Upload failed: " + e.getMessage())
                    .build();
        }
    }
}
