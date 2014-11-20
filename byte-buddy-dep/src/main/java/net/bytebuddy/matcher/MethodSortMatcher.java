package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.method.MethodDescription;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class MethodSortMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    public static enum Sort {
        METHOD("isMethod()") {
            @Override
            protected boolean isSort(MethodDescription target) {
                return target.isMethod();
            }
        },

        CONSTRUCTOR("isConstructor()") {
            @Override
            protected boolean isSort(MethodDescription target) {
                return target.isConstructor();
            }
        },

        TYPE_INITIALIZER("isTypeInitializer()") {
            @Override
            protected boolean isSort(MethodDescription target) {
                return target.isTypeInitializer();
            }
        },
        OVERRIDABLE("isOverridable()") {
            @Override
            protected boolean isSort(MethodDescription target) {
                return target.isOverridable();
            }
        },
        VISIBILITY_BRIDGE("isVisibilityBridge()") {
            @Override
            protected boolean isSort(MethodDescription target) {
                return target.isBridge() && target.getDeclaringType()
                        .getDeclaredMethods()
                        .filter(isMethod().and(not(is(target))).and(isCompatibleTo(target)))
                        .size() == 0;
            }
        };

        private final String description;

        private Sort(String description) {
            this.description = description;
        }

        protected abstract boolean isSort(MethodDescription target);

        protected String getDescription() {
            return description;
        }
    }

    private final Sort sort;

    public MethodSortMatcher(Sort sort) {
        this.sort = sort;
    }

    @Override
    public boolean matches(T target) {
        return sort.isSort(target);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && sort == ((MethodSortMatcher) other).sort;
    }

    @Override
    public int hashCode() {
        return sort.hashCode();
    }

    @Override
    public String toString() {
        return sort.getDescription();
    }
}
