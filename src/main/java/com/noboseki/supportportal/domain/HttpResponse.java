package com.noboseki.supportportal.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.http.HttpStatus;

import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonFormat.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpResponse {
    @Builder.Default
    @JsonFormat(shape = Shape.STRING, pattern = "dd-MM-yyy hh:mm:ss", timezone = "Europe/Belgrade")
    private Date timestamp = new Date();
    private int httpStatusCode;
    private HttpStatus httpStatus;
    private String reason;
    private String message;

}
