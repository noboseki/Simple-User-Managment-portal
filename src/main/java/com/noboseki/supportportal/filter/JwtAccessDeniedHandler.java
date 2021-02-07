package com.noboseki.supportportal.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noboseki.supportportal.domain.HttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

import static com.noboseki.supportportal.constant.SecurityConstant.ACCESS_DENIED_MESSAGE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        HttpResponse httpResponse = HttpResponse.builder()
                .httpStatusCode(UNAUTHORIZED.value())
                .httpStatus(UNAUTHORIZED)
                .reason(UNAUTHORIZED.getReasonPhrase().toUpperCase())
                .message(ACCESS_DENIED_MESSAGE).build();

        ObjectMapper mapper = new ObjectMapper();

        response.setContentType(APPLICATION_JSON_VALUE);
        response.setStatus(UNAUTHORIZED.value());

        OutputStream outputStream = response.getOutputStream();
        mapper.writeValue(outputStream, httpResponse);
        outputStream.flush();
    }
}
