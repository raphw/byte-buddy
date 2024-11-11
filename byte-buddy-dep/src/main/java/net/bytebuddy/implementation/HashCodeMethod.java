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
package net.bytebuddy.implementation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.*;
import net.bytebuddy.implementation.bytecode.constant.ClassConstant;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.matcher.ElementMatcher;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * An implementation of {@link Object#hashCode()} that takes a class's declared fields into consideration. A hash code is computed by transforming
 * primitive field types to an {@code int} value and by summing those values up starting from a given offset after multiplying any previous value
 * with a multiplier. Reference values are checked against {@code null} values unless specified otherwise.
 */
@HashCodeAndEqualsPlugin.Enhance
public class HashCodeMethod implements Implementation {

    /**
     * The default offset which should be a prime number.
     */
    private static final int DEFAULT_OFFSET = 17;

    /**
     * The default multiplier for each value before adding a field's hash code value which should be a prime number.
     */
    private static final int DEFAULT_MULTIPLIER = 31;

    /**
     * The {@link Object#hashCode()} method.
     */
    private static final MethodDescription.InDefinedShape HASH_CODE = TypeDescription.ForLoadedType.of(Object.class)
            .getDeclaredMethods()
            .filter(isHashCode())
            .getOnly();

    /**
     * The {@link Object#getClass()} method.
     */
    private static final MethodDescription.InDefinedShape GET_CLASS = TypeDescription.ForLoadedType.of(Object.class)
            .getDeclaredMethods()
            .filter(named("getClass").and(takesArguments(0)))
            .getOnly();

    /**
     * The hash code's offset provider.
     */
    private final OffsetProvider offsetProvider;

    /**
     * A multiplier for each value before adding a field's hash code value.
     */
    private final int multiplier;

    /**
     * A matcher to filter fields that should not be used for a hash codes computation.
     */
    private final ElementMatcher.Junction<? super FieldDescription.InDefinedShape> ignored;

    /**
     * A matcher to determine fields of a reference type that cannot be {@code null}.
     */
    private final ElementMatcher.Junction<? super FieldDescription.InDefinedShape> nonNullable;

    /**
     * A matcher to determine that a field should be considered by its identity.
     */
    private final ElementMatcher.Junction<? super FieldDescription.InDefinedShape> identity;

    /**
     * Creates a new hash code method implementation.
     *
     * @param offsetProvider The hash code's offset provider.
     */
    protected HashCodeMethod(OffsetProvider offsetProvider) {
        this(offsetProvider, DEFAULT_MULTIPLIER, none(), none(), none());
    }

    /**
     * Creates a new hash code method implementation.
     *
     * @param offsetProvider The hash code's offset provider.
     * @param multiplier     A multiplier for each value before adding a field's hash code value
     * @param ignored        A matcher to filter fields that should not be used for a hash codes computation.
     * @param nonNullable    A matcher to determine fields of a reference type that cannot be {@code null}.
     * @param identity       A matcher to determine that a field should be considered by its identity.
     */
    private HashCodeMethod(OffsetProvider offsetProvider,
                           int multiplier,
                           ElementMatcher.Junction<? super FieldDescription.InDefinedShape> ignored,
                           ElementMatcher.Junction<? super FieldDescription.InDefinedShape> nonNullable,
                           ElementMatcher.Junction<? super FieldDescription.InDefinedShape> identity) {
        this.offsetProvider = offsetProvider;
        this.multiplier = multiplier;
        this.ignored = ignored;
        this.nonNullable = nonNullable;
        this.identity = identity;
    }

    /**
     * Creates a hash code method implementation that bases the hash code on the instrumented type's super class's hash code value.
     *
     * @return A hash code method implementation that bases the hash code on the instrumented type's super class's hash code value.
     */
    public static HashCodeMethod usingSuperClassOffset() {
        return new HashCodeMethod(OffsetProvider.ForSuperMethodCall.INSTANCE);
    }

    /**
     * Creates a hash code method implementation that bases the hash code on the instrumented type's class constant's hash code..
     *
     * @param dynamic {@code true} if the type should be resolved from the instance and not be set as the declaring class.
     * @return A hash code method implementation that bases the hash code on the instrumented type's class constant's hash code.
     */
    public static HashCodeMethod usingTypeHashOffset(boolean dynamic) {
        return new HashCodeMethod(dynamic ? OffsetProvider.ForDynamicTypeHash.INSTANCE : OffsetProvider.ForStaticTypeHash.INSTANCE);
    }

    /**
     * Creates a hash code method implementation that bases the hash code on a fixed value.
     *
     * @return A hash code method implementation that bases the hash code on a fixed value.
     */
    public static HashCodeMethod usingDefaultOffset() {
        return usingOffset(DEFAULT_OFFSET);
    }

    /**
     * Creates a hash code method implementation that bases the hash code on a fixed value.
     *
     * @param value The fixed value.
     * @return A hash code method implementation that bases the hash code on a fixed value.
     */
    public static HashCodeMethod usingOffset(int value) {
        return new HashCodeMethod(new OffsetProvider.ForFixedValue(value));
    }

    /**
     * Returns a new version of this hash code method implementation that ignores the specified fields additionally to any
     * previously specified fields.
     *
     * @param ignored A matcher to specify any fields that should be ignored.
     * @return A new version of this hash code method implementation that also ignores any fields matched by the provided matcher.
     */
    public HashCodeMethod withIgnoredFields(ElementMatcher<? super FieldDescription.InDefinedShape> ignored) {
        return new HashCodeMethod(offsetProvider, multiplier, this.ignored.<FieldDescription.InDefinedShape>or(ignored), nonNullable, identity);
    }

    /**
     * Returns a new version of this hash code method implementation that does not apply a {@code null} value check for the specified fields
     * if they have a reference type additionally to any previously specified fields.
     *
     * @param nonNullable A matcher to specify any fields that should not be guarded against {@code null} values.
     * @return A new version of this hash code method implementation that also does not apply {@code null} value checks to any fields matched by
     * the provided matcher.
     */
    public HashCodeMethod withNonNullableFields(ElementMatcher<? super FieldDescription.InDefinedShape> nonNullable) {
        return new HashCodeMethod(offsetProvider, multiplier, ignored, this.nonNullable.<FieldDescription.InDefinedShape>or(nonNullable), identity);
    }

    /**
     * Returns a new version of this hash code method implementation that considers the matched fields by their identity.
     *
     * @param identity A matcher to specify any fields that should be considered by their identity.
     * @return A new version of this hash code method implementation that also considers the matched fields by their identity.
     */
    public HashCodeMethod withIdentityFields(ElementMatcher<? super FieldDescription.InDefinedShape> identity) {
        return new HashCodeMethod(offsetProvider, multiplier, ignored, nonNullable, this.identity.<FieldDescription.InDefinedShape>or(identity));
    }

    /**
     * Returns a new version of this hash code method implementation that uses the given multiplier onto any given hash code before adding a
     * field's hash code.
     *
     * @param multiplier The multiplier to use for any hash code before adding any field's hash code.
     * @return A new version of this hash code method implementation that uses the given multiplier onto any given hash code before adding a
     * field's hash code.
     */
    public Implementation withMultiplier(int multiplier) {
        if (multiplier == 0) {
            throw new IllegalArgumentException("Hash code multiplier must not be zero");
        }
        return new HashCodeMethod(offsetProvider, multiplier, ignored, nonNullable, identity);
    }

    /**
     * {@inheritDoc}
     */
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
    }

    /**
     * {@inheritDoc}
     */
    public ByteCodeAppender appender(Target implementationTarget) {
        if (implementationTarget.getInstrumentedType().isInterface()) {
            throw new IllegalStateException("Cannot implement meaningful hash code method for " + implementationTarget.getInstrumentedType());
        }
        return new Appender(offsetProvider.resolve(implementationTarget.getInstrumentedType()),
                multiplier,
                implementationTarget.getInstrumentedType().getDeclaredFields().filter(not(isStatic().or(ignored))),
                nonNullable,
                identity);
    }

    /**
     * An offset provider is responsible for supplying the initial hash code.
     */
    protected interface OffsetProvider {

        /**
         * Resolves this offset provider for a given instrumented type.
         *
         * @param instrumentedType The instrumented type.
         * @return A stack manipulation that loads the initial hash code onto the operand stack.
         */
        StackManipulation resolve(TypeDescription instrumentedType);

        /**
         * An offset provider that supplies a fixed value.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForFixedValue implements OffsetProvider {

            /**
             * The value to load onto the operand stack.
             */
            private final int value;

            /**
             * Creates a new offset provider for a fixed value.
             *
             * @param value The value to load onto the operand stack.
             */
            protected ForFixedValue(int value) {
                this.value = value;
            }

            /**
             * {@inheritDoc}
             */
            public StackManipulation resolve(TypeDescription instrumentedType) {
                return IntegerConstant.forValue(value);
            }
        }

        /**
         * An offset provider that invokes the super class's {@link Object#hashCode()} implementation.
         */
        enum ForSuperMethodCall implements OffsetProvider {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public StackManipulation resolve(TypeDescription instrumentedType) {
                TypeDefinition superClass = instrumentedType.getSuperClass();
                if (superClass == null) {
                    throw new IllegalStateException(instrumentedType + " does not declare a super class");
                }
                return new StackManipulation.Compound(MethodVariableAccess.loadThis(), MethodInvocation.invoke(HASH_CODE).special(superClass.asErasure()));
            }
        }

        /**
         * An offset provider that uses the instrumented type's class constant's hash code.
         */
        enum ForStaticTypeHash implements OffsetProvider {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public StackManipulation resolve(TypeDescription instrumentedType) {
                return new StackManipulation.Compound(ClassConstant.of(instrumentedType), MethodInvocation.invoke(HASH_CODE).virtual(TypeDescription.ForLoadedType.of(Class.class)));
            }
        }

        /**
         * An offset provider that uses the instance's class's hash code.
         */
        enum ForDynamicTypeHash implements OffsetProvider {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public StackManipulation resolve(TypeDescription instrumentedType) {
                return new StackManipulation.Compound(MethodVariableAccess.loadThis(),
                        MethodInvocation.invoke(GET_CLASS).virtual(instrumentedType),
                        MethodInvocation.invoke(HASH_CODE).virtual(TypeDescription.ForLoadedType.of(Class.class)));
            }
        }
    }

    /**
     * A guard against {@code null} values for fields with reference types.
     */
    protected interface NullValueGuard {

        /**
         * Returns a stack manipulation to apply before computing a hash value.
         *
         * @return A stack manipulation to apply before computing a hash value.
         */
        StackManipulation before();

        /**
         * Returns a stack manipulation to apply after computing a hash value.
         *
         * @return A stack manipulation to apply after computing a hash value.
         */
        StackManipulation after();

        /**
         * Returns the required padding for the local variable array to apply this guard.
         *
         * @return The required padding for the local variable array to apply this guard.
         */
        int getRequiredVariablePadding();

        /**
         * A non-operational null value guard.
         */
        enum NoOp implements NullValueGuard {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public StackManipulation before() {
                return StackManipulation.Trivial.INSTANCE;
            }

            /**
             * {@inheritDoc}
             */
            public StackManipulation after() {
                return StackManipulation.Trivial.INSTANCE;
            }

            /**
             * {@inheritDoc}
             */
            public int getRequiredVariablePadding() {
                return StackSize.ZERO.getSize();
            }
        }

        /**
         * A null value guard that expects a reference type and that uses a jump if a field value is {@code null}.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class UsingJump implements NullValueGuard {

            /**
             * The instrumented method.
             */
            private final MethodDescription instrumentedMethod;

            /**
             * A label to indicate the target of a jump.
             */
            private final Label label;

            /**
             * Creates a new null value guard using a jump instruction for {@code null} values.
             *
             * @param instrumentedMethod The instrumented method.
             */
            protected UsingJump(MethodDescription instrumentedMethod) {
                this.instrumentedMethod = instrumentedMethod;
                label = new Label();
            }

            /**
             * {@inheritDoc}
             */
            public StackManipulation before() {
                return new BeforeInstruction();
            }

            /**
             * {@inheritDoc}
             */
            public StackManipulation after() {
                return new AfterInstruction();
            }

            /**
             * {@inheritDoc}
             */
            public int getRequiredVariablePadding() {
                return 1;
            }

            /**
             * The stack manipulation to apply before the hash value computation.
             */
            @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
            protected class BeforeInstruction extends StackManipulation.AbstractBase {

                /**
                 * {@inheritDoc}
                 */
                public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                    methodVisitor.visitVarInsn(Opcodes.ASTORE, instrumentedMethod.getStackSize());
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, instrumentedMethod.getStackSize());
                    methodVisitor.visitJumpInsn(Opcodes.IFNULL, label);
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, instrumentedMethod.getStackSize());
                    return Size.ZERO;
                }
            }

            /**
             * The stack manipulation to apply after the hash value computation.
             */
            @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
            protected class AfterInstruction extends StackManipulation.AbstractBase {

                /**
                 * {@inheritDoc}
                 */
                public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                    methodVisitor.visitLabel(label);
                    implementationContext.getFrameGeneration().same1(methodVisitor,
                            TypeDescription.ForLoadedType.of(int.class),
                            Arrays.asList(implementationContext.getInstrumentedType(), TypeDescription.ForLoadedType.of(Object.class)));
                    return Size.ZERO;
                }
            }
        }
    }

    /**
     * A value transformer that is responsible for resolving a field value to an {@code int} value.
     */
    protected enum ValueTransformer implements StackManipulation {

        /**
         * A transformer for a {@code long} value.
         */
        LONG {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitInsn(Opcodes.DUP2);
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 32);
                methodVisitor.visitInsn(Opcodes.LUSHR);
                methodVisitor.visitInsn(Opcodes.LXOR);
                methodVisitor.visitInsn(Opcodes.L2I);
                return new Size(-1, 3);
            }
        },

        /**
         * A transformer for a {@code float} value.
         */
        FLOAT {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "floatToIntBits", "(F)I", false);
                return Size.ZERO;
            }
        },

        /**
         * A transformer for a {@code double} value.
         */
        DOUBLE {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "doubleToLongBits", "(D)J", false);
                methodVisitor.visitInsn(Opcodes.DUP2);
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 32);
                methodVisitor.visitInsn(Opcodes.LUSHR);
                methodVisitor.visitInsn(Opcodes.LXOR);
                methodVisitor.visitInsn(Opcodes.L2I);
                return new Size(-1, 3);
            }
        },

        /**
         * A transformer for a {@code boolean[]} value.
         */
        BOOLEAN_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "hashCode", "([Z)I", false);
                return Size.ZERO;
            }
        },

        /**
         * A transformer for a {@code byte[]} value.
         */
        BYTE_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "hashCode", "([B)I", false);
                return Size.ZERO;
            }
        },

        /**
         * A transformer for a {@code short[]} value.
         */
        SHORT_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "hashCode", "([S)I", false);
                return Size.ZERO;
            }
        },

        /**
         * A transformer for a {@code char[]} value.
         */
        CHARACTER_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "hashCode", "([C)I", false);
                return Size.ZERO;
            }
        },

        /**
         * A transformer for an {@code int[]} value.
         */
        INTEGER_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "hashCode", "([I)I", false);
                return Size.ZERO;
            }
        },

        /**
         * A transformer for a {@code long[]} value.
         */
        LONG_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "hashCode", "([J)I", false);
                return Size.ZERO;
            }
        },

        /**
         * A transformer for a {@code float[]} value.
         */
        FLOAT_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "hashCode", "([F)I", false);
                return Size.ZERO;
            }
        },

        /**
         * A transformer for a {@code double[]} value.
         */
        DOUBLE_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "hashCode", "([D)I", false);
                return Size.ZERO;
            }
        },

        /**
         * A transformer for a reference array value.
         */
        REFERENCE_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "hashCode", "([Ljava/lang/Object;)I", false);
                return Size.ZERO;
            }
        },

        /**
         * A transformer for a nested reference array value.
         */
        NESTED_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "deepHashCode", "([Ljava/lang/Object;)I", false);
                return Size.ZERO;
            }
        },

        /**
         * A transformer for computing the identity hash code for a reference.
         */
        REFERENCE_IDENTITY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I", false);
                return Size.ZERO;
            }
        };

        /**
         * Resolves a type definition to a hash code.
         *
         * @param typeDefinition The type definition to resolve.
         * @return The stack manipulation to apply.
         */
        @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming component type for array type.")
        public static StackManipulation of(TypeDefinition typeDefinition) {
            if (typeDefinition.represents(boolean.class)
                    || typeDefinition.represents(byte.class)
                    || typeDefinition.represents(short.class)
                    || typeDefinition.represents(char.class)
                    || typeDefinition.represents(int.class)) {
                return Trivial.INSTANCE;
            } else if (typeDefinition.represents(long.class)) {
                return LONG;
            } else if (typeDefinition.represents(float.class)) {
                return FLOAT;
            } else if (typeDefinition.represents(double.class)) {
                return DOUBLE;
            } else if (typeDefinition.represents(boolean[].class)) {
                return BOOLEAN_ARRAY;
            } else if (typeDefinition.represents(byte[].class)) {
                return BYTE_ARRAY;
            } else if (typeDefinition.represents(short[].class)) {
                return SHORT_ARRAY;
            } else if (typeDefinition.represents(char[].class)) {
                return CHARACTER_ARRAY;
            } else if (typeDefinition.represents(int[].class)) {
                return INTEGER_ARRAY;
            } else if (typeDefinition.represents(long[].class)) {
                return LONG_ARRAY;
            } else if (typeDefinition.represents(float[].class)) {
                return FLOAT_ARRAY;
            } else if (typeDefinition.represents(double[].class)) {
                return DOUBLE_ARRAY;
            } else if (typeDefinition.isArray()) {
                return typeDefinition.getComponentType().isArray()
                        ? NESTED_ARRAY
                        : REFERENCE_ARRAY;
            } else {
                return MethodInvocation.invoke(HASH_CODE).virtual(typeDefinition.asErasure());
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean isValid() {
            return true;
        }
    }

    /**
     * A byte code appender to implement a hash code method.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class Appender implements ByteCodeAppender {

        /**
         * Loads the initial hash code onto the operand stack.
         */
        private final StackManipulation initialValue;

        /**
         * A multiplier for each value before adding a field's hash code value.
         */
        private final int multiplier;

        /**
         * A list of fields to include in the hash code computation.
         */
        private final List<FieldDescription.InDefinedShape> fieldDescriptions;

        /**
         * A matcher to determine fields of a reference type that cannot be {@code null}.
         */
        private final ElementMatcher<? super FieldDescription.InDefinedShape> nonNullable;

        /**
         * A matcher to determine that a field should be considered by its identity.
         */
        private final ElementMatcher<? super FieldDescription.InDefinedShape> identity;

        /**
         * Creates a new appender for implementing a hash code method.
         *
         * @param initialValue      Loads the initial hash code onto the operand stack.
         * @param multiplier        A multiplier for each value before adding a field's hash code value.
         * @param fieldDescriptions A list of fields to include in the hash code computation.
         * @param nonNullable       A matcher to determine fields of a reference type that cannot be {@code null}.
         * @param identity          A matcher to determine that a field should be considered by its identity.
         */
        protected Appender(StackManipulation initialValue,
                           int multiplier,
                           List<FieldDescription.InDefinedShape> fieldDescriptions,
                           ElementMatcher<? super FieldDescription.InDefinedShape> nonNullable,
                           ElementMatcher<? super FieldDescription.InDefinedShape> identity) {
            this.initialValue = initialValue;
            this.multiplier = multiplier;
            this.fieldDescriptions = fieldDescriptions;
            this.nonNullable = nonNullable;
            this.identity = identity;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            if (instrumentedMethod.isStatic()) {
                throw new IllegalStateException("Hash code method must not be static: " + instrumentedMethod);
            } else if (!instrumentedMethod.getReturnType().represents(int.class)) {
                throw new IllegalStateException("Hash code method does not return primitive integer: " + instrumentedMethod);
            }
            List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(2 + fieldDescriptions.size() * 8);
            stackManipulations.add(initialValue);
            int padding = 0;
            for (FieldDescription.InDefinedShape fieldDescription : fieldDescriptions) {
                stackManipulations.add(IntegerConstant.forValue(multiplier));
                stackManipulations.add(Multiplication.INTEGER);
                stackManipulations.add(MethodVariableAccess.loadThis());
                stackManipulations.add(FieldAccess.forField(fieldDescription).read());
                if (!fieldDescription.getType().isPrimitive() && identity.matches(fieldDescription)) {
                    stackManipulations.add(ValueTransformer.REFERENCE_IDENTITY);
                    stackManipulations.add(Addition.INTEGER);
                } else {
                    NullValueGuard nullValueGuard = fieldDescription.getType().isPrimitive() || fieldDescription.getType().isArray() || nonNullable.matches(fieldDescription)
                            ? NullValueGuard.NoOp.INSTANCE
                            : new NullValueGuard.UsingJump(instrumentedMethod);
                    stackManipulations.add(nullValueGuard.before());
                    stackManipulations.add(ValueTransformer.of(fieldDescription.getType()));
                    stackManipulations.add(Addition.INTEGER);
                    stackManipulations.add(nullValueGuard.after());
                    padding = Math.max(padding, nullValueGuard.getRequiredVariablePadding());
                }
            }
            stackManipulations.add(MethodReturn.INTEGER);
            return new Size(new StackManipulation.Compound(stackManipulations).apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize() + padding);
        }
    }
}
