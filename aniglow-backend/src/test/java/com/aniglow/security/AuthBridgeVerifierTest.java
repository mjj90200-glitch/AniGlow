package com.aniglow.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationServiceException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthBridgeVerifierTest {

    private static final String SECRET = "test-auth-bridge-secret-32-characters-minimum";
    private final AuthBridgeVerifier verifier = new AuthBridgeVerifier(SECRET);

    @Test
    void acceptsTheConfiguredServerSecret() {
        verifier.requireValid(SECRET);
    }

    @Test
    void rejectsMissingOrIncorrectSecrets() {
        assertThatThrownBy(() -> verifier.requireValid(null))
                .isInstanceOf(AuthenticationServiceException.class);
        assertThatThrownBy(() -> verifier.requireValid("attacker"))
                .isInstanceOf(AuthenticationServiceException.class);
    }

    @Test
    void rejectsWeakConfigurationAtStartup() {
        assertThatThrownBy(() -> new AuthBridgeVerifier("too-short"))
                .isInstanceOf(IllegalStateException.class);
    }
}
