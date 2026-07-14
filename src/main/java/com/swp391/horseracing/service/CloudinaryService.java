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
        // Cấu hình tùy chọn: đẩy vào thư mục cụ thể trên Cloudinary
        Map options = ObjectUtils.asMap(
                "folder", folderName
        );

        // Thực hiện upload
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), options);

        // Trả về URL an toàn (https)
        return uploadResult.get("secure_url").toString();
    }
}
