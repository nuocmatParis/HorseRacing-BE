package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.veterinarian.request.VeterinarianCreationRequest;
import com.swp391.horseracing.dto.veterinarian.request.VeterinarianUpdateRequest;
import com.swp391.horseracing.dto.veterinarian.response.VeterinarianResponse;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.entity.Veterinarian;
import com.swp391.horseracing.enums.VetStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.VeterinarianMapper;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.repository.VeterinarianRepository;
import com.swp391.horseracing.service.VeterinarianService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class VeterinarianServiceImpl implements VeterinarianService {

    VeterinarianRepository veterinarianRepository;
    UserRepository userRepository;
    VeterinarianMapper veterinarianMapper;

    @Override
    @Transactional
    public VeterinarianResponse create(VeterinarianCreationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (veterinarianRepository.existsByUser_UserId(user.getUserId())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }

        Veterinarian veterinarian = veterinarianMapper.toVeterinarian(request);
        veterinarian.setUser(user);
        veterinarian.setStatus(VetStatus.AVAILABLE);
        veterinarian.setCreatedAt(LocalDateTime.now());

        return veterinarianMapper.toVeterinarianResponse(veterinarianRepository.save(veterinarian));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VeterinarianResponse> getAll() {
        return veterinarianRepository.findAll()
                .stream()
                .map(veterinarianMapper::toVeterinarianResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VeterinarianResponse getById(UUID id) {
        Veterinarian veterinarian = veterinarianRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VETERINARIAN_PROFILE_NOT_FOUND));
        return veterinarianMapper.toVeterinarianResponse(veterinarian);
    }

    @Override
    @Transactional
    public VeterinarianResponse update(UUID id, VeterinarianUpdateRequest request) {
        Veterinarian veterinarian = veterinarianRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VETERINARIAN_PROFILE_NOT_FOUND));

        veterinarianMapper.updateVeterinarian(veterinarian, request);
        return veterinarianMapper.toVeterinarianResponse(veterinarianRepository.save(veterinarian));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Veterinarian veterinarian = veterinarianRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VETERINARIAN_PROFILE_NOT_FOUND));
        veterinarianRepository.delete(veterinarian);
    }
}
