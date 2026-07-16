package com.swp391.horseracing.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminContractEndpointTest {
    @Test
    void adminContractApiIsReadOnlyExceptForFinalPayout() {
        Set<String> getPaths = new HashSet<>();
        Set<String> postPaths = new HashSet<>();

        for (Method method : AdminController.class.getDeclaredMethods()) {
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            if (getMapping != null) {
                for (String path : getMapping.value()) {
                    getPaths.add(path);
                }
            }
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            if (postMapping != null) {
                for (String path : postMapping.value()) {
                    postPaths.add(path);
                }
            }
        }

        assertTrue(getPaths.contains("/contracts"));
        assertTrue(getPaths.contains("/contracts/approved/tournaments/{tournamentId}"));
        assertFalse(getPaths.contains("/contracts/pending"));
        assertFalse(postPaths.contains("/contracts/{id}/approve"));
        assertFalse(postPaths.contains("/contracts/{id}/reject"));
        assertTrue(postPaths.contains("/contracts/{id}/release-final-payout"));
    }
}
