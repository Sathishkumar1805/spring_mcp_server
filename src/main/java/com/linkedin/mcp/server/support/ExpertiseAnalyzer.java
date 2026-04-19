package com.linkedin.mcp.server.support;

import com.linkedin.mcp.server.domain.Position;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ranks expertise themes from positions using duration-weighted term frequency.
 */
public final class ExpertiseAnalyzer {

    private ExpertiseAnalyzer() {}

    /**
     * Computes weighted term scores from titles and descriptions.
     *
     * @param positions work history
     * @param topN number of themes to return
     * @return ranked expertise lines with evidence
     */
    public static List<String> rankExpertise(List<Position> positions, int topN) {
        Map<String, Double> scores = new HashMap<>();
        Map<String, String> evidence = new HashMap<>();

        for (Position p : positions) {
            double months = estimateMonths(p);
            String blob = String.join(
                    " ",
                    safe(p.getTitle()),
                    safe(p.getCompanyName()),
                    safe(p.getDescription()));
            Map<String, Integer> tf = LinkedInText.termFrequency(blob);
            for (Map.Entry<String, Integer> e : tf.entrySet()) {
                String term = e.getKey();
                double w = e.getValue() * Math.max(1.0, months);
                scores.merge(term, w, Double::sum);
                evidence.putIfAbsent(term, buildEvidenceLine(p, term));
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(Math.max(1, topN))
                .map(e -> String.format(
                        Locale.US,
                        "- **%s** — score %.1f; evidence: %s",
                        e.getKey(),
                        e.getValue(),
                        evidence.getOrDefault(e.getKey(), "(see positions)")))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static String buildEvidenceLine(Position p, String term) {
        String title = safe(p.getTitle());
        String company = safe(p.getCompanyName());
        String hint = (title + " @ " + company).trim();
        String desc = safe(p.getDescription());
        if (desc.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT))) {
            String snip = LinkedInText.snippetAroundKeyword(desc, term, 80);
            return hint + "; " + LinkedInText.highlightKeyword(snip, term);
        }
        return hint;
    }

    private static double estimateMonths(Position p) {
        Long start = LinkedInText.parseYearMonthIndex(p.getStartDate());
        Long end;
        if (LinkedInText.isPresentEndDate(p.getEndDate())) {
            YearMonth now = YearMonth.now();
            end = now.getYear() * 12L + now.getMonthValue();
        } else {
            end = LinkedInText.parseYearMonthIndex(p.getEndDate());
        }
        if (start != null && end != null && end >= start) {
            return Math.max(1.0, (end - start) + 1.0);
        }
        return 12.0;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Orders positions for display: current first, then reverse-chronological by start date when parseable.
     */
    public static List<Position> chronologicalForDisplay(List<Position> positions) {
        List<Position> copy = new ArrayList<>(positions);
        copy.sort(Comparator.comparing((Position p) -> !LinkedInText.isPresentEndDate(p.getEndDate()))
                .thenComparing(
                        (Position p) -> LinkedInText.parseYearMonthIndex(p.getStartDate()),
                        Comparator.nullsLast(Comparator.reverseOrder())));
        return copy;
    }
}
