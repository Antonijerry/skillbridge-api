package com.skillbridge.common;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Failures raised by the servlet container (before Spring MVC) end up here, in our error format. */
@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        Object attribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = attribute instanceof Integer code && HttpStatus.resolve(code) != null
                ? HttpStatus.valueOf(code)
                : HttpStatus.NOT_FOUND;

        String code;
        String message;
        if (status == HttpStatus.BAD_REQUEST) {
            code = "BAD_REQUEST";
            message = "The request could not be understood";
        } else if (status == HttpStatus.NOT_FOUND) {
            code = "NOT_FOUND";
            message = "No such path";
        } else if (status.is5xxServerError()) {
            code = "INTERNAL_ERROR";
            message = "Unexpected error";
        } else {
            code = "REQUEST_REJECTED";
            message = "The request was rejected";
        }

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(Map.of("error", error));
    }
}