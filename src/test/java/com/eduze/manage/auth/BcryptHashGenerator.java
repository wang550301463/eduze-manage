package com.eduze.manage.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** One-off helper; run main to print BCrypt for seed SQL. */
public final class BcryptHashGenerator {

    private BcryptHashGenerator() {}

    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode("admin@123"));
    }
}
