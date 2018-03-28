package net.bytebuddy.matcher;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;

/**
 * An element matcher that compares two strings by a given pattern which is characterized by a
 * {@link net.bytebuddy.matcher.StringMatcher.Mode}.
 */
@HashCodeAndEqualsPlugin.Enhance
public class StringMatcher extends ElementMatcher.Junction.AbstractBase<String> {

    /**
     * The text value to match against.
     */
    private final String value;

    /**
     * The mode to apply for matching the given value against the matcher's input.
     */
    private final Mode mode;

    /**
     * Creates a new string matcher.
     *
     * @param value The value that is the base of the matching.
     * @param mode  The mode to apply for matching the given value against the matcher's input
     */
    public StringMatcher(String value, Mode mode) {
        this.value = value;
        this.mode = mode;
    }

    @Override
    public boolean matches(String target) {
        return mode.matches(value, target);
    }

    @Override
    public String toString() {
        return mode.getDescription() + '(' + value + ')';
    }

    /**
     * Defines the mode a {@link net.bytebuddy.matcher.StringMatcher} compares to strings with.
     */
    public enum Mode {

        /**
         * Checks if two strings equal and respects casing differences.
         */
        EQUALS_FULLY("equals") {
            @Override
            protected boolean matches(String expected, String actual) {
                return actual.equals(expected);
            }
        },

        /**
         * Checks if two strings equal without respecting casing differences.
         */
        EQUALS_FULLY_IGNORE_CASE("equalsIgnoreCase") {
            @Override
            protected boolean matches(String expected, String actual) {
                return actual.equalsIgnoreCase(expected);
            }
        },

        /**
         * Checks if a string starts with the a second string with respecting casing differences.
         */
        STARTS_WITH("startsWith") {
            @Override
            protected boolean matches(String expected, String actual) {
                return actual.startsWith(expected);
            }
        },

        /**
         * Checks if a string starts with a second string without respecting casing differences.
         */
        STARTS_WITH_IGNORE_CASE("startsWithIgnoreCase") {
            @Override
            @SuppressFBWarnings(value = "DM_CONVERT_CASE", justification = "Both strings are transformed by the default locale")
            protected boolean matches(String expected, String actual) {
                return actual.toLowerCase().startsWith(expected.toLowerCase());
            }
        },

        /**
         * Checks if a string ends with a second string with respecting casing differences.
         */
        ENDS_WITH("endsWith") {
            @Override
            protected boolean matches(String expected, String actual) {
                return actual.endsWith(expected);
            }
        },

        /**
         * Checks if a string ends with a second string without respecting casing differences.
         */
        ENDS_WITH_IGNORE_CASE("endsWithIgnoreCase") {
            @Override
            @SuppressFBWarnings(value = "DM_CONVERT_CASE", justification = "Both strings are transformed by the default locale")
            protected boolean matches(String expected, String actual) {
                return actual.toLowerCase().endsWith(expected.toLowerCase());
            }
        },

        /**
         * Checks if a string contains another string with respecting casing differences.
         */
        CONTAINS("contains") {
            @Override
            protected boolean matches(String expected, String actual) {
                return actual.contains(expected);
            }
        },

        /**
         * Checks if a string contains another string without respecting casing differences.
         */
        CONTAINS_IGNORE_CASE("containsIgnoreCase") {
            @Override
            @SuppressFBWarnings(value = "DM_CONVERT_CASE", justification = "Both strings are transformed by the default locale")
            protected boolean matches(String expected, String actual) {
                return actual.toLowerCase().contains(expected.toLowerCase());
            }
        },

        /**
         * Checks if a string can be matched by a regular expression.
         */
        MATCHES("matches") {
            @Override
            protected boolean matches(String expected, String actual) {
                return actual.matches(expected);
            }
        };

        /**
         * A description of the string for providing meaningful {@link Object#toString()} implementations for
         * method matchers that rely on a match mode.
         */
        private final String description;

        /**
         * Creates a new match mode.
         *
         * @param description The description of this mode for providing meaningful {@link Object#toString()}
         *                    implementations.
         */
        Mode(String description) {
            this.description = description;
        }

        /**
         * Returns the description of this match mode.
         *
         * @return The description of this match mode.
         */
        protected String getDescription() {
            return description;
        }

        /**
         * Matches a string against another string.
         *
         * @param expected The target of the comparison against which the {@code actual} string is compared.
         * @param actual   The source which is subject of the comparison to the {@code expected} value.
         * @return {@code true} if the source matches the target.
         */
        protected abstract boolean matches(String expected, String actual);
    }
}
