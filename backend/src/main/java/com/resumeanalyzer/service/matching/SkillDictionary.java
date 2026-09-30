package com.resumeanalyzer.service.matching;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The vocabulary of recognized skill phrases used by {@link SkillMatcher}.
 *
 * Loaded once at startup from a plain-text classpath resource (one skill
 * per line), so the vocabulary can be extended without a code change.
 * This is deliberately a fixed, curated dictionary rather than a machine
 * learning model - skill matching stays deterministic and explainable.
 */
@Component
public class SkillDictionary {

    private static final String RESOURCE_PATH = "skills/technical-skills.txt";

    private final Set<String> skills;

    public SkillDictionary() {
        this.skills = load();
    }

    public Set<String> getSkills() {
        return skills;
    }

    private Set<String> load() {
        Set<String> loaded = new LinkedHashSet<>();
        ClassPathResource resource = new ClassPathResource(RESOURCE_PATH);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim().toLowerCase(Locale.ROOT);
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                loaded.add(trimmed);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load skills dictionary from " + RESOURCE_PATH, ex);
        }

        return Set.copyOf(loaded);
    }
}
