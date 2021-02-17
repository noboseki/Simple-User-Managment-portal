package com.noboseki.supportportal.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Builder
@Getter
@Setter
@NoArgsConstructor
public class UpdateUserDto {
    private String currentUsername;
    private String newFirstName;
    private String newLastName;
    private String newUsername;
    private String newEmail;
    private String role;
    private boolean isNonLocked;
    private boolean isActive;
    private MultipartFile profileImage;
}
