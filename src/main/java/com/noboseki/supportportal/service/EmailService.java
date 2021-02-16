package com.noboseki.supportportal.service;

import com.noboseki.supportportal.exception.domain.EmailSendException;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;

import static com.noboseki.supportportal.constant.EmailConstant.*;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender javaMailSender;

    @Async
    public void sendEmail(SimpleMailMessage email) throws EmailSendException {
        try {
            javaMailSender.send(email);
        } catch (Exception e) {
            throw new EmailSendException(EXCEPTION_MESSAGE + Arrays.toString(email.getTo()));
        }
    }

    public SimpleMailMessage activationEmileSender(String password, String emile) throws EmailSendException {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(emile);
        mailMessage.setSubject(REGISTRATION_SUBJECT);
        mailMessage.setFrom(FROM_EMAIL);
        mailMessage.setSentDate(new Date());
        mailMessage.setText("Hello " + emile + ", \n \n Your new account password is: " + password +
                "\n \n The Support Team");

        sendEmail(mailMessage);

        return mailMessage;
    }
}
