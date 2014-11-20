package net.bytebuddy.matcher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ListOneToOneMatcher<T> extends ElementMatcher.Junction.AbstractBase<List<? extends T>> {

    private final List<? extends ElementMatcher<? super T>> elementMatchers;

    protected ListOneToOneMatcher(List<? extends ElementMatcher<? super T>> elementMatchers) {
        this.elementMatchers = new ArrayList<ElementMatcher<? super T>>(elementMatchers);
    }

    @Override
    public boolean matches(List<? extends T> target) {
        if (target.size() != elementMatchers.size()) {
            return false;
        }
        Iterator<? extends ElementMatcher<? super T>> iterator = elementMatchers.iterator();
        for (T value : target) {
            if (!iterator.next().matches(value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && elementMatchers.equals(((ListOneToOneMatcher) other).elementMatchers);
    }

    @Override
    public int hashCode() {
        return elementMatchers.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("ofTypes(");
        boolean first = true;
        for (Object value : elementMatchers) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append(", ");
            }
            stringBuilder.append(value);
        }
        return stringBuilder.append(")").toString();
    }
}
