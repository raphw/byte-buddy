package net.bytebuddy.matcher;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;

/**
 * A matcher that remembers the results of previously matching an equal target.
 *
 * @param <T> The actual matched type of this matcher.
 */
public class CachingMatcher<T> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The underlying matcher to apply for non-cached targets.
     */
    private final ElementMatcher<? super T> matcher;

    /**
     * A map that serves as a cache for previous matches.
     */
    protected final ConcurrentMap<? super T, Boolean> map;

    /**
     * Creates a new caching matcher.
     *
     * @param matcher The underlying matcher to apply for non-cached targets.
     * @param map     A map that serves as a cache for previous matches. This match is strongly referenced and
     *                can cause a memory leak if it is not evicted while keeping this matcher alive.
     */
    public CachingMatcher(ElementMatcher<? super T> matcher, ConcurrentMap<? super T, Boolean> map) {
        this.matcher = matcher;
        this.map = map;
    }

    @Override
    public boolean matches(T target) {
        Boolean cached = map.get(target);
        if (cached == null) {
            cached = onCacheMiss(target);
        }
        return cached;
    }

    /**
     * Invoked if the cache is not hit.
     *
     * @param target The element to be matched.
     * @return {@code true} if the element is matched.
     */
    protected boolean onCacheMiss(T target) {
        boolean cached = matcher.matches(target);
        map.put(target, cached);
        return cached;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof CachingMatcher)) return false;
        CachingMatcher<?> that = (CachingMatcher<?>) object;
        return matcher.equals(that.matcher);
    }

    @Override
    public int hashCode() {
        return matcher.hashCode();
    }

    @Override
    public String toString() {
        return "cached(" + matcher + ")";
    }

    /**
     * A caching matcher with inline cache eviction.
     *
     * @param <S> The actual matched type of this matcher.
     */
    @SuppressFBWarnings(value = "EQ_DOESNT_OVERRIDE_EQUALS", justification = "Caching mechanism is not supposed to decide on equality")
    public static class WithInlineEviction<S> extends CachingMatcher<S> {

        /**
         * The maximum amount of entries in this map before removing a random entry from the map.
         */
        private final int evictionSize;

        /**
         * Creates a new caching matcher with inlined cache eviction.
         *
         * @param matcher      The underlying matcher to apply for non-cached targets.
         * @param map          A map that serves as a cache for previous matches. This match is strongly referenced and
         *                     can cause a memory leak if it is not evicted while keeping this matcher alive.
         * @param evictionSize The maximum amount of entries in this map before removing a random entry from the map.
         */
        public WithInlineEviction(ElementMatcher<? super S> matcher, ConcurrentMap<? super S, Boolean> map, int evictionSize) {
            super(matcher, map);
            this.evictionSize = evictionSize;
        }

        @Override
        protected boolean onCacheMiss(S target) {
            if (map.size() >= evictionSize) {
                Iterator<?> iterator = map.entrySet().iterator();
                iterator.next();
                iterator.remove();
            }
            return super.onCacheMiss(target);
        }
    }
}

