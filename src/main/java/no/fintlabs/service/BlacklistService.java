package no.fintlabs.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Slf4j
public class BlacklistService {

    @Value("${fint.graphql.blacklist:}")
    private Set<String> blacklistedIpaddresses;

    @PostConstruct
    public void logIpAddresses() {
        log.info("Blocked ip addresses: ");
        blacklistedIpaddresses.forEach(log::info);
    }

    public boolean isBlacklisted(String ipAddress) {
        return blacklistedIpaddresses.contains(ipAddress);
    }

}
