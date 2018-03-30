package net.bytebuddy.test.utility;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.mockito.Mockito.mockingDetails;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

public class FieldByFieldComparison<T> extends BaseMatcher<T> {

    private final Object base;

    private FieldByFieldComparison(Object base) {
        this.base = base;
    }

    public static <S> Matcher<S> hasPrototype(S instance) {
        if (instance == null) {
            throw new AssertionError("No instance can be similar to null");
        }
        return new FieldByFieldComparison<S>(instance);
    }

    public static <S> S matchesPrototype(S instance) {
        return argThat(hasPrototype(instance));
    }

    @Override
    public boolean matches(Object other) {
        if (other == null || other.getClass() != base.getClass()) {
            return false;
        } else if (other == base) {
            return true;
        }
        try {
            return matches(base.getClass(), base, other, new HashSet<Pair>());
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static boolean matches(Class<?> type, Object left, Object right, Set<Pair> visited) throws Exception {
        if (!visited.add(new Pair(left, right))) {
            return true;
        } else if (mockingDetails(left).isMock() || mockingDetails(left).isSpy() || mockingDetails(right).isMock() || mockingDetails(right).isSpy()) {
            return left == right;
        }
        while (type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                HashCodeAndEqualsPlugin.ValueHandling valueHandling = field.getAnnotation(HashCodeAndEqualsPlugin.ValueHandling.class);
                if (valueHandling != null && valueHandling.value() == HashCodeAndEqualsPlugin.ValueHandling.Sort.IGNORE) {
                    continue;
                }
                field.setAccessible(true);
                if (field.getType() == boolean.class) {
                    if (field.getBoolean(left) != field.getBoolean(right)) {
                        return false;
                    }
                } else if (field.getType() == byte.class) {
                    if (field.getBoolean(left) != field.getBoolean(right)) {
                        return false;
                    }
                } else if (field.getType() == short.class) {
                    if (field.getShort(left) != field.getShort(right)) {
                        return false;
                    }
                } else if (field.getType() == char.class) {
                    if (field.getChar(left) != field.getChar(right)) {
                        return false;
                    }
                } else if (field.getType() == int.class) {
                    if (field.getInt(left) != field.getInt(right)) {
                        return false;
                    }
                } else if (field.getType() == long.class) {
                    if (field.getLong(left) != field.getLong(right)) {
                        return false;
                    }
                } else if (field.getType() == float.class) {
                    if (field.getFloat(left) != field.getFloat(right)) {
                        return false;
                    }
                } else if (field.getType() == double.class) {
                    if (field.getDouble(left) != field.getDouble(right)) {
                        return false;
                    }
                } else if (field.getType().isEnum()) {
                    if (field.get(left) != field.get(right)) {
                        return false;
                    }
                } else {
                    Object leftObject = field.get(left), rightObject = field.get(right);
                    if (mockingDetails(leftObject).isMock() || mockingDetails(rightObject).isSpy() || mockingDetails(rightObject).isMock() || mockingDetails(rightObject).isSpy()) {
                        if (leftObject != rightObject) {
                            return false;
                        }
                    } else if (Iterable.class.isAssignableFrom(field.getType())) {
                        if (rightObject == null) {
                            return false;
                        }
                        Iterator<?> rightIterable = ((Iterable<?>) rightObject).iterator();
                        for (Object instance : (Iterable<?>) leftObject) {
                            if (!rightIterable.hasNext() || !matches(instance.getClass(), instance, rightIterable.next(), visited)) {
                                return false;
                            }
                        }
                    } else if (field.getType().getName().startsWith("net.bytebuddy.")) {
                        if (leftObject == null ? rightObject != null : !matches(leftObject.getClass(), leftObject, rightObject, visited)) {
                            return false;
                        }
                    } else {
                        if (leftObject == null ? rightObject != null : !leftObject.equals(rightObject)) {
                            return false;
                        }
                    }
                }
            }
            type = type.getSuperclass();
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("an object similar to " + base);
    }

    private static class Pair {

        private final Object left, right;

        private Pair(Object left, Object right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (other == null || getClass() != other.getClass()) {
                return false;
            }
            Pair pair = (Pair) other;
            return (left != null ? left.equals(pair.left) : pair.left == null) && (right != null ? right.equals(pair.right) : pair.right == null);
        }

        @Override
        public int hashCode() {
            int result = left != null ? left.hashCode() : 0;
            result = 31 * result + (right != null ? right.hashCode() : 0);
            return result;
        }
    }
}
