package com.mycompany.util;

/**
 * Utility class for safe database query construction.
 * Prevents LIKE wildcard abuse (%, _, \) that can cause full table scans (DoS).
 */
public class QueryUtils {

    private QueryUtils() {
        // Utility class - no instantiation
    }

    /**
     * Escapes special LIKE wildcards in a search keyword.
     * Prevents attackers from sending "%%%%%" to trigger expensive full table
     * scans.
     *
     * Uses '!' as the escape character to avoid backslash ambiguity across JPQL/SQL
     * dialects.
     * Characters escaped: % _ !
     *
     * @param keyword raw user input
     * @return escaped keyword safe for use in LIKE queries with ESCAPE '!'
     */
    public static String escapeLikeKeyword(String keyword) {
        if (keyword == null)
            return null;
        return keyword
                .replace("!", "!!") // must be escaped first
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
