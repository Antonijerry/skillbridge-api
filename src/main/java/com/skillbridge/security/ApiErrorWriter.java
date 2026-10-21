package com.skillbridge.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

/** Writes the project error format from the security filters, where @RestControllerAdvice cannot reach. */
final class ApiErrorWriter {

    private ApiErrorWriter() { }

    /** Code and message must be fixed strings: nothing from the request is ever echoed back. */
    static void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}}");
    }
}