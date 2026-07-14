package com.swp391.horseracing.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloudinaryService {

    Cloudinary cloudinary;

    public String uploadImage(MultipartFile file, String folderName) throws IOException {
        return uploadFile(file, folderName, "image");
    }

    public String uploadFile(MultipartFile file, String folderName, String resourceType) throws IOException {
        Map options = ObjectUtils.asMap(
                "folder", folderName,
                "resource_type", resourceType
        );
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
        return uploadResult.get("secure_url").toString();
    }
}
