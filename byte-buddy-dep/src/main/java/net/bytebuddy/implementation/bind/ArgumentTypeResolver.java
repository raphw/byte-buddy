/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;

/**
 * Implementation of an
 * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver}
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
 * <li>If none of the methods is dominant and if the comparison did not result in an ambiguous resolution, the method that
 * consists of the most one-to-one parameter bindings is considered dominant.</li>
 * </ol>
 * Primitive types are considered dominant in the same manner as by the Java compiler.
 * <p>&nbsp;</p>
 * For example: If a source method only parameter was successfully bound one-to-one to the only parameters of the target
 * methods {@code foo(Object)} and {@code bar(String)}, this ambiguity resolver will detect that the {@code String} type
 * is more specific than the {@code Object} type and determine {@code bar(String)} as the dominant binding.
 */
public enum ArgumentTypeResolver implements MethodDelegationBinder.AmbiguityResolver {

    /**
     * The singleton instance.
     */
    INSTANCE;

    /**
     * Resolves two bindings by comparing their binding of similar arguments and determining their most specific types.
     *
     * @param sourceParameterType The parameter type of the source method
     * @param leftParameterIndex  The index of the parameter of the left method.
     * @param left                The left method's parameter binding.
     * @param rightParameterIndex The index of the parameter of the right method.
     * @param right               The right method's parameter binding.
     * @return A resolution according to the given parameters.
     */
    private static Resolution resolveRivalBinding(TypeDescription sourceParameterType,
                                                  int leftParameterIndex,
                                                  MethodDelegationBinder.MethodBinding left,
                                                  int rightParameterIndex,
                                                  MethodDelegationBinder.MethodBinding right) {
        TypeDescription leftParameterType = left.getTarget().getParameters().get(leftParameterIndex).getType().asErasure();
        TypeDescription rightParameterType = right.getTarget().getParameters().get(rightParameterIndex).getType().asErasure();
        if (!leftParameterType.equals(rightParameterType)) {
            if (leftParameterType.isPrimitive() && rightParameterType.isPrimitive()) {
                return PrimitiveTypePrecedence.forPrimitive(leftParameterType).resolve(PrimitiveTypePrecedence.forPrimitive(rightParameterType));
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

    /**
     * Resolves the most specific method by their score. A method's score is calculated by the absolute number of
     * parameters that were bound by using an explicit {@link net.bytebuddy.implementation.bind.annotation.Argument}
     * annotation.
     *
     * @param boundParameterScore The difference of the scores of the left and the right method.
     * @return A resolution according to this score.
     */
    private static Resolution resolveByScore(int boundParameterScore) {
        if (boundParameterScore == 0) {
            return Resolution.AMBIGUOUS;
        } else if (boundParameterScore > 0) {
            return Resolution.LEFT;
        } else /* difference < 0*/ {
            return Resolution.RIGHT;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Resolution resolve(MethodDescription source,
                              MethodDelegationBinder.MethodBinding left,
                              MethodDelegationBinder.MethodBinding right) {
        Resolution resolution = Resolution.UNKNOWN;
        ParameterList<?> sourceParameters = source.getParameters();
        int leftExtra = 0, rightExtra = 0;
        for (int sourceParameterIndex = 0; sourceParameterIndex < sourceParameters.size(); sourceParameterIndex++) {
            ParameterIndexToken parameterIndexToken = new ParameterIndexToken(sourceParameterIndex);
            Integer leftParameterIndex = left.getTargetParameterIndex(parameterIndexToken);
            Integer rightParameterIndex = right.getTargetParameterIndex(parameterIndexToken);
            if (leftParameterIndex != null && rightParameterIndex != null) {
                resolution = resolution.merge(resolveRivalBinding(sourceParameters.get(sourceParameterIndex).getType().asErasure(),
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
        return resolution == Resolution.UNKNOWN
                ? resolveByScore(leftExtra - rightExtra)
                : resolution;
    }

    /**
     * A representation of the precedence of a most specific primitive type in the Java programming language.
     */
    protected enum PrimitiveTypePrecedence {

        /**
         * The precedence of the {@code boolean} type.
         */
        BOOLEAN(0),

        /**
         * The precedence of the {@code byte} type.
         */
        BYTE(1),

        /**
         * The precedence of the {@code short} type.
         */
        SHORT(2),

        /**
         * The precedence of the {@code int} type.
         */
        INTEGER(3),

        /**
         * The precedence of the {@code char} type.
         */
        CHARACTER(4),

        /**
         * The precedence of the {@code long} type.
         */
        LONG(5),

        /**
         * The precedence of the {@code float} type.
         */
        FLOAT(6),

        /**
         * The precedence of the {@code double} type.
         */
        DOUBLE(7);

        /**
         * A score representing the precedence where a higher score represents a less specific type.
         */
        private final int score;

        /**
         * Creates a new primitive type precedence.
         *
         * @param score A score representing the precedence where a higher score represents a less specific type.
         */
        PrimitiveTypePrecedence(int score) {
            this.score = score;
        }

        /**
         * Locates the primitive type precedence for a given type.
         *
         * @param typeDescription The non-void, primitive type for which the precedence should be located.
         * @return The corresponding primitive type precedence.
         */
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

        /**
         * Resolves the least specific type of two primitive type precedence with this instance representing a
         * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver.Resolution#LEFT}
         * resolution and the argument type representing the
         * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver.Resolution#RIGHT}
         * resolution.
         *
         * @param right Another primitive type precedence against which this precedence should be resolved.
         * @return The resolution of
         */
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
     * @see net.bytebuddy.implementation.bind.MethodDelegationBinder.MethodBinding#getTargetParameterIndex(Object)
     */
    public static class ParameterIndexToken {

        /**
         * The parameter index that is represented by this token.
         */
        @SuppressWarnings("unused")
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
        public int hashCode() {
            return parameterIndex;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (other == null || getClass() != other.getClass()) {
                return false;
            }
            ParameterIndexToken parameterIndexToken = (ParameterIndexToken) other;
            return parameterIndex == parameterIndexToken.parameterIndex;
        }
    }
}
