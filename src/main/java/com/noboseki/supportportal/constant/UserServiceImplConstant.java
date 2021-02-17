package com.noboseki.supportportal.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserServiceImplConstant {
    public static final String NOT_FOUND_BY_USERNAME = "No user found by username: ";
    public static final String USER_ALREADY_EXIST = "User already exist";
    public static final String EMAIL_ALREADY_EXIST = "Email already exist";
    public static final String USER_WAS_FOUND = "Returning found user by username : ";
    public static final String NO_USER_FOUND_BY_EMAIL ="No user found for email: ";

}
