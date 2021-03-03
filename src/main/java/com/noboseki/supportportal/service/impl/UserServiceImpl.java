package com.noboseki.supportportal.service.impl;

import com.noboseki.supportportal.domain.User;
import com.noboseki.supportportal.domain.UserPrincipal;
import com.noboseki.supportportal.dtos.AddNewUserDto;
import com.noboseki.supportportal.dtos.UpdateUserDto;
import com.noboseki.supportportal.enumeration.Role;
import com.noboseki.supportportal.exception.domain.NotAnImageFileException;
import com.noboseki.supportportal.exception.domain.*;
import com.noboseki.supportportal.repository.UserRepository;
import com.noboseki.supportportal.service.EmailService;
import com.noboseki.supportportal.service.LoginAttemptService;
import com.noboseki.supportportal.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.noboseki.supportportal.constant.FileConstant.*;
import static com.noboseki.supportportal.constant.UserServiceImplConstant.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.util.MimeTypeUtils.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@Qualifier("userDetailsService")
public class UserServiceImpl implements UserService, UserDetailsService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final LoginAttemptService attemptService;
    private final EmailService emailService;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findUserByUsername(username).orElseThrow(() -> {
            log.error(NOT_FOUND_BY_USERNAME + username);
            return new UsernameNotFoundException(NOT_FOUND_BY_USERNAME + username);
        });

        validateLoginAttempt(user);
        user.setLastLoginDateDisplay(user.getLastLoginDate());
        user.setLastLoginDate(new Date());
        userRepository.save(user);

        UserPrincipal userPrincipal = new UserPrincipal(user);
        log.info(USER_WAS_FOUND + username);
        return userPrincipal;
    }

    private void validateLoginAttempt(User user) {
        if (user.isNotLocked()) {
            if (attemptService.hasExceededMaxAttempts(user.getUsername())) {
                user.setNotLocked(false);
            } else {
                user.setNotLocked(true);
            }
        } else {
            attemptService.evictUserFromLoginAttemptCache(user.getUsername());
        }
    }

    @Override
    @Transactional
    public User register(String firstName, String lastName, String username, String email)
            throws UserNotFoundException, UsernameExistException, EmailExistException, EmailSendException {
        validateNewUsernameAndEmail(StringUtils.EMPTY, username, email);
        String password = generatePassword();
        User user = User.builder()
                .userId(generateUserId())
                .firstName(firstName)
                .lastName(lastName)
                .username(username)
                .email(email)
                .joinDate(new Date())
                .password(encodePassword(password))
                .isActive(true)
                .isNotLocked(true)
                .role(Role.ROLE_USER.name())
                .authorities(Role.ROLE_USER.getAuthorities())
                .profileImageUrl(getTemporaryProfileImg(username)).build();

        userRepository.save(user);
        log.info("New user password: " + password);
        emailService.sendNewPasswordEmail(password, email);
        return user;
    }

    @Override
    public User addNewUser(AddNewUserDto dto)
            throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
        validateNewUsernameAndEmail(EMPTY, dto.getUsername(), dto.getEmail());

        String password = generatePassword();
        User user = addNewUserGetUserObject(dto);
        user.setPassword(encodePassword(password));
        userRepository.save(user);

        saveProfileImage(user, dto.getProfileImage());
        return user;
    }

    @Override
    public User updateUser(UpdateUserDto dto)
            throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
        User currentUser = updateUserGetUpdateUser(dto);
        userRepository.save(currentUser);

        saveProfileImage(currentUser, dto.getProfileImage());
        return currentUser;
    }

    @Override
    public void deleteUser(String username) throws IOException {
        User user = findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Path userFolder = Paths.get(USER_FOLDER + user.getUsername()).toAbsolutePath().normalize();
        FileUtils.deleteDirectory(new File(userFolder.toString()));
        userRepository.deleteById(user.getId());
    }

    @Override
    public void resetPassword(String email) throws EmailNotFoundException, EmailSendException {
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new EmailNotFoundException(NO_USER_FOUND_BY_EMAIL + email));
        String password = generatePassword();
        user.setPassword(encodePassword(password));
        userRepository.save(user);
        emailService.sendNewPasswordEmail(password, user.getEmail());
    }

    @Override
    public User updateProfileImage(String username, MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
        User user = validateNewUsernameAndEmail(username, null, null);
        saveProfileImage(user, profileImage);
        return user;
    }

    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findUserByUsername(username);
    }

    @Override
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findUserByEmail(email);
    }

    private User addNewUserGetUserObject(AddNewUserDto dto) {
        return User.builder()
                .userId(generateUserId())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .username(dto.getUsername())
                .email(dto.getEmail())
                .joinDate(new Date())
                .isActive(true)
                .isNotLocked(true)
                .role(getRoleEnumName(dto.getRole()).name())
                .authorities(getRoleEnumName(dto.getRole()).getAuthorities())
                .profileImageUrl(getTemporaryProfileImg(dto.getUsername())).build();
    }

    private User updateUserGetUpdateUser(UpdateUserDto dto) throws UserNotFoundException, UsernameExistException, EmailExistException {
        User currentUser = validateNewUsernameAndEmail(dto.getCurrentUsername(), dto.getNewUsername(), dto.getNewEmail());
        currentUser.setFirstName(dto.getNewFirstName());
        currentUser.setLastName(dto.getNewLastName());
        currentUser.setUsername(dto.getNewUsername());
        currentUser.setEmail(dto.getNewEmail());
        currentUser.setActive(dto.isActive());
        currentUser.setNotLocked(dto.isNonLocked());
        currentUser.setAuthorities(getRoleEnumName(dto.getRole()).getAuthorities());
        currentUser.setRole(getRoleEnumName(dto.getRole()).name());

        return currentUser;
    }

    private void saveProfileImage(User user, MultipartFile profileImage) throws IOException, NotAnImageFileException {
        if (profileImage != null) {
            if (!Arrays.asList(IMAGE_JPEG_VALUE, IMAGE_PNG_VALUE, IMAGE_GIF_VALUE).contains(profileImage.getContentType())){
                throw new NotAnImageFileException(profileImage.getOriginalFilename() + "is not an image file");
            }
            Path userFolder = Paths.get(USER_FOLDER + user.getUsername()).toAbsolutePath().normalize();
            if (!Files.exists(userFolder)) {
                Files.createDirectories(userFolder);
                log.info(DIRECTORY_CREATED + userFolder);
            }
            Files.deleteIfExists(Paths.get(userFolder + user.getUsername() + DOT + JPG_EXTENSION));
            Files.copy(profileImage.getInputStream(), userFolder.resolve(user.getUsername() + DOT + JPG_EXTENSION), REPLACE_EXISTING);
            user.setProfileImageUrl(setProfileImageUrl(user.getUsername()));
            userRepository.save(user);
            log.info(FILE_SAVED_IN_FILE_SYSTEM + profileImage.getOriginalFilename());
        }
    }

    private String setProfileImageUrl(String username) {
        return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path(USER_IMAGE_PATH + username + FORWARD_SLASH + username + DOT + JPG_EXTENSION)
                .toUriString();
    }

    private Role getRoleEnumName(String role) {
        return Role.valueOf(role.toUpperCase());
    }

    private String getTemporaryProfileImg(String username) {
        return ServletUriComponentsBuilder
                .fromCurrentContextPath().path(DEFAULT_USER_IMAGE_PATH + username).toUriString();
    }

    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    private String generatePassword() {
        return RandomStringUtils.randomAlphabetic(10);
    }

    private String generateUserId() {
        return RandomStringUtils.randomNumeric(10);
    }

    private User validateNewUsernameAndEmail(String currentUsername, String newUsername, String newEmail)
            throws UserNotFoundException, UsernameExistException, EmailExistException {
        Optional<User> userByNewUsername = findByUsername(newUsername);
        Optional<User> userByNewEmail = findUserByEmail(newEmail);

        if (StringUtils.isNotBlank(currentUsername)) {
            User currentUser = findByUsername(currentUsername)
                    .orElseThrow(() -> new UserNotFoundException(NOT_FOUND_BY_USERNAME + currentUsername));

            if (userByNewUsername.isPresent() && !currentUser.getId().equals(userByNewUsername.get().getId())) {
                throw new UsernameExistException(USER_ALREADY_EXIST);
            }
            if (userByNewEmail.isPresent() && !currentUser.getId().equals(userByNewEmail.get().getId())) {
                throw new EmailExistException(EMAIL_ALREADY_EXIST);
            }
            return currentUser;

        } else {
            if (userByNewUsername.isPresent()) {
                throw new UsernameExistException(USER_ALREADY_EXIST);
            }
            if (userByNewEmail.isPresent()) {
                throw new EmailExistException(EMAIL_ALREADY_EXIST);
            }
            return null;
        }
    }
}
