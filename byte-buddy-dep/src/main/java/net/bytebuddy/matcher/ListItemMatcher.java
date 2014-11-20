package net.bytebuddy.matcher;

import java.util.List;

public class ListItemMatcher<T> extends ElementMatcher.Junction.AbstractBase<List<? extends T>> {

    public static enum Mode {

        MATCH_ANY(true, "any"),

        MATCH_NONE(false, "none");

        private final boolean match;

        private final String description;

        private Mode(boolean match, String description) {
            this.match = match;
            this.description = description;
        }

        protected boolean isMatch() {
            return match;
        }

        protected String getDescription() {
            return description;
        }
    }

    private final ElementMatcher<? super T> elementMatcher;

    private final Mode mode;

    public ListItemMatcher(ElementMatcher<? super T> elementMatcher, Mode mode) {
        this.elementMatcher = elementMatcher;
        this.mode = mode;
    }

    @Override
    public boolean matches(List<? extends T> target) {
        for (T value : target) {
            if (elementMatcher.matches(value)) {
                return mode.isMatch();
            }
        }
        return !mode.isMatch();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && elementMatcher.equals(((ListItemMatcher) other).elementMatcher)
                && mode == ((ListItemMatcher) other).mode;
    }

    @Override
    public int hashCode() {
        int result = elementMatcher.hashCode();
        result = 31 * result + mode.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return mode.getDescription() + "(" + elementMatcher + ")";
    }
}
