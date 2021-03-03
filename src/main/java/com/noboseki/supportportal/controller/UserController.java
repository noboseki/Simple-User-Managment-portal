package com.noboseki.supportportal.controller;

import com.noboseki.supportportal.domain.HttpResponse;
import com.noboseki.supportportal.domain.User;
import com.noboseki.supportportal.domain.UserPrincipal;
import com.noboseki.supportportal.dtos.AddNewUserDto;
import com.noboseki.supportportal.dtos.UpdateUserDto;
import com.noboseki.supportportal.exception.ExceptionHandling;
import com.noboseki.supportportal.exception.domain.NotAnImageFileException;
import com.noboseki.supportportal.exception.domain.*;
import com.noboseki.supportportal.service.UserService;
import com.noboseki.supportportal.utility.JWTTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.noboseki.supportportal.constant.FileConstant.*;
import static com.noboseki.supportportal.constant.SecurityConstant.JWT_TOKEN_HEADER;
import static com.noboseki.supportportal.constant.UserServiceImplConstant.USER_WAS_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = {"/", "user"})
public class UserController extends ExceptionHandling {
    public static final String EMAIL_SENT = "An email with a new password was sent to: ";
    public static final String USER_DELETE_SUCCESSFULLY = "User delete successfully";
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JWTTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody User user) throws UserNotFoundException {
        authenticate(user.getUsername(), user.getPassword());
        User loginUser = userService.findByUsername(user.getUsername()).orElseThrow(() -> new UserNotFoundException(""));
        UserPrincipal userPrincipal = new UserPrincipal(loginUser);
        HttpHeaders jwtHeader = getJwtHeader(userPrincipal);
        return new ResponseEntity<>(loginUser, jwtHeader, OK);
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) throws UserNotFoundException, UsernameExistException, EmailExistException, EmailSendException {
        User newUser = userService.register(user.getFirstName(), user.getLastName(), user.getUsername(), user.getEmail());
        return ResponseEntity.ok(newUser);
    }

    @PostMapping("/add")
    public ResponseEntity<User> addNewUser(@RequestParam("firstName") String firstName,
                                           @RequestParam("lastName") String lastName,
                                           @RequestParam("username") String username,
                                           @RequestParam("email") String email,
                                           @RequestParam("role") String role,
                                           @RequestParam("isActive") String isActive,
                                           @RequestParam("isNonLocked") String isNonLocked,
                                           @RequestParam(value = "profileImage", required = false) MultipartFile multipartFile)
            throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {

        User user = userService.addNewUser(AddNewUserDto.builder()
                .firstName(firstName).lastName(lastName)
                .username(username).email(email).role(role)
                .isActive(Boolean.parseBoolean(isActive))
                .isNonLocked(Boolean.parseBoolean(isNonLocked))
                .profileImage(multipartFile).build());

        return new ResponseEntity<>(user, OK);
    }

    @PostMapping("/update")
    public ResponseEntity<User> update(@RequestParam("currentUsername") String currentUsername,
                                       @RequestParam("firstName") String firstName,
                                       @RequestParam("lastName") String lastName,
                                       @RequestParam("username") String username,
                                       @RequestParam("email") String email,
                                       @RequestParam("role") String role,
                                       @RequestParam("isActive") String isActive,
                                       @RequestParam("isNonLocked") String isNonLocked,
                                       @RequestParam(value = "profileImage", required = false) MultipartFile multipartFile)
            throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {

        User updatedUser = userService.updateUser(UpdateUserDto.builder()
                .currentUsername(currentUsername).newFirstName(firstName)
                .newLastName(lastName).newUsername(username)
                .newEmail(email).role(role)
                .isActive(Boolean.parseBoolean(isActive))
                .isNonLocked(Boolean.parseBoolean(isNonLocked)).build());

        return new ResponseEntity<>(updatedUser, OK);
    }

    @GetMapping("/find/{username}")
    public ResponseEntity<User> getUser(@PathVariable("username") String username) throws UserNotFoundException {
        User user = userService.findByUsername(username).orElseThrow(() -> new UserNotFoundException(USER_WAS_FOUND + username));
        return new ResponseEntity<>(user, OK);
    }

    @GetMapping("/list")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> user = userService.getUsers();
        return new ResponseEntity<>(user, OK);
    }

    @GetMapping("/resetPassword/{email}")
    public ResponseEntity<HttpResponse> resetPassword(@PathVariable("email") String email) throws EmailNotFoundException, EmailSendException {
        userService.resetPassword(email);
        return response(OK, EMAIL_SENT + email);
    }

    @DeleteMapping("/delete/{username}")
    @PreAuthorize("hasAnyAuthority('user:delete')")
    public ResponseEntity<HttpResponse> deleteUser(@PathVariable("username")String username) throws IOException {
        userService.deleteUser(username);
        return response(NO_CONTENT, USER_DELETE_SUCCESSFULLY);
    }

    @PostMapping("/updateProfileImage")
    public ResponseEntity<User> updateProfileImage(@RequestParam("username") String username,
                                                   @RequestParam(value = "profileImage") MultipartFile multipartFile)
            throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
        User user = userService.updateProfileImage(username, multipartFile);

        return new ResponseEntity<>(user, OK);
    }

    @GetMapping(path = "/image/{username}/{fileName}", produces = IMAGE_JPEG_VALUE)
    public byte[] getProfileImage(@PathVariable("username") String username, @PathVariable("fileName") String fileName) throws IOException {
        return Files.readAllBytes(Paths.get(USER_FOLDER + username + FORWARD_SLASH + fileName));
    }

    @GetMapping(path = "/image/profile/{username}", produces = IMAGE_JPEG_VALUE)
    public byte[] getTempProfileImage(@PathVariable("username") String username) throws IOException {
        URL url = new URL(TEMP_PROFILE_IMAGE_BASE_URL + username);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (InputStream inputStream = url.openStream()) {
            int bytesRead;
            byte [] chunk = new byte[1024];
            while ((bytesRead = inputStream.read(chunk)) > 0) {
                byteArrayOutputStream.write(chunk, 0, bytesRead);
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    private ResponseEntity<HttpResponse> response(HttpStatus httpStatus, String message) {
        HttpResponse body = HttpResponse.builder()
                .httpStatus(httpStatus).httpStatusCode(httpStatus.value())
                .reason(httpStatus.getReasonPhrase().toUpperCase())
                .message(message.toUpperCase()).build();
        return new ResponseEntity<>(body, httpStatus);
    }

    private HttpHeaders getJwtHeader(UserPrincipal userPrincipal) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(JWT_TOKEN_HEADER, jwtTokenProvider.generateJwtToken(userPrincipal));
        return headers;
    }

    private void authenticate(String username, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }
}