package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

public enum MostSpecificTypeResolver implements MethodDelegationBinder.AmbiguityResolver {
    INSTANCE;

    public static class ParameterIndexToken {

        private final int parameterIndex;

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
            return "ParameterIndexToken{" + parameterIndex + '}';
        }
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

        private final int score;

        private PrimitiveTypePrecedence(int score) {
            this.score = score;
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

    @Override
    public Resolution resolve(MethodDescription source,
                              MethodDelegationBinder.Binding left,
                              MethodDelegationBinder.Binding right) {
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
                                right));
            } else if (leftParameterIndex != null /* && rightParameterIndex == null */) {
                leftExtra++;
            } else if (/*leftParameterIndex == null && */ rightParameterIndex != null) {
                rightExtra++;
            }
        }
        return resolution == Resolution.UNKNOWN ? resolveByScore(leftExtra - rightExtra) : resolution;
    }

    private static Resolution resolveRivalBinding(TypeDescription sourceParameterType,
                                                  int leftParameterIndex,
                                                  MethodDelegationBinder.Binding left,
                                                  int rightParameterIndex,
                                                  MethodDelegationBinder.Binding right) {
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
}
