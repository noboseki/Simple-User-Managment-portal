package com.noboseki.supportportal.resource;

import com.noboseki.supportportal.domain.User;
import com.noboseki.supportportal.exception.domain.EmailExistException;
import com.noboseki.supportportal.exception.domain.ExceptionHandling;
import com.noboseki.supportportal.exception.domain.UserNotFoundException;
import com.noboseki.supportportal.exception.domain.UsernameExistException;
import com.noboseki.supportportal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = {"/", "user"})
public class UserResource extends ExceptionHandling {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) throws UserNotFoundException, UsernameExistException, EmailExistException {
        User newUser = userService.register(user.getFirstName(), user.getLastName(), user.getUsername(), user.getEmail());
        return ResponseEntity.ok(newUser);
    }
}