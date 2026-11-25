package com.missioncart.possessions;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PossessionService {

    private final UserPossessionRepository repo;

    private static final Pattern OWNED_CLAUSE_PATTERN = Pattern.compile(
        "(?:i\\s+already\\s+(?:have|own)|already\\s+(?:have|own)|i\\s+got|i\\s+own|do\\s+not\\s+recommend|don't\\s+recommend|exclude|skip)\\s+([^.?!;]+)",
        Pattern.CASE_INSENSITIVE
    );

    public PossessionService(UserPossessionRepository repo) {
        this.repo = repo;
    }

    public List<UserPossession> listForUser(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<UserPossession> addFromInput(Long userId, String naturalText, String categorySlug, Long matchedProductId) {
        List<String> items = extractPossessionNames(naturalText);
        if (items.isEmpty() && naturalText != null && !naturalText.isBlank()) {
            items = List.of(naturalText.trim());
        }
        List<UserPossession> existing = repo.findByUserIdOrderByCreatedAtDesc(userId);
        Set<String> existingLower = new HashSet<>();
        for (UserPossession up : existing) existingLower.add(up.getItemName().toLowerCase());

        List<UserPossession> saved = new ArrayList<>();
        for (String name : items) {
            if (existingLower.contains(name.toLowerCase())) continue;
            UserPossession p = new UserPossession();
            p.setUserId(userId);
            p.setItemName(name);
            p.setCategorySlug(categorySlug);
            p.setKeywords(name.toLowerCase());
            p.setMatchedProductId(matchedProductId);
            saved.add(repo.save(p));
            existingLower.add(name.toLowerCase());
        }
        return saved;
    }

    public void deletePossession(Long userId, Long id) {
        UserPossession up = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Possession not found"));
        if (!up.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to delete this possession");
        }
        repo.delete(up);
    }

    public List<String> extractPossessionNames(String rawText) {
        if (rawText == null || rawText.isBlank()) return List.of();
        List<String> results = new ArrayList<>();
        Matcher m = OWNED_CLAUSE_PATTERN.matcher(rawText);
        String target = m.find() ? m.group(1) : rawText;

        String cleaned = target
            .replaceAll("^(?:a|an|the|my|some)\\s+", "")
            .replaceAll("\\s+and\\s+", ",")
            .replaceAll("\\s+with\\s+", ",");

        Set<String> pronouns = Set.of("them", "it", "those", "these", "anything", "nothing");
        for (String part : cleaned.split(",")) {
            String item = part.trim()
                .replaceAll("^(?:a|an|the|my|some)\\s+", "")
                .replaceAll("\\s+(?:already|too)$", "")
                .trim();
            if (item.length() >= 2 && item.length() <= 60 && !pronouns.contains(item.toLowerCase())) {
                results.add(item);
            }
        }
        return results;
    }

    public boolean matchesOwnedItem(String productName, String productDescription, String categorySlug, List<String> ownedTerms) {
        if (ownedTerms == null || ownedTerms.isEmpty()) return false;
        String nameLower = productName == null ? "" : productName.toLowerCase();
        String descLower = productDescription == null ? "" : productDescription.toLowerCase();

        for (String owned : ownedTerms) {
            String token = owned.toLowerCase().trim();
            if (token.length() < 3) continue;
            // Exact item or head-noun match; never exclude an entire broad category unless explicitly stated
            if (nameLower.contains(token)) return true;
            String[] words = token.split("\\s+");
            if (words.length == 1 && (nameLower.matches(".*\\b" + Pattern.quote(token) + "\\b.*"))) {
                return true;
            }
            if (words.length > 1 && Arrays.stream(words).filter(w -> w.length() > 3).allMatch(w -> nameLower.contains(w) || descLower.contains(w))) {
                return true;
            }
        }
        return false;
    }
}
