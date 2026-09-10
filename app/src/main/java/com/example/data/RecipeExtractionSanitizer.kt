package com.example.data

/**
 * Strips blog narratives, advertisements, affiliate marketing disclosures, 
 * web tracking parameters, and social media calls-to-action to produce 
 * pristine, distilled culinary data (Title, Times, Ingredients, and Instructions).
 */
object RecipeExtractionSanitizer {

    private val AD_AND_TRACKING_PATTERNS = listOf(
        Regex("(?i)https?://\\S+utm_[\\w=&%-]+"),
        Regex("(?i)https?://\\S+fbclid=[\\w-]+"),
        Regex("(?i)https?://\\S+gclid=[\\w-]+"),
        Regex("(?i)https?://amzn\\.to/\\S+"),
        Regex("(?i)https?://\\S+affiliate\\S*"),
        Regex("(?i)<script[\\s\\S]*?</script>"),
        Regex("(?i)<style[\\s\\S]*?</style>"),
        Regex("(?i)<iframe[\\s\\S]*?</iframe>"),
        Regex("(?i)<!--[\\s\\S]*?-->"),
        Regex("(?i)<[^>]+>") // Strip remaining HTML tags
    )

    private val BLOG_FILLER_PHRASES = listOf(
        Regex("(?i)^.*jump to recipe.*$"),
        Regex("(?i)^.*skip to recipe.*$"),
        Regex("(?i)^.*print this recipe.*$"),
        Regex("(?i)^.*pin this for later.*$"),
        Regex("(?i)^.*save to pinterest.*$"),
        Regex("(?i)^.*don'?t forget to subscribe.*$"),
        Regex("(?i)^.*like and follow for more.*$"),
        Regex("(?i)^.*as an amazon associate I earn.*$"),
        Regex("(?i)^.*this post contains affiliate links.*$"),
        Regex("(?i)^.*sponsored by.*$"),
        Regex("(?i)^.*brought to you by.*$"),
        Regex("(?i)^.*growing up, my grandmother always.*$"),
        Regex("(?i)^.*the crisp autumn air always reminds me of.*$"),
        Regex("(?i)^.*every sunday morning in our household.*$"),
        Regex("(?i)^.*leave a 5-star rating below.*$"),
        Regex("(?i)^.*check out my other recipes at.*$"),
        Regex("(?i)^.*cookie preferences.*accept all.*$")
    )

    /**
     * Sanitizes a raw title by stripping clickbait tags, promotional emojis, and website branding.
     */
    fun sanitizeTitle(rawTitle: String): String {
        var clean = rawTitle
            .replace(Regex("(?i)\\b(BEST EVER|MUST TRY|VIRAL|TIKTOK|THE ONLY|SECRET|GUARANTEED)\\b"), "")
            .replace(Regex("(?i)\\|.*$"), "") // Remove "| Food Blog Name"
            .replace(Regex("(?i)-.*(kitchen|recipe|blog|eats).*$"), "")
            .replace(Regex("[✨🔥💥❤️🍳👩‍🍳👇📌]"), "")
            .trim()
            .replace(Regex("\\s+"), " ")

        if (clean.isBlank()) clean = "Heirloom Recipe"
        return clean
    }

    /**
     * Sanitizes a URL by stripping tracking parameters, affiliate queries, and fragments.
     */
    fun sanitizeUrl(rawUrl: String): String {
        var clean = rawUrl.trim()
        clean = clean.split("?utm_")[0]
        clean = clean.split("&utm_")[0]
        clean = clean.split("?fbclid=")[0]
        clean = clean.split("&fbclid=")[0]
        clean = clean.split("?gclid=")[0]
        clean = clean.split("&gclid=")[0]
        return clean
    }

    /**
     * Cleans an ingredient line, removing affiliate tags and blog asterisks.
     */
    fun sanitizeIngredient(rawLine: String): String {
        var clean = rawLine.trim()
        AD_AND_TRACKING_PATTERNS.forEach { pattern ->
            clean = pattern.replace(clean, "")
        }
        clean = clean
            .replace(Regex("^[-*•·\\d]+\\.\\s*"), "") // Leading bullets/numbers
            .replace(Regex("(?i)\\(affiliate link\\)"), "")
            .replace(Regex("(?i)\\(see note\\)"), "")
            .replace(Regex("(?i)\\(see blog post for brand\\)"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return clean
    }

    /**
     * Filters a list of raw ingredient lines, dropping empty lines and blog filler lines.
     */
    fun sanitizeIngredientsList(rawLines: List<String>): List<String> {
        return rawLines
            .map { sanitizeIngredient(it) }
            .filter { line ->
                line.isNotBlank() &&
                BLOG_FILLER_PHRASES.none { it.matches(line) } &&
                !line.startsWith("http", ignoreCase = true) &&
                line.length in 3..160
            }
    }

    /**
     * Cleans an instruction step, removing social media callouts and ads.
     */
    fun sanitizeInstructionStep(rawStep: String): String {
        var clean = rawStep.trim()
        AD_AND_TRACKING_PATTERNS.forEach { pattern ->
            clean = pattern.replace(clean, "")
        }
        clean = clean
            .replace(Regex("^Step\\s*\\d+[:.]?\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^\\d+[.)]\\s*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return clean
    }

    /**
     * Filters a list of raw steps into concise, ad-free cooking instructions.
     */
    fun sanitizeInstructionsList(rawSteps: List<String>): List<String> {
        return rawSteps
            .map { sanitizeInstructionStep(it) }
            .filter { step ->
                step.isNotBlank() &&
                BLOG_FILLER_PHRASES.none { it.matches(step) } &&
                !step.startsWith("http", ignoreCase = true) &&
                step.length >= 8
            }
    }
}
