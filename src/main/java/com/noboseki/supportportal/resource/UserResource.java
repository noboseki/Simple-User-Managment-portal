package com.noboseki.supportportal.resource;

import com.noboseki.supportportal.domain.User;
import com.noboseki.supportportal.exception.domain.ExceptionHandling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = {"/", "user"})
public class UserResource extends ExceptionHandling {

    @GetMapping
    public User showUser() {
        return new User();
    }
}