package com.resumeanalyzer.service.matching;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextNormalizerTest {

    private final TextNormalizer normalizer = new TextNormalizer();

    @Test
    void normalizeToTermSetReturnsEmptyForNullOrBlank() {
        assertEquals(Set.of(), normalizer.normalizeToTermSet(null));
        assertEquals(Set.of(), normalizer.normalizeToTermSet("   "));
    }

    @Test
    void normalizeToTermSetLowercasesAndDeduplicates() {
        Set<String> terms = normalizer.normalizeToTermSet("Java JAVA java Spring");

        assertTrue(terms.contains("java"));
        assertTrue(terms.contains("spring"));
        assertEquals(2, terms.size());
    }

    @Test
    void normalizeToTermSetRemovesStopwords() {
        Set<String> terms = normalizer.normalizeToTermSet("the developer with the strongest skills");

        assertFalse(terms.contains("the"));
        assertFalse(terms.contains("with"));
        assertTrue(terms.contains("developer"));
        assertTrue(terms.contains("strongest"));
        assertTrue(terms.contains("skills"));
    }

    @Test
    void normalizeToPhraseTextReturnsEmptyForNullOrBlank() {
        assertEquals("", normalizer.normalizeToPhraseText(null));
        assertEquals("", normalizer.normalizeToPhraseText(""));
    }

    @Test
    void normalizeToPhraseTextLowercasesAndCollapsesWhitespace() {
        String result = normalizer.normalizeToPhraseText("Spring   Boot\nDeveloper");

        assertEquals("spring boot developer", result);
    }
}
