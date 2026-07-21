package com.wut.practicum.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class IpValidator {
    private final List<IpRange> ranges = new ArrayList<>();

    public IpValidator(@Value("${oa.attendance.allowed-ips:127.0.0.1,0:0:0:0:0:0:0:1,192.168.0.0/16,10.0.0.0/8,172.16.0.0/12}") String allowedIpsConfig) {
        if (allowedIpsConfig != null && !allowedIpsConfig.trim().isEmpty()) {
            List<String> allowedIps = Arrays.asList(allowedIpsConfig.split(","));
            for (String allowedIp : allowedIps) {
                allowedIp = allowedIp.trim();
                try {
                    if (allowedIp.contains("/")) {
                        String[] parts = allowedIp.split("/");
                        String ip = parts[0];
                        int prefixLength = Integer.parseInt(parts[1]);
                        ranges.add(new IpRange(InetAddress.getByName(ip), prefixLength));
                    } else {
                        ranges.add(new IpRange(InetAddress.getByName(allowedIp), -1));
                    }
                } catch (Exception e) {
                    // Ignore malformed subnets/IPs
                }
            }
        }
    }

    public boolean isValid(String clientIp) {
        if (clientIp == null || clientIp.trim().isEmpty()) {
            return false;
        }
        try {
            // Remove IPv6 zone index if present (e.g. fe80::1%12)
            if (clientIp.contains("%")) {
                clientIp = clientIp.substring(0, clientIp.indexOf("%"));
            }
            InetAddress clientAddr = InetAddress.getByName(clientIp);
            for (IpRange range : ranges) {
                if (range.matches(clientAddr)) {
                    return true;
                }
            }
        } catch (UnknownHostException e) {
            return false;
        }
        return false;
    }

    private static class IpRange {
        private final InetAddress address;
        private final int prefixLength;

        public IpRange(InetAddress address, int prefixLength) {
            this.address = address;
            this.prefixLength = prefixLength;
        }

        public boolean matches(InetAddress clientAddress) {
            if (prefixLength == -1) {
                return address.equals(clientAddress);
            }
            byte[] addressBytes = address.getAddress();
            byte[] clientBytes = clientAddress.getAddress();
            if (addressBytes.length != clientBytes.length) {
                return false;
            }
            int bits = prefixLength;
            for (int i = 0; i < addressBytes.length && bits > 0; i++) {
                int mask = 0xFF;
                if (bits < 8) {
                    mask = (mask << (8 - bits)) & 0xFF;
                    bits = 0;
                } else {
                    bits -= 8;
                }
                if ((addressBytes[i] & mask) != (clientBytes[i] & mask)) {
                    return false;
                }
            }
            return true;
        }
    }
}
