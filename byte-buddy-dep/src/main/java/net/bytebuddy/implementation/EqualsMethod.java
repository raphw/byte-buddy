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
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.assign.InstanceCheck;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.matcher.ElementMatcher;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * An implementation of {@link Object#equals(Object)} that takes a class's declared fields into consideration. Equality is resolved by comparing two
 * instances of the same or a compatible class field by field where reference fields must either both be {@code null} or where the field value of
 * the instance upon which the method is invoked returns {@code true} upon calling the value's {@code equals} method. For arrays, the corresponding
 * utilities of {@link java.util.Arrays} are used.
 */
@HashCodeAndEqualsPlugin.Enhance
public class EqualsMethod implements Implementation {

    /**
     * The {@link Object#equals(Object)} method.
     */
    private static final MethodDescription.InDefinedShape EQUALS = TypeDescription.OBJECT
            .getDeclaredMethods()
            .filter(isEquals())
            .getOnly();

    /**
     * The baseline equality to check.
     */
    private final SuperClassCheck superClassCheck;

    /**
     * The instance type compatibility check.
     */
    private final TypeCompatibilityCheck typeCompatibilityCheck;

    /**
     * A matcher to filter fields that should not be used for a equality resolution.
     */
    private final ElementMatcher.Junction<? super FieldDescription.InDefinedShape> ignored;

    /**
     * A matcher to determine fields of a reference type that cannot be {@code null}.
     */
    private final ElementMatcher.Junction<? super FieldDescription.InDefinedShape> nonNullable;

    /**
     * The comparator to apply for ordering fields.
     */
    private final Comparator<? super FieldDescription.InDefinedShape> comparator;

    /**
     * Creates a new equals method implementation.
     *
     * @param superClassCheck The baseline equality to check.
     */
    protected EqualsMethod(SuperClassCheck superClassCheck) {
        this(superClassCheck, TypeCompatibilityCheck.EXACT, none(), none(), NaturalOrderComparator.INSTANCE);
    }

    /**
     * Creates a new equals method implementation.
     *
     * @param superClassCheck        The baseline equality to check.
     * @param typeCompatibilityCheck The instance type compatibility check.
     * @param ignored                A matcher to filter fields that should not be used for a equality resolution.
     * @param nonNullable            A matcher to determine fields of a reference type that cannot be {@code null}.
     * @param comparator             The comparator to apply for ordering fields.
     */
    private EqualsMethod(SuperClassCheck superClassCheck,
                         TypeCompatibilityCheck typeCompatibilityCheck,
                         ElementMatcher.Junction<? super FieldDescription.InDefinedShape> ignored,
                         ElementMatcher.Junction<? super FieldDescription.InDefinedShape> nonNullable,
                         Comparator<? super FieldDescription.InDefinedShape> comparator) {
        this.superClassCheck = superClassCheck;
        this.typeCompatibilityCheck = typeCompatibilityCheck;
        this.ignored = ignored;
        this.nonNullable = nonNullable;
        this.comparator = comparator;
    }

    /**
     * Creates an equals implementation that invokes the super class's {@link Object#equals(Object)} method first.
     *
     * @return An equals implementation that invokes the super class's {@link Object#equals(Object)} method first.
     */
    public static EqualsMethod requiringSuperClassEquality() {
        return new EqualsMethod(SuperClassCheck.ENABLED);
    }

    /**
     * Creates an equals method implementation that does not invoke the super class's {@link Object#equals(Object)} method.
     *
     * @return An equals method implementation that does not invoke the super class's {@link Object#equals(Object)} method.
     */
    public static EqualsMethod isolated() {
        return new EqualsMethod(SuperClassCheck.DISABLED);
    }

    /**
     * Returns a new version of this equals method implementation that ignores the specified fields additionally to any
     * previously specified fields.
     *
     * @param ignored A matcher to specify any fields that should be ignored.
     * @return A new version of this equals method implementation that also ignores any fields matched by the provided matcher.
     */
    public EqualsMethod withIgnoredFields(ElementMatcher<? super FieldDescription.InDefinedShape> ignored) {
        return new EqualsMethod(superClassCheck, typeCompatibilityCheck, this.ignored.<FieldDescription.InDefinedShape>or(ignored), nonNullable, comparator);
    }

    /**
     * Returns a new version of this equals method implementation that does not apply a {@code null} value check for the specified fields
     * if they have a reference type additionally to any previously specified fields.
     *
     * @param nonNullable A matcher to specify any fields that should not be guarded against {@code null} values.
     * @return A new version of this equals method implementation that also does not apply {@code null} value checks to any fields matched by
     * the provided matcher.
     */
    public EqualsMethod withNonNullableFields(ElementMatcher<? super FieldDescription.InDefinedShape> nonNullable) {
        return new EqualsMethod(superClassCheck, typeCompatibilityCheck, ignored, this.nonNullable.<FieldDescription.InDefinedShape>or(nonNullable), comparator);
    }

    /**
     * Returns a new version of this equals method that compares fields with primitive types prior to fields with non-primitive types.
     *
     * @return A new version of this equals method that compares primitive-typed fields before fields with non-primitive-typed fields.
     */
    public EqualsMethod withPrimitiveTypedFieldsFirst() {
        return withFieldOrder(TypePropertyComparator.FOR_PRIMITIVE_TYPES);
    }

    /**
     * Returns a new version of this equals method that compares fields with enumeration types prior to fields with non-enumeration types.
     *
     * @return A new version of this equals method that compares enumeration-typed fields before fields with non-enumeration-typed fields.
     */
    public EqualsMethod withEnumerationTypedFieldsFirst() {
        return withFieldOrder(TypePropertyComparator.FOR_ENUMERATION_TYPES);
    }

    /**
     * Returns a new version of this equals method that compares fields with primitive wrapper types prior to fields with non-primitive wrapper types.
     *
     * @return A new version of this equals method that compares primitive wrapper-typed fields before fields with non-primitive wrapper-typed fields.
     */
    public EqualsMethod withPrimitiveWrapperTypedFieldsFirst() {
        return withFieldOrder(TypePropertyComparator.FOR_PRIMITIVE_WRAPPER_TYPES);
    }

    /**
     * Returns a new version of this equals method that compares fields with {@link String} types prior to fields with non-{@link String} types.
     *
     * @return A new version of this equals method that compares {@link String}-typed fields before fields with non-{@link String}-typed fields.
     */
    public EqualsMethod withStringTypedFieldsFirst() {
        return withFieldOrder(TypePropertyComparator.FOR_STRING_TYPES);
    }

    /**
     * Applies the supplied comparator to determine an order for fields for being compared. Fields with the lowest sort order are compared
     * first. Any previously defined comparators are applied prior to the supplied comparator.
     *
     * @param comparator The comparator to apply.
     * @return A new version of this equals method that sorts fields in their application order using the supplied comparator.
     */
    @SuppressWarnings("unchecked") // In absence of @SafeVarargs
    public EqualsMethod withFieldOrder(Comparator<? super FieldDescription.InDefinedShape> comparator) {
        return new EqualsMethod(superClassCheck, typeCompatibilityCheck, ignored, nonNullable, new CompoundComparator(this.comparator, comparator));
    }

    /**
     * Returns a new version of this equals method implementation that permits subclasses of the instrumented type to be equal to instances
     * of the instrumented type instead of requiring an exact match.
     *
     * @return A new version of this equals method implementation that permits subclasses of the instrumented type to be equal to instances
     * of the instrumented type instead of requiring an exact match.
     */
    public Implementation withSubclassEquality() {
        return new EqualsMethod(superClassCheck, TypeCompatibilityCheck.SUBCLASS, ignored, nonNullable, comparator);
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
            throw new IllegalStateException("Cannot implement meaningful equals method for " + implementationTarget.getInstrumentedType());
        }
        List<FieldDescription.InDefinedShape> fields = new ArrayList<FieldDescription.InDefinedShape>(implementationTarget.getInstrumentedType()
                .getDeclaredFields()
                .filter(not(isStatic().or(ignored))));
        Collections.sort(fields, comparator);
        return new Appender(implementationTarget.getInstrumentedType(), new StackManipulation.Compound(
                superClassCheck.resolve(implementationTarget.getInstrumentedType()),
                MethodVariableAccess.loadThis(),
                MethodVariableAccess.REFERENCE.loadFrom(1),
                ConditionalReturn.onIdentity().returningTrue(),
                typeCompatibilityCheck.resolve(implementationTarget.getInstrumentedType())
        ), fields, nonNullable);
    }

    /**
     * Checks the equality contract against the super class.
     */
    protected enum SuperClassCheck {

        /**
         * Does not perform any super class check.
         */
        DISABLED {
            @Override
            protected StackManipulation resolve(TypeDescription instrumentedType) {
                return StackManipulation.Trivial.INSTANCE;
            }
        },

        /**
         * Invokes the super class's {@link Object#equals(Object)} method.
         */
        ENABLED {
            @Override
            protected StackManipulation resolve(TypeDescription instrumentedType) {
                TypeDefinition superClass = instrumentedType.getSuperClass();
                if (superClass == null) {
                    throw new IllegalStateException(instrumentedType + " does not declare a super class");
                }
                return new StackManipulation.Compound(MethodVariableAccess.loadThis(),
                        MethodVariableAccess.REFERENCE.loadFrom(1),
                        MethodInvocation.invoke(EQUALS).special(superClass.asErasure()),
                        ConditionalReturn.onZeroInteger());
            }
        };

        /**
         * Resolves a stack manipulation for the required super class check.
         *
         * @param instrumentedType The instrumented type.
         * @return A stack manipulation that implements the specified check.
         */
        protected abstract StackManipulation resolve(TypeDescription instrumentedType);
    }

    /**
     * Checks the overall type of the provided argument.
     */
    protected enum TypeCompatibilityCheck {

        /**
         * Requires an exact type match.
         */
        EXACT {
            @Override
            public StackManipulation resolve(TypeDescription instrumentedType) {
                return new StackManipulation.Compound(
                        MethodVariableAccess.REFERENCE.loadFrom(1),
                        ConditionalReturn.onNullValue(),
                        MethodVariableAccess.REFERENCE.loadFrom(0),
                        MethodInvocation.invoke(GET_CLASS),
                        MethodVariableAccess.REFERENCE.loadFrom(1),
                        MethodInvocation.invoke(GET_CLASS),
                        ConditionalReturn.onNonIdentity()
                );
            }
        },

        /**
         * Requires a subtype relationship.
         */
        SUBCLASS {
            @Override
            protected StackManipulation resolve(TypeDescription instrumentedType) {
                return new StackManipulation.Compound(
                        MethodVariableAccess.REFERENCE.loadFrom(1),
                        InstanceCheck.of(instrumentedType),
                        ConditionalReturn.onZeroInteger()
                );
            }
        };

        /**
         * The {@link Object#getClass()} method.
         */
        protected static final MethodDescription.InDefinedShape GET_CLASS = TypeDescription.ForLoadedType.of(Object.class)
                .getDeclaredMethods()
                .filter(named("getClass"))
                .getOnly();

        /**
         * Resolves a stack manipulation for the required type compatibility check.
         *
         * @param instrumentedType The instrumented type.
         * @return A stack manipulation that implements the specified check.
         */
        protected abstract StackManipulation resolve(TypeDescription instrumentedType);
    }

    /**
     * Guards a field value against a potential {@code null} value.
     */
    protected interface NullValueGuard {

        /**
         * Returns a stack manipulation to apply before computing equality.
         *
         * @return A stack manipulation to apply before computing equality.
         */
        StackManipulation before();

        /**
         * Returns a stack manipulation to apply after computing equality.
         *
         * @return A stack manipulation to apply after computing equality.
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
         * A null value guard that expects a reference type and that skips the comparison if both values are {@code null} but returns if
         * the invoked instance's field value is {@code null} but not the compared instance's value.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class UsingJump implements NullValueGuard {

            /**
             * An empty array.
             */
            private static final Object[] EMPTY = new Object[0];

            /**
             * An array containing a single reference value.
             */
            private static final Object[] REFERENCE = new Object[]{Type.getInternalName(Object.class)};

            /**
             * The instrumented method.
             */
            private final MethodDescription instrumentedMethod;

            /**
             * The label to jump to if the first value is {@code null} whereas the second value is not {@code null}.
             */
            private final Label firstValueNull;

            /**
             * The label to jump to if the second value is {@code null}.
             */
            private final Label secondValueNull;

            /**
             * A label indicating the end of the null-guarding block.
             */
            private final Label endOfBlock;

            /**
             * Creates a new null value guard using a jump instruction for {@code null} values.
             *
             * @param instrumentedMethod The instrumented method.
             */
            protected UsingJump(MethodDescription instrumentedMethod) {
                this.instrumentedMethod = instrumentedMethod;
                firstValueNull = new Label();
                secondValueNull = new Label();
                endOfBlock = new Label();
            }

            /**
             * {@inheritDoc}
             */
            public StackManipulation before() {
                return new UsingJump.BeforeInstruction();
            }

            /**
             * {@inheritDoc}
             */
            public StackManipulation after() {
                return new UsingJump.AfterInstruction();
            }

            /**
             * {@inheritDoc}
             */
            public int getRequiredVariablePadding() {
                return 2;
            }

            /**
             * The stack manipulation to apply before the equality computation.
             */
            @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
            protected class BeforeInstruction implements StackManipulation {

                /**
                 * {@inheritDoc}
                 */
                public boolean isValid() {
                    return true;
                }

                /**
                 * {@inheritDoc}
                 */
                public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                    methodVisitor.visitVarInsn(Opcodes.ASTORE, instrumentedMethod.getStackSize());
                    methodVisitor.visitVarInsn(Opcodes.ASTORE, instrumentedMethod.getStackSize() + 1);
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, instrumentedMethod.getStackSize() + 1);
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, instrumentedMethod.getStackSize());
                    methodVisitor.visitJumpInsn(Opcodes.IFNULL, secondValueNull);
                    methodVisitor.visitJumpInsn(Opcodes.IFNULL, firstValueNull);
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, instrumentedMethod.getStackSize() + 1);
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, instrumentedMethod.getStackSize());
                    return new Size(0, 0);
                }
            }

            /**
             * The stack manipulation to apply after the equality computation.
             */
            @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
            protected class AfterInstruction implements StackManipulation {

                /**
                 * {@inheritDoc}
                 */
                public boolean isValid() {
                    return true;
                }

                /**
                 * {@inheritDoc}
                 */
                public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                    methodVisitor.visitJumpInsn(Opcodes.GOTO, endOfBlock);
                    methodVisitor.visitLabel(secondValueNull);
                    if (implementationContext.getClassFileVersion().isAtLeast(ClassFileVersion.JAVA_V6)) {
                        methodVisitor.visitFrame(Opcodes.F_SAME1, EMPTY.length, EMPTY, REFERENCE.length, REFERENCE);
                    }
                    methodVisitor.visitJumpInsn(Opcodes.IFNULL, endOfBlock);
                    methodVisitor.visitLabel(firstValueNull);
                    if (implementationContext.getClassFileVersion().isAtLeast(ClassFileVersion.JAVA_V6)) {
                        methodVisitor.visitFrame(Opcodes.F_SAME, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
                    }
                    methodVisitor.visitInsn(Opcodes.ICONST_0);
                    methodVisitor.visitInsn(Opcodes.IRETURN);
                    methodVisitor.visitLabel(endOfBlock);
                    if (implementationContext.getClassFileVersion().isAtLeast(ClassFileVersion.JAVA_V6)) {
                        methodVisitor.visitFrame(Opcodes.F_SAME, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
                    }
                    return new Size(0, 0);
                }
            }
        }
    }

    /**
     * A value comparator is responsible to compare to values of a given type.
     */
    protected enum ValueComparator implements StackManipulation {

        /**
         * A comparator for a {@code long} value.
         */
        LONG {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitInsn(Opcodes.LCMP);
                return new Size(-2, 0);
            }
        },

        /**
         * A comparator for a {@code float} value.
         */
        FLOAT {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "compare", "(FF)I", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A comparator for a {@code double} value.
         */
        DOUBLE {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "compare", "(DD)I", false);
                return new Size(-2, 0);
            }
        },

        /**
         * A comparator for a {@code boolean[]} value.
         */
        BOOLEAN_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([Z[Z)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A comparator for a {@code byte[]} value.
         */
        BYTE_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([B[B)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A comparator for a {@code short[]} value.
         */
        SHORT_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([S[S)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A comparator for a {@code char[]} value.
         */
        CHARACTER_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([C[C)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A comparator for an {@code int[]} value.
         */
        INTEGER_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([I[I)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A comparator for a {@code long[]} value.
         */
        LONG_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([J[J)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A comparator for a {@code float[]} value.
         */
        FLOAT_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([F[F)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A transformer for a {@code double[]} value.
         */
        DOUBLE_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([D[D)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A transformer for a reference array value.
         */
        REFERENCE_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([Ljava/lang/Object;[Ljava/lang/Object;)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A transformer for a nested reference array value.
         */
        NESTED_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "deepEquals", "([Ljava/lang/Object;[Ljava/lang/Object;)Z", false);
                return new Size(-1, 0);
            }
        };

        /**
         * Resolves a type definition to a equality comparison.
         *
         * @param typeDefinition The type definition to resolve.
         * @return The stack manipulation to apply.
         */
        public static StackManipulation of(TypeDefinition typeDefinition) {
            if (typeDefinition.represents(boolean.class)
                    || typeDefinition.represents(byte.class)
                    || typeDefinition.represents(short.class)
                    || typeDefinition.represents(char.class)
                    || typeDefinition.represents(int.class)) {
                return ConditionalReturn.onNonEqualInteger();
            } else if (typeDefinition.represents(long.class)) {
                return new Compound(LONG, ConditionalReturn.onNonZeroInteger());
            } else if (typeDefinition.represents(float.class)) {
                return new Compound(FLOAT, ConditionalReturn.onNonZeroInteger());
            } else if (typeDefinition.represents(double.class)) {
                return new Compound(DOUBLE, ConditionalReturn.onNonZeroInteger());
            } else if (typeDefinition.represents(boolean[].class)) {
                return new Compound(BOOLEAN_ARRAY, ConditionalReturn.onZeroInteger());
            } else if (typeDefinition.represents(byte[].class)) {
                return new Compound(BYTE_ARRAY, ConditionalReturn.onZeroInteger());
            } else if (typeDefinition.represents(short[].class)) {
                return new Compound(SHORT_ARRAY, ConditionalReturn.onZeroInteger());
            } else if (typeDefinition.represents(char[].class)) {
                return new Compound(CHARACTER_ARRAY, ConditionalReturn.onZeroInteger());
            } else if (typeDefinition.represents(int[].class)) {
                return new Compound(INTEGER_ARRAY, ConditionalReturn.onZeroInteger());
            } else if (typeDefinition.represents(long[].class)) {
                return new Compound(LONG_ARRAY, ConditionalReturn.onZeroInteger());
            } else if (typeDefinition.represents(float[].class)) {
                return new Compound(FLOAT_ARRAY, ConditionalReturn.onZeroInteger());
            } else if (typeDefinition.represents(double[].class)) {
                return new Compound(DOUBLE_ARRAY, ConditionalReturn.onZeroInteger());
            } else if (typeDefinition.isArray()) {
                return new Compound(typeDefinition.getComponentType().isArray()
                        ? NESTED_ARRAY
                        : REFERENCE_ARRAY, ConditionalReturn.onZeroInteger());
            } else {
                return new Compound(MethodInvocation.invoke(EQUALS).virtual(typeDefinition.asErasure()), ConditionalReturn.onZeroInteger());
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
     * A byte code appender to implement the {@link EqualsMethod}.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class Appender implements ByteCodeAppender {

        /**
         * The instrumented type.
         */
        private final TypeDescription instrumentedType;

        /**
         * The baseline stack manipulation.
         */
        private final StackManipulation baseline;

        /**
         * A list of fields to use for the comparison.
         */
        private final List<FieldDescription.InDefinedShape> fieldDescriptions;

        /**
         * A matcher to determine fields of a reference type that cannot be {@code null}.
         */
        private final ElementMatcher<? super FieldDescription.InDefinedShape> nonNullable;

        /**
         * Creates a new appender.
         *
         * @param instrumentedType  The instrumented type.
         * @param baseline          The baseline stack manipulation.
         * @param fieldDescriptions A list of fields to use for the comparison.
         * @param nonNullable       A matcher to determine fields of a reference type that cannot be {@code null}.
         */
        protected Appender(TypeDescription instrumentedType,
                           StackManipulation baseline,
                           List<FieldDescription.InDefinedShape> fieldDescriptions,
                           ElementMatcher<? super FieldDescription.InDefinedShape> nonNullable) {
            this.instrumentedType = instrumentedType;
            this.baseline = baseline;
            this.fieldDescriptions = fieldDescriptions;
            this.nonNullable = nonNullable;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            if (instrumentedMethod.isStatic()) {
                throw new IllegalStateException("Hash code method must not be static: " + instrumentedMethod);
            } else if (instrumentedMethod.getParameters().size() != 1 || instrumentedMethod.getParameters().getOnly().getType().isPrimitive()) {
                throw new IllegalStateException();
            } else if (!instrumentedMethod.getReturnType().represents(boolean.class)) {
                throw new IllegalStateException("Hash code method does not return primitive boolean: " + instrumentedMethod);
            }
            List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(3 + fieldDescriptions.size() * 8);
            stackManipulations.add(baseline);
            int padding = 0;
            for (FieldDescription.InDefinedShape fieldDescription : fieldDescriptions) {
                stackManipulations.add(MethodVariableAccess.loadThis());
                stackManipulations.add(FieldAccess.forField(fieldDescription).read());
                stackManipulations.add(MethodVariableAccess.REFERENCE.loadFrom(1));
                stackManipulations.add(TypeCasting.to(instrumentedType));
                stackManipulations.add(FieldAccess.forField(fieldDescription).read());
                NullValueGuard nullValueGuard = fieldDescription.getType().isPrimitive() || fieldDescription.getType().isArray() || nonNullable.matches(fieldDescription)
                        ? NullValueGuard.NoOp.INSTANCE
                        : new NullValueGuard.UsingJump(instrumentedMethod);
                stackManipulations.add(nullValueGuard.before());
                stackManipulations.add(ValueComparator.of(fieldDescription.getType()));
                stackManipulations.add(nullValueGuard.after());
                padding = Math.max(padding, nullValueGuard.getRequiredVariablePadding());
            }
            stackManipulations.add(IntegerConstant.forValue(true));
            stackManipulations.add(MethodReturn.INTEGER);
            return new Size(new StackManipulation.Compound(stackManipulations).apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize() + padding);
        }
    }

    /**
     * A conditional return aborts the equality computation if a given condition was reached.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class ConditionalReturn implements StackManipulation {

        /**
         * An empty array.
         */
        private static final Object[] EMPTY = new Object[0];

        /**
         * The conditional jump instruction upon which the return is not triggered.
         */
        private final int jumpCondition;

        /**
         * The opcode for the value being returned.
         */
        private final int value;

        /**
         * Creates a conditional return for a value of {@code false}.
         *
         * @param jumpCondition The opcode upon which the return is not triggered.
         */
        protected ConditionalReturn(int jumpCondition) {
            this(jumpCondition, Opcodes.ICONST_0);
        }

        /**
         * Creates a conditional return.
         *
         * @param jumpCondition The opcode upon which the return is not triggered.
         * @param value         The opcode for the value being returned.
         */
        private ConditionalReturn(int jumpCondition, int value) {
            this.jumpCondition = jumpCondition;
            this.value = value;
        }

        /**
         * Returns a conditional return that returns on an {@code int} value of {@code 0}.
         *
         * @return A conditional return that returns on an {@code int} value of {@code 0}.
         */
        protected static ConditionalReturn onZeroInteger() {
            return new ConditionalReturn(Opcodes.IFNE);
        }

        /**
         * Returns a conditional return that returns on an {@code int} value of not {@code 0}.
         *
         * @return A conditional return that returns on an {@code int} value of not {@code 0}.
         */
        protected static ConditionalReturn onNonZeroInteger() {
            return new ConditionalReturn(Opcodes.IFEQ);
        }

        /**
         * Returns a conditional return that returns on a reference value of {@code null}.
         *
         * @return A conditional return that returns on a reference value of {@code null}.
         */
        protected static ConditionalReturn onNullValue() {
            return new ConditionalReturn(Opcodes.IFNONNULL);
        }

        /**
         * Returns a conditional return that returns if two reference values are not identical.
         *
         * @return A conditional return that returns if two reference values are not identical.
         */
        protected static ConditionalReturn onNonIdentity() {
            return new ConditionalReturn(Opcodes.IF_ACMPEQ);
        }

        /**
         * Returns a conditional return that returns if two reference values are identical.
         *
         * @return A conditional return that returns if two reference values are identical.
         */
        protected static ConditionalReturn onIdentity() {
            return new ConditionalReturn(Opcodes.IF_ACMPNE);
        }

        /**
         * Returns a conditional return that returns if two {@code int} values are not equal.
         *
         * @return A conditional return that returns if two {@code int} values are not equal.
         */
        protected static ConditionalReturn onNonEqualInteger() {
            return new ConditionalReturn(Opcodes.IF_ICMPEQ);
        }

        /**
         * Returns a new stack manipulation that returns {@code true} for the given condition.
         *
         * @return A new stack manipulation that returns {@code true} for the given condition.
         */
        protected StackManipulation returningTrue() {
            return new ConditionalReturn(jumpCondition, Opcodes.ICONST_1);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isValid() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
            Label label = new Label();
            methodVisitor.visitJumpInsn(jumpCondition, label);
            methodVisitor.visitInsn(value);
            methodVisitor.visitInsn(Opcodes.IRETURN);
            methodVisitor.visitLabel(label);
            if (implementationContext.getClassFileVersion().isAtLeast(ClassFileVersion.JAVA_V6)) {
                methodVisitor.visitFrame(Opcodes.F_SAME, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
            }
            return new Size(-1, 1);
        }
    }

    /**
     * A comparator that retains the natural order.
     */
    protected enum NaturalOrderComparator implements Comparator<FieldDescription.InDefinedShape> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public int compare(FieldDescription.InDefinedShape left, FieldDescription.InDefinedShape right) {
            return 0;
        }
    }

    /**
     * A comparator that sorts fields by a type property.
     */
    protected enum TypePropertyComparator implements Comparator<FieldDescription.InDefinedShape> {

        /**
         * Weights primitive types before non-primitive types.
         */
        FOR_PRIMITIVE_TYPES {
            @Override
            protected boolean resolve(TypeDefinition typeDefinition) {
                return typeDefinition.isPrimitive();
            }
        },

        /**
         * Weights enumeration types before non-enumeration types.
         */
        FOR_ENUMERATION_TYPES {
            @Override
            protected boolean resolve(TypeDefinition typeDefinition) {
                return typeDefinition.isEnum();
            }
        },

        /**
         * Weights {@link String} types first.
         */
        FOR_STRING_TYPES {
            @Override
            protected boolean resolve(TypeDefinition typeDefinition) {
                return typeDefinition.represents(String.class);
            }
        },

        /**
         * Weights primitive wrapper types first.
         */
        FOR_PRIMITIVE_WRAPPER_TYPES {
            @Override
            protected boolean resolve(TypeDefinition typeDefinition) {
                return typeDefinition.asErasure().isPrimitiveWrapper();
            }
        };

        /**
         * {@inheritDoc}
         */
        public int compare(FieldDescription.InDefinedShape left, FieldDescription.InDefinedShape right) {
            if (resolve(left.getType()) && !resolve(right.getType())) {
                return -1;
            } else if (!resolve(left.getType()) && resolve(right.getType())) {
                return 1;
            } else {
                return 0;
            }
        }

        /**
         * Resolves a type property.
         *
         * @param typeDefinition The type to resolve the property for.
         * @return {@code true} if the type property is resolved.
         */
        protected abstract boolean resolve(TypeDefinition typeDefinition);
    }

    /**
     * A compound comparator that compares the values of multiple fields.
     */
    @HashCodeAndEqualsPlugin.Enhance
    @SuppressFBWarnings(value = "SE_COMPARATOR_SHOULD_BE_SERIALIZABLE", justification = "Not used within a serializable instance")
    protected static class CompoundComparator implements Comparator<FieldDescription.InDefinedShape> {

        /**
         * All comparators to be applied in the application order.
         */
        private final List<Comparator<? super FieldDescription.InDefinedShape>> comparators;

        /**
         * Creates a compound comparator.
         *
         * @param comparator All comparators to be applied in the application order.
         */
        @SuppressWarnings("unchecked") // In absence of @SafeVarargs
        protected CompoundComparator(Comparator<? super FieldDescription.InDefinedShape>... comparator) {
            this(Arrays.asList(comparator));
        }

        /**
         * Creates a compound comparator.
         *
         * @param comparators All comparators to be applied in the application order.
         */
        protected CompoundComparator(List<? extends Comparator<? super FieldDescription.InDefinedShape>> comparators) {
            this.comparators = new ArrayList<Comparator<? super FieldDescription.InDefinedShape>>();
            for (Comparator<? super FieldDescription.InDefinedShape> comparator : comparators) {
                if (comparator instanceof CompoundComparator) {
                    this.comparators.addAll(((CompoundComparator) comparator).comparators);
                } else if (!(comparator instanceof NaturalOrderComparator)) {
                    this.comparators.add(comparator);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public int compare(FieldDescription.InDefinedShape left, FieldDescription.InDefinedShape right) {
            for (Comparator<? super FieldDescription.InDefinedShape> comparator : comparators) {
                int comparison = comparator.compare(left, right);
                if (comparison != 0) {
                    return comparison;
                }
            }
            return 0;
        }
    }
}
