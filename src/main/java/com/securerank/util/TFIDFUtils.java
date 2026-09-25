package com.securerank.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@Slf4j
public class TFIDFUtils {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
            "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being",
            "below", "between", "both", "but", "by", "can't", "cannot", "could", "couldn't",
            "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during",
            "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't",
            "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here",
            "here's", "hers", "herself", "him", "himself", "his", "how", "how's", "i",
            "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's",
            "its", "itself", "let's", "me", "more", "most", "mustn't", "my", "myself",
            "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought",
            "our", "ours", "ourselves", "out", "over", "own", "same", "shan't", "she",
            "she'd", "she'll", "she's", "should", "shouldn't", "so", "some", "such",
            "than", "that", "that's", "the", "their", "theirs", "them", "themselves",
            "then", "there", "there's", "these", "they", "they'd", "they'll", "they're",
            "they've", "this", "those", "through", "to", "too", "under", "until", "up",
            "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were",
            "weren't", "what", "what's", "when", "when's", "where", "where's", "which",
            "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would",
            "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours",
            "yourself", "yourselves"
    ));

    public String buildIndexVector(String content, String keywords) {
        String combined = (content != null ? content : "") + " " + (keywords != null ? keywords : "");
        String[] rawTokens = combined.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", " ").split("\\s+");

        List<String> validTokens = new ArrayList<>();
        for (String token : rawTokens) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty() && !STOP_WORDS.contains(trimmed) && trimmed.length() > 1) {
                validTokens.add(trimmed);
            }
        }

        if (validTokens.isEmpty()) {
            return "";
        }

        Map<String, Integer> frequencyMap = new HashMap<>();
        for (String token : validTokens) {
            frequencyMap.put(token, frequencyMap.getOrDefault(token, 0) + 1);
        }

        int totalWords = validTokens.size();
        StringBuilder vectorBuilder = new StringBuilder();

        for (Map.Entry<String, Integer> entry : frequencyMap.entrySet()) {
            double tf = (double) entry.getValue() / totalWords;
            if (vectorBuilder.length() > 0) {
                vectorBuilder.append("|");
            }
            vectorBuilder.append(entry.getKey())
                    .append(":")
                    .append(String.format(Locale.US, "%.4f", tf));
        }

        return Base64.getEncoder().encodeToString(vectorBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

    public double calculateRelevanceScore(String indexVectorBase64, List<String> searchKeywords, long totalDocuments) {
        if (indexVectorBase64 == null || indexVectorBase64.trim().isEmpty() || searchKeywords == null || searchKeywords.isEmpty()) {
            return 0.0;
        }

        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(indexVectorBase64), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return 0.0;
        }

        Map<String, Double> termFrequencies = new HashMap<>();
        String[] pairs = decoded.split("\\|");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                try {
                    termFrequencies.put(kv[0].trim().toLowerCase(), Double.parseDouble(kv[1].trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        double totalScore = 0.0;
        double idfWeight = Math.log10((double) Math.max(totalDocuments, 1) + 1.0);

        for (String keyword : searchKeywords) {
            String cleanKw = keyword.trim().toLowerCase();
            if (termFrequencies.containsKey(cleanKw)) {
                double tf = termFrequencies.get(cleanKw);
                totalScore += tf * (1.0 + idfWeight);
            }
        }

        return Math.round(totalScore * 10000.0) / 10000.0;
    }
}
