package com.varcal.cheermanager.security;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Map<String, Date> blacklist = new ConcurrentHashMap<>();

    public void blacklistToken(String token, Date expiration) {
        if (token != null && expiration != null) {
            blacklist.put(token, expiration);
        }
    }

    public boolean isBlacklisted(String token) {
        if (token == null) {
            return false;
        }
        Date exp = blacklist.get(token);
        if (exp == null) {
            return false;
        }
        if (exp.before(new Date())) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

}