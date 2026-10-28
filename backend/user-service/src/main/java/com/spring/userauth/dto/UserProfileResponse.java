package com.spring.userauth.dto;

import java.util.Date;
import java.util.List;

public record UserProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String name,
        String email,
        String gender,
        String mobile,
        List<String> roles,
        Date createdDate
) {
}
