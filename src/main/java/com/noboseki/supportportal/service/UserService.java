package com.noboseki.supportportal.service;

import com.noboseki.supportportal.domain.User;
import com.noboseki.supportportal.dtos.AddNewUserDto;
import com.noboseki.supportportal.dtos.UpdateUserDto;
import com.noboseki.supportportal.exception.domain.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface UserService {

    User register(String firstName, String lastName, String username, String email) throws UserNotFoundException, UsernameExistException, EmailExistException, EmailSendException;

    List<User> getUsers();

    Optional<User> findByUsername(String username);

    Optional<User> findUserByEmail(String email);

    User addNewUser(AddNewUserDto addNewUserDto)
            throws UserNotFoundException, UsernameExistException, EmailExistException, IOException;

    User updateUser(UpdateUserDto updateUserDto) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException;

    void deleteUser(long id);

    void resetPassword(String email) throws EmailNotFoundException, EmailSendException;

    User updateProfileImage(String username, MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException;
}
