package com.resumeanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the AI-Powered Resume & Job Match Analyzer backend.
 *
 * Phase 0 established application bootstrap, MySQL/JPA wiring, security
 * scaffolding, and configuration infrastructure. Phase 1 adds JWT-based
 * authentication and role management (POST /api/auth/register,
 * POST /api/auth/login) on top of that foundation. No resume/matching/ATS
 * business functionality exists yet - that begins in Phase 2 onward.
 *
 * The configuration-properties scan is broadened from "com.resumeanalyzer.config"
 * to the whole "com.resumeanalyzer" base package so that JwtProperties
 * (in the security package) is also picked up, without needing to keep
 * expanding an explicit list of packages in every future phase.
 */
@SpringBootApplication
@ConfigurationPropertiesScan("com.resumeanalyzer")
public class ResumeAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumeAnalyzerApplication.class, args);
    }
}
