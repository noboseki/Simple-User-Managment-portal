package com.noboseki.supportportal.service.impl;

import com.noboseki.supportportal.domain.User;
import com.noboseki.supportportal.domain.UserPrincipal;
import com.noboseki.supportportal.enumeration.Role;
import com.noboseki.supportportal.exception.domain.EmailExistException;
import com.noboseki.supportportal.exception.domain.EmailSendException;
import com.noboseki.supportportal.exception.domain.UserNotFoundException;
import com.noboseki.supportportal.exception.domain.UsernameExistException;
import com.noboseki.supportportal.repository.UserRepository;
import com.noboseki.supportportal.service.EmailService;
import com.noboseki.supportportal.service.LoginAttemptService;
import com.noboseki.supportportal.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.noboseki.supportportal.service.impl.UserServiceImplConstant.*;

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
        String encodedPassword = encodePassword(password);
        User user = User.builder()
                .userId(generateUserId())
                .firstName(firstName)
                .lastName(lastName)
                .username(username)
                .email(email)
                .joinDate(new Date())
                .password(encodedPassword)
                .isActive(true)
                .isNotLocked(true)
                .role(Role.ROLE_USER.name())
                .authorities(Role.ROLE_USER.getAuthorities())
                .profileImageUrl(getTemporaryProfileImg()).build();

        userRepository.save(user);
        log.info("New user password: " + password);
        emailService.activationEmileSender(password, email);
        return user;
    }

    private String getTemporaryProfileImg() {
        return ServletUriComponentsBuilder
                .fromCurrentContextPath().path(DEFAULT_USER_IMAGE_PATH).toUriString();
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
