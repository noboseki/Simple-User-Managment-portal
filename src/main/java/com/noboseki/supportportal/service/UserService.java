package com.noboseki.supportportal.service;

import com.noboseki.supportportal.domain.User;
import com.noboseki.supportportal.exception.domain.EmailExistException;
import com.noboseki.supportportal.exception.domain.EmailSendException;
import com.noboseki.supportportal.exception.domain.UserNotFoundException;
import com.noboseki.supportportal.exception.domain.UsernameExistException;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User register(String firstName, String lastName, String username, String email) throws UserNotFoundException, UsernameExistException, EmailExistException, EmailSendException;

    List<User> getUsers();

    Optional<User> findByUsername(String username);

    Optional<User> findUserByEmail(String email);
}
