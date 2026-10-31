package com.skillbridge.auth.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestTest {

    @Test
    void nameIsTrimmedAndEmailIsTrimmedAndLowerCased() {
        RegisterRequest r = new RegisterRequest("  Ada Test  ", "  ADA@Example.COM ", "Passw0rd!");
        assertThat(r.fullName()).isEqualTo("Ada Test");
        assertThat(r.email()).isEqualTo("ada@example.com");
    }

    @Test
    void passwordIsNeverTrimmedOrAltered() {
        RegisterRequest r = new RegisterRequest("Ada", "ada@example.com", "  spaced pass  ");
        assertThat(r.password()).isEqualTo("  spaced pass  ");
    }

    @Test
    void toStringNeverPrintsThePassword() {
        RegisterRequest r = new RegisterRequest("Ada", "ada@example.com", "Secret-123");
        assertThat(r.toString()).doesNotContain("Secret-123").contains("<hidden>");
    }
}