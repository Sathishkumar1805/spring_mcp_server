package com.linkedin.mcp.server.support;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Human-readable formatting and light parsing helpers for LinkedIn export JSON (dates are not uniform).
 */
public final class LinkedInText {

    private static final Pattern YEAR_MONTH = Pattern.compile("(\\d{4})\\s*[-/]\\s*(\\d{1,2})");
    private static final Pattern YEAR_ONLY = Pattern.compile("(\\d{4})");

    private LinkedInText() {}

    /**
     * Formats arbitrary LinkedIn date representations for display.
     *
     * @param value Jackson-deserialized date (String, Map, or null)
     * @return readable text, or empty string
     */
    public static String formatDate(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String s) {
            return s.trim();
        }
        if (value instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) map;
            Object y = m.get("year");
            Object month = m.get("month");
            if (y != null && month != null) {
                return month + "/" + y;
            }
            if (y != null) {
                return String.valueOf(y);
            }
            return map.toString();
        }
        if (value instanceof Number n) {
            return String.valueOf(n);
        }
        return String.valueOf(value);
    }

    /**
     * @return true if the value represents a current role (LinkedIn often uses "Present").
     */
    public static boolean isPresentEndDate(Object endDate) {
        String s = formatDate(endDate);
        if (s.isEmpty()) {
            return true;
        }
        return "present".equalsIgnoreCase(s) || "current".equalsIgnoreCase(s);
    }

    /**
     * Parses a sortable month index (year*12+month) when possible, else null.
     */
    public static Long parseYearMonthIndex(Object value) {
        String s = formatDate(value);
        if (s.isEmpty()) {
            return null;
        }
        Matcher ym = YEAR_MONTH.matcher(s);
        if (ym.find()) {
            int year = Integer.parseInt(ym.group(1));
            int month = Integer.parseInt(ym.group(2));
            return year * 12L + month;
        }
        Matcher y = YEAR_ONLY.matcher(s);
        if (y.find()) {
            int year = Integer.parseInt(y.group(1));
            return year * 12L;
        }
        try {
            YearMonth ym2 = YearMonth.parse(s.trim());
            return ym2.getYear() * 12L + ym2.getMonthValue();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Highlights occurrences of a keyword in text using markdown bold (best-effort, case-insensitive).
     */
    public static String highlightKeyword(String text, String keyword) {
        if (text == null || text.isEmpty() || keyword == null || keyword.isBlank()) {
            return text == null ? "" : text;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        String kw = keyword.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf(kw);
        if (idx < 0) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        int from = 0;
        while (idx >= 0) {
            sb.append(text, from, idx);
            sb.append("**");
            sb.append(text, idx, idx + keyword.length());
            sb.append("**");
            from = idx + keyword.length();
            idx = lower.indexOf(kw, from);
        }
        sb.append(text.substring(from));
        return sb.toString();
    }

    /**
     * Extracts a bounded snippet around the first keyword match.
     */
    public static String snippetAroundKeyword(String text, String keyword, int radius) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (keyword == null || keyword.isBlank()) {
            return text.length() > radius * 2 ? text.substring(0, radius * 2) + "…" : text;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf(keyword.toLowerCase(Locale.ROOT));
        if (idx < 0) {
            return text.length() > 400 ? text.substring(0, 400) + "…" : text;
        }
        int start = Math.max(0, idx - radius);
        int end = Math.min(text.length(), idx + keyword.length() + radius);
        String prefix = start > 0 ? "…" : "";
        String suffix = end < text.length() ? "…" : "";
        return prefix + text.substring(start, end) + suffix;
    }

    private static final List<String> STOPWORDS = List.of(
            "the", "and", "for", "with", "from", "that", "this", "have", "has", "was", "were", "are", "been",
            "into", "over", "such", "using", "use", "our", "your", "their", "they", "them", "will", "would",
            "work", "team", "role", "job", "company", "experience", "years", "year", "month", "months", "all",
            "not", "but", "also", "including", "include", "included", "various", "multiple", "several", "many",
            "senior", "lead", "principal", "staff", "engineer", "developer", "development", "software", "solutions",
            "solution", "services", "service", "based", "new", "key", "high", "large", "small", "across", "both",
            "other", "more", "most", "some", "any", "each", "every", "within", "while", "during", "through", "via");

    /**
     * Very small tokenizer for expertise analysis: splits on non-alphanumeric, drops stopwords and short tokens.
     */
    public static Map<String, Integer> termFrequency(String text) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return out;
        }
        String[] parts = text.toLowerCase(Locale.ROOT).split("[^a-z0-9+#./]+");
        for (String p : parts) {
            if (p.length() < 3) {
                continue;
            }
            if (STOPWORDS.contains(p)) {
                continue;
            }
            out.merge(p, 1, Integer::sum);
        }
        return out;
    }
}
