package net.bytebuddy.matcher;

/**
 * An element matcher that compares two strings by a given pattern which is characterized by a
 * {@link net.bytebuddy.matcher.StringMatcher.Mode}.
 */
public class StringMatcher extends ElementMatcher.Junction.AbstractBase<String> {

    /**
     * Defines the mode a {@link net.bytebuddy.matcher.StringMatcher} compares to strings with.
     */
    public static enum Mode {

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
        private Mode(String description) {
            this.description = description;
        }

        /**
         * Returns the description of this match mode.
         *
         * @return The description of this match mode.
         */
        private String getDescription() {
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

    /**
     * The name to match the method's name against.
     */
    private final String name;

    /**
     * The mode of matching the given name.
     */
    private final Mode mode;

    /**
     * Creates a new method name matcher.
     *
     * @param name The name to be matched.
     * @param mode The mode of matching the given name.
     */
    public StringMatcher(String name, Mode mode) {
        this.name = name;
        this.mode = mode;
    }

    @Override
    public boolean matches(String target) {
        return mode.matches(name, target);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && mode == ((StringMatcher) other).mode
                && name.equals(((StringMatcher) other).name);
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + mode.hashCode();
    }

    @Override
    public String toString() {
        return mode.getDescription() + '(' + name + ')';
    }
}
