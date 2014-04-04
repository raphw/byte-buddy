package net.bytebuddy.instrumentation.method.bytecode.bind;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;

/**
 * Implementation of an
 * {@link net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.AmbiguityResolver}
 * that resolves two conflicting bindings by considering most-specific types of target method parameters in the same manner
 * as the Java compiler resolves bindings of overloaded method.
 * <p>&nbsp;</p>
 * This ambiguity resolver:
 * <ol>
 * <li>Checks for each parameter of the source method if a one-to-one parameter binding to both of the target methods exist.</li>
 * <li>If any of the source method parameters were bound one-to-one to both target methods, the method with the most specific
 * type is considered as dominant.</li>
 * <li>If this result is dominant for both the left and the right target method, this resolver will consider the binding as
 * ambiguous.</li>
 * <li>If none of the methods is dominant and if the comparison did not result in an ambigous resolution, the method that
 * consists of the most one-to-one parameter bindings is considered dominant.</li>
 * </ol>
 * Primitive types are considered dominant in the same manner as by the Java compiler.
 * <p>&nbsp;</p>
 * For example: If a source method only parameter was successfully bound one-to-one to the only parameters of the target
 * methods {@code foo(Object)} and {@code bar(String)}, this ambiguity resolver will detect that the {@code String} type
 * is more specific than the {@code Object} type and determine {@code bar(String)} as the dominant binding.
 */
public enum MostSpecificTypeResolver implements MethodDelegationBinder.AmbiguityResolver {
    INSTANCE;

    private static Resolution resolveRivalBinding(TypeDescription sourceParameterType,
                                                  int leftParameterIndex,
                                                  MethodDelegationBinder.MethodBinding left,
                                                  int rightParameterIndex,
                                                  MethodDelegationBinder.MethodBinding right) {
        TypeDescription leftParameterType = left.getTarget().getParameterTypes().get(leftParameterIndex);
        TypeDescription rightParameterType = right.getTarget().getParameterTypes().get(rightParameterIndex);
        if (!leftParameterType.equals(rightParameterType)) {
            if (leftParameterType.isPrimitive() && rightParameterType.isPrimitive()) {
                return PrimitiveTypePrecedence.forPrimitive(leftParameterType)
                        .resolve(PrimitiveTypePrecedence.forPrimitive(rightParameterType));
            } else if (leftParameterType.isPrimitive() /* && !rightParameterType.isPrimitive() */) {
                return sourceParameterType.isPrimitive() ? Resolution.LEFT : Resolution.RIGHT;
            } else if (/* !leftParameterType.isPrimitive() && */ rightParameterType.isPrimitive()) {
                return sourceParameterType.isPrimitive() ? Resolution.RIGHT : Resolution.LEFT;
            } else {
                // Note that leftParameterType != rightParameterType, thus both cannot be true.
                if (leftParameterType.isAssignableFrom(rightParameterType)) {
                    return Resolution.RIGHT;
                } else if (rightParameterType.isAssignableFrom(leftParameterType)) {
                    return Resolution.LEFT;
                } else {
                    return Resolution.AMBIGUOUS;
                }
            }
        } else {
            return Resolution.UNKNOWN;
        }
    }

    private static Resolution resolveByScore(int boundParameterScore) {
        if (boundParameterScore == 0) {
            return Resolution.AMBIGUOUS;
        } else if (boundParameterScore > 0) {
            return Resolution.LEFT;
        } else /* difference < 0*/ {
            return Resolution.RIGHT;
        }
    }

    @Override
    public Resolution resolve(MethodDescription source,
                              MethodDelegationBinder.MethodBinding left,
                              MethodDelegationBinder.MethodBinding right) {
        Resolution resolution = Resolution.UNKNOWN;
        int leftExtra = 0, rightExtra = 0;
        for (int sourceParameterIndex = 0;
             sourceParameterIndex < source.getParameterTypes().size();
             sourceParameterIndex++) {
            ParameterIndexToken parameterIndexToken = new ParameterIndexToken(sourceParameterIndex);
            Integer leftParameterIndex = left.getTargetParameterIndex(parameterIndexToken);
            Integer rightParameterIndex = right.getTargetParameterIndex(parameterIndexToken);
            if (leftParameterIndex != null && rightParameterIndex != null) {
                resolution = resolution.merge(
                        resolveRivalBinding(source.getParameterTypes().get(sourceParameterIndex),
                                leftParameterIndex,
                                left,
                                rightParameterIndex,
                                right)
                );
            } else if (leftParameterIndex != null /* && rightParameterIndex == null */) {
                leftExtra++;
            } else if (/*leftParameterIndex == null && */ rightParameterIndex != null) {
                rightExtra++;
            }
        }
        return resolution == Resolution.UNKNOWN ? resolveByScore(leftExtra - rightExtra) : resolution;
    }

    private static enum PrimitiveTypePrecedence {

        BOOLEAN(0),
        BYTE(1),
        SHORT(2),
        INTEGER(3),
        CHARACTER(4),
        LONG(5),
        FLOAT(6),
        DOUBLE(7);
        private final int score;

        private PrimitiveTypePrecedence(int score) {
            this.score = score;
        }

        public static PrimitiveTypePrecedence forPrimitive(TypeDescription typeDescription) {
            if (typeDescription.represents(boolean.class)) {
                return BOOLEAN;
            } else if (typeDescription.represents(byte.class)) {
                return BYTE;
            } else if (typeDescription.represents(short.class)) {
                return SHORT;
            } else if (typeDescription.represents(int.class)) {
                return INTEGER;
            } else if (typeDescription.represents(char.class)) {
                return CHARACTER;
            } else if (typeDescription.represents(long.class)) {
                return LONG;
            } else if (typeDescription.represents(float.class)) {
                return FLOAT;
            } else if (typeDescription.represents(double.class)) {
                return DOUBLE;
            } else {
                throw new IllegalArgumentException("Not a non-void, primitive type " + typeDescription);
            }
        }

        public Resolution resolve(PrimitiveTypePrecedence right) {
            if (score - right.score == 0) {
                return Resolution.UNKNOWN;
            } else if (score - right.score > 0) {
                return Resolution.RIGHT;
            } else /* score - right.score < 0 */ {
                return Resolution.LEFT;
            }
        }
    }

    /**
     * This token is used to mark a one-to-one binding of a source method parameter to a target method parameter.
     *
     * @see net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.MethodBinding#getTargetParameterIndex(Object)
     */
    public static class ParameterIndexToken {

        private final int parameterIndex;

        /**
         * Create a parameter index token for a given parameter of the source method.
         *
         * @param parameterIndex The parameter index of the source method which is mapped to a target method parameter.
         */
        public ParameterIndexToken(int parameterIndex) {
            this.parameterIndex = parameterIndex;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && parameterIndex == ((ParameterIndexToken) other).parameterIndex;
        }

        @Override
        public int hashCode() {
            return parameterIndex;
        }

        @Override
        public String toString() {
            return "MostSpecificTypeResolver.ParameterIndexToken{" + parameterIndex + '}';
        }
    }
}
