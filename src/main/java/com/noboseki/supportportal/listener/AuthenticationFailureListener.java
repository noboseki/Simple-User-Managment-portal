package com.noboseki.supportportal.listener;

import com.noboseki.supportportal.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationFailureListener {
    private final LoginAttemptService attemptService;

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if (principal instanceof String) {
            String username = (String) event.getAuthentication().getPrincipal();
            attemptService.addUserToLoginAttemptCache(username);
        }
    }
}
