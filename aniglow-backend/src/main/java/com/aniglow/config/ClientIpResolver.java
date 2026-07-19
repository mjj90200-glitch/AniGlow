package com.aniglow.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ClientIpResolver {

    private final List<Subnet> trustedProxies;

    public ClientIpResolver(
            @Value("${aniglow.security.trusted-proxies:127.0.0.1/32,::1/128}") String configuredProxies
    ) {
        this.trustedProxies = Arrays.stream(configuredProxies.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Subnet::parse)
                .toList();
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddress = normalize(request.getRemoteAddr());
        if (!isTrusted(remoteAddress)) {
            return remoteAddress;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return remoteAddress;
        }

        List<String> chain = new ArrayList<>();
        for (String value : forwardedFor.split(",")) {
            String candidate = normalize(value);
            if (isAddress(candidate)) {
                chain.add(candidate);
            }
        }

        for (int index = chain.size() - 1; index >= 0; index--) {
            String candidate = chain.get(index);
            if (!isTrusted(candidate)) {
                return candidate;
            }
        }
        return chain.isEmpty() ? remoteAddress : chain.getFirst();
    }

    private boolean isTrusted(String address) {
        return trustedProxies.stream().anyMatch(subnet -> subnet.contains(address));
    }

    private boolean isAddress(String value) {
        if (!value.matches("[0-9A-Fa-f:.]+")) {
            return false;
        }
        try {
            InetAddress.getByName(value);
            return true;
        } catch (UnknownHostException exception) {
            return false;
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        String normalized = value.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.startsWith("::ffff:")) {
            normalized = normalized.substring("::ffff:".length());
        }
        return normalized;
    }

    private record Subnet(byte[] network, int prefixLength) {

        private static Subnet parse(String value) {
            String[] parts = value.split("/", 2);
            if (!parts[0].matches("[0-9A-Fa-f:.]+")) {
                throw new IllegalArgumentException("可信代理必须使用 IP 地址或 CIDR: " + value);
            }
            try {
                byte[] address = InetAddress.getByName(parts[0]).getAddress();
                int prefix = parts.length == 2 ? Integer.parseInt(parts[1]) : address.length * 8;
                if (prefix < 0 || prefix > address.length * 8) {
                    throw new IllegalArgumentException("可信代理 CIDR 前缀无效: " + value);
                }
                return new Subnet(address, prefix);
            } catch (UnknownHostException | NumberFormatException exception) {
                throw new IllegalArgumentException("可信代理地址无效: " + value, exception);
            }
        }

        private boolean contains(String candidate) {
            try {
                byte[] address = InetAddress.getByName(candidate).getAddress();
                if (address.length != network.length) {
                    return false;
                }
                int fullBytes = prefixLength / 8;
                int remainingBits = prefixLength % 8;
                for (int index = 0; index < fullBytes; index++) {
                    if (address[index] != network[index]) {
                        return false;
                    }
                }
                if (remainingBits == 0) {
                    return true;
                }
                int mask = 0xFF << (8 - remainingBits);
                return (address[fullBytes] & mask) == (network[fullBytes] & mask);
            } catch (UnknownHostException exception) {
                return false;
            }
        }
    }
}
