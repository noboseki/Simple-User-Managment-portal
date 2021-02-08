package com.noboseki.supportportal.service.impl;

import com.noboseki.supportportal.domain.User;
import com.noboseki.supportportal.domain.UserPrincipal;
import com.noboseki.supportportal.repository.UserRepository;
import com.noboseki.supportportal.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@Qualifier("UserDetailsService")
public class UserServiceImpl implements UserService, UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        final String USERNAME_NOT_FOUND = "User not found by username: " + username;
        final String USER_WAS_FOUND = "Returning found user by username :" + username;

        User user = userRepository.findUserByUsername(username).orElseThrow(() -> {
            log.error(USERNAME_NOT_FOUND);
            return new UsernameNotFoundException(USERNAME_NOT_FOUND);
        });

        user.setLastLoginDateDisplay(user.getLastLoginDate());
        user.setLastLoginDate(new Date());
        userRepository.save(user);

        UserPrincipal userPrincipal = new UserPrincipal(user);
        log.info(USER_WAS_FOUND);
        return userPrincipal;
    }
}
