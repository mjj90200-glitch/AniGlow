package com.aniglow.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver(
            "127.0.0.1/32,::1/128,10.0.0.0/24"
    );

    @Test
    void resolvesTheRightmostUntrustedAddressBehindKnownProxies() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.7, 10.0.0.9");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.7");
    }

    @Test
    void ignoresForwardedHeadersFromUnknownPeers() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.5");
        request.addHeader("X-Forwarded-For", "198.51.100.7");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.5");
    }

    @Test
    void normalizesIpv4MappedLoopbackAddresses() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("::ffff:127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.8");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.8");
    }
}
