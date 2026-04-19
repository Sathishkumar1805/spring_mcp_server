package com.linkedin.mcp.server.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class LinkedInTextTest {

    @Test
    void highlightKeyword_wrapsMatch() {
        String s = LinkedInText.highlightKeyword("Work on Spring Boot services", "Spring");
        assertThat(s).contains("**Spring**");
    }

    @Test
    void termFrequency_dropsStopwords() {
        Map<String, Integer> tf = LinkedInText.termFrequency("the spring boot and spring");
        assertThat(tf.get("spring")).isEqualTo(2);
        assertThat(tf.containsKey("the")).isFalse();
    }
}
