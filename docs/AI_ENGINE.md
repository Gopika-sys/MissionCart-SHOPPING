# MissionCart AI & Fallback Engine

## Architecture
- `AIProvider` interface with `GeminiAIProvider`, `OpenAIProvider`, and `DeterministicFallbackEngine`
- `MissionIntentParser` -> `RequirementExtractor` -> `ProductRetrievalService` -> `MissionBundleBuilder` -> `MissionExplanationService`

## Zero-Hallucination Guarantee
1. AI providers only output structured intent (`requirements`, `budget`, `existingPossessions`, `assumptions`).
2. `ProductRetrievalService` resolves products strictly from the MySQL/JPA database.
3. Unmatched requirements are disclosed honestly instead of padding with random items.
4. Without `GEMINI_API_KEY` or `OPENAI_API_KEY`, `DeterministicFallbackEngine` handles arbitrary situations via multi-category trigger rules + general token matching + INR budget extraction (`₹`, `Rs`, `k`).
