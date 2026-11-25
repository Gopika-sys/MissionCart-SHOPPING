package com.missioncart.ai;

import com.missioncart.ai.AIModels.ParsedIntent;
import com.missioncart.ai.AIModels.RequirementSpec;
import com.missioncart.possessions.PossessionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DeterministicFallbackEngine implements AIProvider {

    private final PossessionService possessionService;

    private record ConceptRule(
        List<String> triggers,
        String label,
        String categorySlug,
        List<String> keywords,
        String priority,
        String reason
    ) {}

    private static final List<ConceptRule> CONCEPT_RULES = List.of(
        // Bedroom / House / Apartment / Hostel / Storage
        new ConceptRule(List.of("bedroom", "bedside", "small bedroom", "furniture", "decor", "house", "new house"), "Ambient Lighting & Lamp", "electronics", List.of("lamp", "desk lamp", "string lights", "candle"), "ESSENTIAL", "Recommended because your room setup benefits from warm focused lighting"),
        new ConceptRule(List.of("bedroom", "bedding", "bed", "hostel", "dorm", "house", "new house", "apartment"), "Cotton Bedding & Cushions", "home-decor", List.of("bedsheet", "cushion", "blanket", "throw"), "ESSENTIAL", "Core sleep and soft-furnishing essentials for a comfortable room"),
        new ConceptRule(List.of("bedroom", "small bedroom", "storage", "closet", "hostel", "apartment", "house", "organize"), "Space-Saving Storage & Organizers", "storage-organization", List.of("basket", "organizer", "hanger", "utility cart"), "ESSENTIAL", "Recommended because compact spaces require vertical and modular storage"),
        new ConceptRule(List.of("bedroom", "decor", "vase", "house", "apartment"), "Ceramic Vase & Accent Decor", "home-decor", List.of("vase", "candle", "cushion"), "OPTIONAL", "Adds warm editorial character without crowding floor space"),

        // Hostel & Living Essentials
        new ConceptRule(List.of("hostel", "dorm", "pg", "first month", "house", "new house", "apartment"), "Electric Kettle / Quick Brew", "kitchen", List.of("kettle", "electric kettle"), "ESSENTIAL", "Quick boiling for tea, coffee, and warm water in a new home or hostel"),
        new ConceptRule(List.of("hostel", "bath", "first month", "house", "travel", "manali", "trip"), "Bath Towels & Toiletry Organizer", "personal-care", List.of("towel", "toiletry", "pouch"), "ESSENTIAL", "Daily hygiene and bathroom essentials"),
        new ConceptRule(List.of("hostel", "desk", "study", "wfh", "remote", "electronics", "apartment", "house", "bedroom"), "Surge-Protected Power Strip", "electronics", List.of("power strip", "surge", "extension"), "ESSENTIAL", "Safe multi-outlet power distribution for chargers, lamps, and appliances"),

        // Study, Programming & Work From Home
        new ConceptRule(List.of("programming", "coding", "software", "developer", "laptop for"), "Laptop Computer (Core Device)", "electronics", List.of("programming laptop", "notebook computer", "macbook", "thinkpad"), "ESSENTIAL", "Primary computing hardware requested for programming"),
        new ConceptRule(List.of("study", "college", "desk", "wfh", "remote", "workspace", "productive", "programming"), "LED Study & Desk Lamp", "electronics", List.of("lamp", "desk lamp"), "ESSENTIAL", "Focused glare-free illumination for long desk sessions"),
        new ConceptRule(List.of("study", "laptop", "wfh", "remote", "workspace", "ergonomic", "programming"), "Laptop Stand, Desk Mat & USB-C Hub", "study-workspace", List.of("laptop stand", "usb", "hub", "desk mat"), "ESSENTIAL", "Ergonomic screen elevation and clean desk connectivity"),
        new ConceptRule(List.of("study", "notebook", "journal", "college", "planner", "workspace"), "Notebooks & Desk Organizer", "study-workspace", List.of("notebook", "organizer", "pen"), "ESSENTIAL", "Structured note-taking and clutter-free stationery organization"),
        new ConceptRule(List.of("study", "wfh", "remote", "focus", "office", "entertainment", "music", "headphones"), "Wireless Focus Headphones", "electronics", List.of("headphones", "wireless"), "OPTIONAL", "Blocks background distractions during calls, coding, or media"),
        new ConceptRule(List.of("study", "wfh", "desk", "mouse", "programming", "workspace"), "Wireless Ergonomic Mouse", "electronics", List.of("mouse", "wireless mouse"), "OPTIONAL", "Accurate wrist-friendly pointing device for desk setups"),

        // Home Entertainment
        new ConceptRule(List.of("entertainment", "home entertainment", "party", "speaker", "music", "movie"), "Portable Bluetooth Speaker", "electronics", List.of("speaker", "bluetooth speaker"), "ESSENTIAL", "Room-filling wireless audio for music, movies, and gatherings"),

        // Office / Wedding / Fashion / Gifting
        new ConceptRule(List.of("office", "job", "interview", "formal", "professional"), "Tailored Cotton Shirt & Blazer", "fashion", List.of("shirt", "blazer", "cotton shirt"), "ESSENTIAL", "Sharp workplace attire ready for your first office job"),
        new ConceptRule(List.of("office", "job", "footwear", "shoes", "wedding", "formal"), "Formal Loafers / Versatile Sneakers", "fashion", List.of("loafer", "sneakers"), "ESSENTIAL", "Comfortable all-day footwear matched to your setting"),
        new ConceptRule(List.of("office", "job", "laptop bag", "commute", "college"), "Commuter Laptop Backpack / Tote", "fashion", List.of("backpack", "tote"), "ESSENTIAL", "Padded protection for daily office commutes"),
        new ConceptRule(List.of("office", "wedding", "accessory", "watch", "gift", "best friend"), "Classic Watch / Eyewear / Jewellery", "fashion", List.of("watch", "sunglasses", "jewellery"), "OPTIONAL", "Refined everyday accessory or thoughtful birthday gift"),
        new ConceptRule(List.of("wedding", "festive", "ethnic", "shaadi", "sangeet", "ceremony"), "Festive Ethnic Outfit", "fashion", List.of("kurta", "ethnic", "silk"), "ESSENTIAL", "Occasion-appropriate festive attire"),
        new ConceptRule(List.of("wedding", "grooming", "office"), "Precision Grooming Trimmer", "electronics", List.of("trimmer", "clipper"), "OPTIONAL", "Clean grooming finish before workdays and events"),

        // Gift Shopping
        new ConceptRule(List.of("gift", "best friend", "present", "birthday gift"), "Curated Keepsake Gift (Candle / Mug / Watch)", "everyday-essentials", List.of("candle", "mug", "watch", "notebook", "vase"), "ESSENTIAL", "Thoughtful, lasting gift option tailored to daily enjoyment"),

        // Travel & Camping
        new ConceptRule(List.of("travel", "trip", "manali", "mountain", "vacation", "flight", "five-day", "5 day"), "Cabin Suitcase & Packing Cubes", "travel", List.of("suitcase", "packing cube", "backpack"), "ESSENTIAL", "Organized carry luggage suited for multi-day trips"),
        new ConceptRule(List.of("camping", "camp", "tent", "trek", "outdoor"), "Camping Tent & Sleeping Gear", "travel", List.of("camping tent", "sleeping bag", "lantern"), "ESSENTIAL", "Dedicated shelter and sleeping gear for outdoor camping"),
        new ConceptRule(List.of("manali", "cold", "winter", "hill", "camping", "trek"), "Packable Thermal Puffer Jacket", "fashion", List.of("puffer", "jacket", "winter"), "ESSENTIAL", "Insulated lightweight warmth for chilly evenings outdoors"),
        new ConceptRule(List.of("travel", "trip", "manali", "camping", "flight", "train", "charger"), "Power Bank & Universal Travel Adapter", "electronics", List.of("power bank", "adapter", "neck pillow"), "ESSENTIAL", "Reliable backup power and plug compatibility away from home"),
        new ConceptRule(List.of("travel", "trip", "manali", "camping", "outdoor", "fitness"), "Insulated Stainless Water Bottle", "kitchen", List.of("water bottle", "insulated"), "ESSENTIAL", "Keeps hydration cold or warm through transit and outdoor days"),

        // Skincare & Beauty
        new ConceptRule(List.of("skincare", "oily", "acne", "skin", "morning", "night", "routine", "face"), "Gentle Facial Cleanser", "skincare", List.of("cleanser", "salicylic"), "ESSENTIAL", "Step 1 gentle cleansing without stripping natural moisture"),
        new ConceptRule(List.of("skincare", "oily", "serum", "niacinamide", "routine", "vitamin c"), "Targeted Balancing Serum", "skincare", List.of("serum", "niacinamide", "vitamin c"), "ESSENTIAL", "Active treatment serum matched to your skin routine"),
        new ConceptRule(List.of("skincare", "moisturizer", "oily", "dry", "routine"), "Lightweight Gel Moisturizer", "skincare", List.of("moisturizer", "gel"), "ESSENTIAL", "Balanced hydration layer"),
        new ConceptRule(List.of("skincare", "sunscreen", "spf", "morning", "manali", "travel", "camping"), "Broad-Spectrum SPF 50 Sunscreen", "skincare", List.of("sunscreen", "spf"), "ESSENTIAL", "Daily broad-spectrum UV protection"),

        // Kitchen & Groceries
        new ConceptRule(List.of("kitchen", "cook", "apartment", "home", "house", "new house", "three people"), "Induction Cooktop & Frying Pan", "kitchen", List.of("induction", "frying pan", "cookware"), "ESSENTIAL", "Core cooking setup for daily meals"),
        new ConceptRule(List.of("kitchen", "storage", "apartment", "three people", "meal"), "Glass Food Containers & Dinner Plates", "kitchen", List.of("containers", "plates", "mug"), "ESSENTIAL", "Everyday dining tableware and airtight meal storage"),
        new ConceptRule(List.of("grocery", "groceries", "coffee", "tea", "breakfast", "pantry", "snacks", "house"), "Pantry Staples (Coffee, Tea & Oats)", "groceries", List.of("coffee", "tea", "oats", "trail mix"), "ESSENTIAL", "Wholesome breakfast and daily beverage pantry staples"),

        // Fitness
        new ConceptRule(List.of("fitness", "workout", "gym", "exercise", "yoga", "home workout"), "Non-Slip 6mm Yoga & Exercise Mat", "fitness", List.of("yoga mat", "mat"), "ESSENTIAL", "Cushioned floor grip for home workouts"),
        new ConceptRule(List.of("fitness", "workout", "gym", "strength", "dumbbell"), "Dumbbells, Resistance Bands & Jump Rope", "fitness", List.of("dumbbell", "resistance band", "jump rope"), "ESSENTIAL", "Versatile home strength and cardio training kit"),

        // Pet Essentials
        new ConceptRule(List.of("pet", "dog", "cat", "puppy", "kitten"), "Pet Food, Bowls & Care Accessories", "everyday-essentials", List.of("pet food", "dog bowl", "cat litter", "pet toy"), "ESSENTIAL", "Dedicated pet feeding and care supplies"),

        // Party / Events
        new ConceptRule(List.of("party", "birthday", "celebration", "decor", "guests", "30 people", "15 friends", "event"), "Warm String Lights & Scented Candles", "home-decor", List.of("string lights", "candle", "vase"), "ESSENTIAL", "Festive warm lighting and table centerpiece ambiance"),
        new ConceptRule(List.of("party", "birthday", "tableware", "snacks", "30 people", "15 friends"), "Serving Plates, Mugs & Party Snacks", "kitchen", List.of("plates", "mug", "trail mix", "coffee"), "ESSENTIAL", "Reusable tableware and crowd-friendly refreshments")
    );

    public DeterministicFallbackEngine(PossessionService possessionService) {
        this.possessionService = possessionService;
    }

    @Override
    public String providerName() {
        return "DETERMINISTIC_FALLBACK";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public ParsedIntent parseMission(
        String prompt,
        BigDecimal explicitBudget,
        String timeframe,
        String alreadyOwn,
        List<String> preferences,
        String constraints
    ) {
        String lower = prompt == null ? "" : prompt.toLowerCase();
        BigDecimal extractedBudget = explicitBudget != null && explicitBudget.compareTo(BigDecimal.ZERO) > 0
            ? explicitBudget
            : extractBudget(prompt);

        String extractedTimeframe = (timeframe != null && !timeframe.isBlank()) ? timeframe : extractTimeframe(lower);
        int peopleCount = extractPeopleCount(lower);

        List<String> owned = new ArrayList<>();
        if (alreadyOwn != null && !alreadyOwn.isBlank()) {
            owned.addAll(possessionService.extractPossessionNames(alreadyOwn));
        }
        if (lower.contains("already") || lower.contains("do not recommend") || lower.contains("don't recommend") || lower.contains("exclude")) {
            owned.addAll(possessionService.extractPossessionNames(prompt));
        }
        owned = owned.stream().distinct().toList();

        List<RequirementSpec> matched = new ArrayList<>();
        Set<String> seenLabels = new HashSet<>();

        for (ConceptRule rule : CONCEPT_RULES) {
            boolean hit = rule.triggers().stream().anyMatch(lower::contains);
            if (hit && seenLabels.add(rule.label())) {
                int qty = determineQuantity(rule, peopleCount, lower);
                matched.add(new RequirementSpec(rule.label(), rule.categorySlug(), rule.keywords(), rule.priority(), qty, rule.reason()));
            }
        }

        // Universal catch-all for any arbitrary prompt not covered by specific concept triggers
        if (matched.isEmpty()) {
            List<String> tokens = extractContentKeywords(lower);
            matched.add(new RequirementSpec("Primary Purpose Match", null, tokens, "ESSENTIAL", 1, "Matched directly from your natural-language description"));
            if (tokens.size() > 1) {
                matched.add(new RequirementSpec("Supporting Essentials", null, tokens.subList(1, tokens.size()), "OPTIONAL", 1, "Complementary items matching your secondary keywords"));
            }
        }

        List<String> assumptions = new ArrayList<>();
        assumptions.add("All subtotals, shipping, taxes, and remaining budget are computed from database-stored INR product prices.");
        if (extractedBudget == null) {
            assumptions.add("No explicit budget ceiling was provided; showing best-value options across budget and balanced tiers.");
        }
        if (peopleCount > 1) {
            assumptions.add("Quantities for shared dining/living items scaled for " + peopleCount + " people.");
        }
        assumptions.add("Parsed via local deterministic rule engine (no paid external AI API key required).");

        String title = buildMissionTitle(prompt);
        String followUp = extractedBudget == null ? "Would you like to specify an INR budget ceiling so we can optimize this plan?" : null;

        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("engineMode", "Rule-Based Fallback Engine");
        ctx.put("peopleCount", peopleCount);
        if (constraints != null && !constraints.isBlank()) ctx.put("constraints", constraints);

        return new ParsedIntent(
            title,
            prompt.trim(),
            "Structured plan tailored for: " + title + (peopleCount > 1 ? " (" + peopleCount + " people)" : ""),
            inferPurpose(lower),
            extractedBudget,
            "INR",
            extractedTimeframe,
            peopleCount,
            owned,
            preferences != null ? preferences : List.of(),
            constraints != null && !constraints.isBlank() ? List.of(constraints) : List.of(),
            assumptions,
            matched,
            followUp,
            providerName(),
            ctx
        );
    }

    private BigDecimal extractBudget(String text) {
        if (text == null) return null;
        Matcher kMatcher = Pattern.compile("(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d+)?)\\s*k\\b", Pattern.CASE_INSENSITIVE).matcher(text);
        if (kMatcher.find()) {
            return BigDecimal.valueOf(Double.parseDouble(kMatcher.group(1)) * 1000.0);
        }
        Matcher m = Pattern.compile("(?:₹|rs\\.?|inr|budget|under|within)\\s*[:\\-]?\\s*([0-9][0-9,]{2,7})", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            try { return new BigDecimal(m.group(1).replace(",", "")); } catch (Exception ignored) {}
        }
        return null;
    }

    private String extractTimeframe(String lower) {
        Matcher m = Pattern.compile("(\\d+|one|two|three|four|five|six|seven|ten|first)[-\\s]+(day|days|week|weeks|month|months)", Pattern.CASE_INSENSITIVE).matcher(lower);
        if (m.find()) return m.group(0);
        return "Flexible";
    }

    private int extractPeopleCount(String lower) {
        if (lower.contains("two friends")) return 2;
        if (lower.contains("three people") || lower.contains("three roommates")) return 3;
        Matcher m = Pattern.compile("(\\d+)\\s+(?:friends|people|guests|roommates|members|persons)").matcher(lower);
        if (m.find()) return Math.min(50, Math.max(1, Integer.parseInt(m.group(1))));
        return 1;
    }

    private int determineQuantity(ConceptRule rule, int peopleCount, String lower) {
        if (peopleCount > 1 && ("kitchen".equals(rule.categorySlug()) || "home-decor".equals(rule.categorySlug()))
            && (lower.contains("party") || lower.contains("guests") || lower.contains("three people"))) {
            return Math.min(8, Math.max(1, (int) Math.ceil(peopleCount / 4.0)));
        }
        return 1;
    }

    private List<String> extractContentKeywords(String lower) {
        Set<String> stop = Set.of("i", "am", "im", "i'm", "need", "want", "for", "with", "within", "and", "the", "a", "an", "to", "my", "of", "in", "on", "budget", "setup", "looking", "help", "me", "some", "get", "buy", "suitable", "good", "best", "value", "options", "compare", "different", "recommend", "already", "own", "not");
        return Arrays.stream(lower.replaceAll("[^a-z0-9\\s]", " ").split("\\s+"))
            .filter(w -> w.length() >= 3 && !stop.contains(w))
            .distinct()
            .limit(10)
            .toList();
    }

    private String inferPurpose(String lower) {
        if (lower.contains("bedroom") || lower.contains("house") || lower.contains("hostel") || lower.contains("apartment")) return "Home & Living Setup";
        if (lower.contains("study") || lower.contains("programming") || lower.contains("wfh") || lower.contains("workspace")) return "Work, Study & Tech";
        if (lower.contains("office") || lower.contains("job") || lower.contains("wedding") || lower.contains("fashion")) return "Style & Occasion";
        if (lower.contains("skincare") || lower.contains("skin")) return "Self-Care & Skincare";
        if (lower.contains("travel") || lower.contains("manali") || lower.contains("camping") || lower.contains("trip")) return "Travel & Outdoors";
        if (lower.contains("fitness") || lower.contains("gym") || lower.contains("workout")) return "Fitness & Wellness";
        if (lower.contains("party") || lower.contains("birthday") || lower.contains("gift")) return "Events & Gifting";
        if (lower.contains("kitchen") || lower.contains("grocery") || lower.contains("groceries")) return "Kitchen & Pantry";
        if (lower.contains("pet")) return "Pet Care";
        if (lower.contains("storage")) return "Storage & Organization";
        return "Custom Purpose Plan";
    }

    private String buildMissionTitle(String prompt) {
        String clean = prompt == null ? "Custom Shopping Mission" : prompt.trim();
        if (clean.length() <= 54) return clean;
        return clean.substring(0, 51).trim() + "...";
    }
}
