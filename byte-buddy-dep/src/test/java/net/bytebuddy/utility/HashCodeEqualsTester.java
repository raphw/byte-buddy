package net.bytebuddy.utility;

import org.objectweb.asm.Opcodes;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class HashCodeEqualsTester {

    private static final boolean DEFAULT_BOOLEAN = false, OTHER_BOOLEAN = true;

    private static final byte DEFAULT_BYTE = 1, OTHER_BYTE = 42;

    private static final char DEFAULT_CHAR = 1, OTHER_CHAR = 42;

    private static final short DEFAULT_SHORT = 1, OTHER_SHORT = 42;

    private static final int DEFAULT_INT = 1, OTHER_INT = 42;

    private static final long DEFAULT_LONG = 1, OTHER_LONG = 42;

    private static final float DEFAULT_FLOAT = 1, OTHER_FLOAT = 42;

    private static final double DEFAULT_DOUBLE = 1, OTHER_DOUBLE = 42;

    private static final String DEFAULT_STRING = "foo", OTHER_STRING = "bar";

    private final Class<?> type;

    private final Refinement refinement;

    private final boolean skipSynthetic;

    private HashCodeEqualsTester(Class<?> type, Refinement refinement, boolean skipSynthetic) {
        this.type = type;
        this.refinement = refinement;
        this.skipSynthetic = skipSynthetic;
    }

    public static HashCodeEqualsTester of(Class<?> type) {
        return new HashCodeEqualsTester(type, Refinement.NoOp.INSTANCE, false);
    }

    public HashCodeEqualsTester refine(Refinement refinement) {
        return new HashCodeEqualsTester(type, new Refinement.Pair(this.refinement, refinement), false);
    }

    public HashCodeEqualsTester skipSynthetic() {
        return new HashCodeEqualsTester(type, refinement, true);
    }

    public void apply() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.isSynthetic() && skipSynthetic) {
                continue;
            }
            constructor.setAccessible(true);
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] actualArguments = new Object[parameterTypes.length];
            Object[] otherArguments = new Object[parameterTypes.length];
            int index = 0;
            for (Class<?> parameterType : parameterTypes) {
                putInstance(parameterType, actualArguments, otherArguments, index++);
            }
            int testIndex = 0;
            Object instance = constructor.newInstance(actualArguments);
            assertThat(instance, is(instance));
            assertThat(instance, not(is((Object) null)));
            assertThat(instance, not(is(new Object())));
            Object similarInstance = constructor.newInstance(actualArguments);
            assertThat(instance.hashCode(), is(similarInstance.hashCode()));
            assertThat(instance, is(similarInstance));
            for (Object otherArgument : otherArguments) {
                Object[] compareArguments = new Object[actualArguments.length];
                int argumentIndex = 0;
                for (Object actualArgument : actualArguments) {
                    if (argumentIndex == testIndex) {
                        compareArguments[argumentIndex] = otherArgument;
                    } else {
                        compareArguments[argumentIndex] = actualArgument;
                    }
                    argumentIndex++;
                }
                Object unlikeInstance = constructor.newInstance(compareArguments);
                assertThat(instance.hashCode(), not(is(unlikeInstance)));
                assertThat(instance, not(is(unlikeInstance)));
                testIndex++;
            }
        }
    }

    private void putInstance(Class<?> parameterType, Object actualArguments, Object otherArguments, int index) {
        Object actualArgument, otherArgument;
        if (parameterType == boolean.class) {
            actualArgument = DEFAULT_BOOLEAN;
            otherArgument = OTHER_BOOLEAN;
        } else if (parameterType == byte.class) {
            actualArgument = DEFAULT_BYTE;
            otherArgument = OTHER_BYTE;
        } else if (parameterType == char.class) {
            actualArgument = DEFAULT_CHAR;
            otherArgument = OTHER_CHAR;
        } else if (parameterType == short.class) {
            actualArgument = DEFAULT_SHORT;
            otherArgument = OTHER_SHORT;
        } else if (parameterType == int.class) {
            actualArgument = DEFAULT_INT;
            otherArgument = OTHER_INT;
        } else if (parameterType == long.class) {
            actualArgument = DEFAULT_LONG;
            otherArgument = OTHER_LONG;
        } else if (parameterType == float.class) {
            actualArgument = DEFAULT_FLOAT;
            otherArgument = OTHER_FLOAT;
        } else if (parameterType == double.class) {
            actualArgument = DEFAULT_DOUBLE;
            otherArgument = OTHER_DOUBLE;
        } else if (parameterType == String.class) {
            actualArgument = DEFAULT_STRING;
            otherArgument = OTHER_STRING;
        } else if (parameterType.isEnum()) {
            Object[] enumConstants = parameterType.getEnumConstants();
            if (enumConstants.length == 1) {
                throw new IllegalArgumentException("Enum with only one constant: " + parameterType);
            }
            actualArgument = enumConstants[0];
            otherArgument = enumConstants[1];
        } else if (List.class.isAssignableFrom(parameterType)) {
            actualArgument = Array.newInstance(Object.class, 1);
            otherArgument = Array.newInstance(Object.class, 1);
            putInstance(Object.class, actualArgument, otherArgument, 0);
            actualArgument = Arrays.asList((Object[]) actualArgument);
            otherArgument = Arrays.asList((Object[]) otherArgument);
        } else if (parameterType.isArray()) {
            actualArgument = Array.newInstance(parameterType.getComponentType(), 1);
            otherArgument = Array.newInstance(parameterType.getComponentType(), 1);
            putInstance(parameterType.getComponentType(), actualArgument, otherArgument, 0);
        } else if ((parameterType.getModifiers() & Opcodes.ACC_FINAL) != 0) {
            throw new IllegalArgumentException("Cannot mock final type " + parameterType);
        } else {
            actualArgument = mock(parameterType);
            refinement.apply(actualArgument);
            otherArgument = mock(parameterType);
            refinement.apply(otherArgument);
        }
        Array.set(actualArguments, index, actualArgument);
        Array.set(otherArguments, index, otherArgument);
    }

    public static interface Refinement {

        void apply(Object mock);

        static enum NoOp implements Refinement {

            INSTANCE;

            @Override
            public void apply(Object mock) {
                /* do nothing */
            }
        }

        static class Pair implements Refinement {

            private final Refinement first, second;

            public Pair(Refinement first, Refinement second) {
                this.first = first;
                this.second = second;
            }

            @Override
            public void apply(Object mock) {
                first.apply(mock);
                second.apply(mock);
            }
        }
    }
}
