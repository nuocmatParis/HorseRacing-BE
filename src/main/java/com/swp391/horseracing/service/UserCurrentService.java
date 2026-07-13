package com.swp391.horseracing.service;

import com.swp391.horseracing.entity.HorseOwner;
import com.swp391.horseracing.entity.User;

public interface UserCurrentService {
    User getCurrentUser();

    HorseOwner getCurrentOwner();
}
