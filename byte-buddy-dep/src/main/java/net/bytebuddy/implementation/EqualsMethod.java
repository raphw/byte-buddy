package net.bytebuddy.implementation;

import lombok.EqualsAndHashCode;
import net.bytebuddy.ClassFileVersion;
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
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * An implementation of {@link Object#equals(Object)} that takes a class's declared fields into consideration. Equality is resolved by comparing two
 * instances of the same or a compatible class field by field where reference fields must either both be {@code null} or where the field value of
 * the instance upon which the method is invoked returns {@code true} upon calling the value's {@code equals} method. For arrays, the corresponding
 * utilities of {@link java.util.Arrays} are used.
 */
@EqualsAndHashCode
public class EqualsMethod implements Implementation {

    /**
     * The {@link Object#equals(Object)} method.
     */
    private static final MethodDescription.InDefinedShape EQUALS = new TypeDescription.ForLoadedType(Object.class)
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
     * A matcher to filter fields that should not be used for a equality resoltion.
     */
    private final ElementMatcher.Junction<? super FieldDescription.InDefinedShape> ignored;

    /**
     * A matcher to determine fields of a reference type that cannot be {@code null}.
     */
    private final ElementMatcher.Junction<? super FieldDescription.InDefinedShape> nonNullable;

    /**
     * Creates a new equals method implementation.
     *
     * @param superClassCheck The baseline equality to check.
     */
    protected EqualsMethod(SuperClassCheck superClassCheck) {
        this(superClassCheck, TypeCompatibilityCheck.EXACT, none(), none());
    }

    /**
     * Creates a new equals method implementation.
     *
     * @param superClassCheck        The baseline equality to check.
     * @param typeCompatibilityCheck The instance type compatibility check.
     * @param ignored                A matcher to filter fields that should not be used for a equality resoltion.
     * @param nonNullable            A matcher to determine fields of a reference type that cannot be {@code null}.
     */
    private EqualsMethod(SuperClassCheck superClassCheck,
                         TypeCompatibilityCheck typeCompatibilityCheck,
                         ElementMatcher.Junction<? super FieldDescription.InDefinedShape> ignored,
                         ElementMatcher.Junction<? super FieldDescription.InDefinedShape> nonNullable) {
        this.superClassCheck = superClassCheck;
        this.typeCompatibilityCheck = typeCompatibilityCheck;
        this.ignored = ignored;
        this.nonNullable = nonNullable;
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
        return new EqualsMethod(superClassCheck, typeCompatibilityCheck, this.ignored.or(ignored), nonNullable);
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
        return new EqualsMethod(superClassCheck, typeCompatibilityCheck, ignored, this.nonNullable.or(nonNullable));
    }

    /**
     * Returns a new version of this equals method implementation that permits subclasses of the instrumented type to be equal to instances
     * of the instrumented type instead of requiring an exact match.
     *
     * @return A new version of this equals method implementation that permits subclasses of the instrumented type to be equal to instances
     * of the instrumented type instead of requiring an exact match.
     */
    public Implementation withSubclassEquality() {
        return new EqualsMethod(superClassCheck, TypeCompatibilityCheck.SUBCLASS, ignored, nonNullable);
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
    }

    @Override
    public ByteCodeAppender appender(Target implementationTarget) {
        if (implementationTarget.getInstrumentedType().isInterface()) {
            throw new IllegalStateException("Cannot implement meaningful equals method for " + implementationTarget.getInstrumentedType());
        }
        return new Appender(implementationTarget.getInstrumentedType(), new StackManipulation.Compound(
                superClassCheck.resolve(implementationTarget.getInstrumentedType()),
                MethodVariableAccess.loadThis(),
                MethodVariableAccess.REFERENCE.loadFrom(1),
                ConditionalReturn.onIdentity().returningTrue(),
                typeCompatibilityCheck.resolve(implementationTarget.getInstrumentedType())
        ), implementationTarget.getInstrumentedType().getDeclaredFields().filter(not(isStatic().or(ignored))), nonNullable);
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
                        MethodVariableAccess.REFERENCE.loadFrom(1),
                        MethodInvocation.invoke(GET_CLASS),
                        ClassConstant.of(instrumentedType),
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
        protected static final MethodDescription.InDefinedShape GET_CLASS = new TypeDescription.ForLoadedType(Object.class)
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

            @Override
            public StackManipulation before() {
                return StackManipulation.Trivial.INSTANCE;
            }

            @Override
            public StackManipulation after() {
                return StackManipulation.Trivial.INSTANCE;
            }

            @Override
            public int getRequiredVariablePadding() {
                return StackSize.ZERO.getSize();
            }
        }

        /**
         * A null value guard that expects a reference type and that skips the comparison if both values are {@code null} but returns if
         * the invoked instance's field value is {@code null} but not the compared instance's value.
         */
        @EqualsAndHashCode
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

            @Override
            public StackManipulation before() {
                return new UsingJump.BeforeInstruction(instrumentedMethod, firstValueNull, secondValueNull);
            }

            @Override
            public StackManipulation after() {
                return new UsingJump.AfterInstruction(instrumentedMethod, firstValueNull, secondValueNull, endOfBlock);
            }

            @Override
            public int getRequiredVariablePadding() {
                return 2;
            }

            /**
             * The stack manipulation to apply before the equality computation.
             */
            @EqualsAndHashCode
            protected static class BeforeInstruction implements StackManipulation {

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
                 * Creates an instruction to execute before an equality check.
                 *
                 * @param instrumentedMethod The instrumented method.
                 * @param firstValueNull     The label to jump to if the first value is {@code null} whereas the second value is not {@code null}.
                 * @param secondValueNull    The label to jump to if the second value is {@code null}.
                 */
                protected BeforeInstruction(MethodDescription instrumentedMethod, Label firstValueNull, Label secondValueNull) {
                    this.instrumentedMethod = instrumentedMethod;
                    this.firstValueNull = firstValueNull;
                    this.secondValueNull = secondValueNull;
                }

                @Override
                public boolean isValid() {
                    return true;
                }

                @Override
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
            @EqualsAndHashCode
            protected static class AfterInstruction implements StackManipulation {

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
                 * Creates an instruction to execute after an equality check.
                 *
                 * @param instrumentedMethod The instrumented method.
                 * @param firstValueNull     The label to jump to if the first value is {@code null} whereas the second value is not {@code null}.
                 * @param secondValueNull    The label to jump to if the second value is {@code null}.
                 * @param endOfBlock         A label indicating the end of the null-guarding block.
                 */
                protected AfterInstruction(MethodDescription instrumentedMethod, Label firstValueNull, Label secondValueNull, Label endOfBlock) {
                    this.instrumentedMethod = instrumentedMethod;
                    this.firstValueNull = firstValueNull;
                    this.secondValueNull = secondValueNull;
                    this.endOfBlock = endOfBlock;
                }

                @Override
                public boolean isValid() {
                    return true;
                }

                @Override
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
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitInsn(Opcodes.LCMP);
                return new Size(-2, 0);
            }
        },

        /**
         * A comparator for a {@code float} value.
         */
        FLOAT {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitInsn(Opcodes.FCMPL);
                return new Size(-1, 0);
            }
        },

        /**
         * A comparator for a {@code double} value.
         */
        DOUBLE {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitInsn(Opcodes.DCMPL);
                return new Size(-2, 0);
            }
        },

        /**
         * A comparator for a {@code boolean[]} value.
         */
        BOOLEAN_ARRAY {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([Z[Z)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A comparator for a {@code byte[]} value.
         */
        BYTE_ARRAY {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([B[B)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A comparator for a {@code short[]} value.
         */
        SHORT_ARRAY {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([S[S)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A comparator for a {@code char[]} value.
         */
        CHARACTER_ARRAY {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([C[C)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A comparator for an {@code int[]} value.
         */
        INTEGER_ARRAY {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([I[I)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A comparator for a {@code long[]} value.
         */
        LONG_ARRAY {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([J[J)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A comparator for a {@code float[]} value.
         */
        FLOAT_ARRAY {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([F[F)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A transformer for a {@code double[]} value.
         */
        DOUBLE_ARRAY {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([D[D)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A transformer for a reference array value.
         */
        REFERENCE_ARRAY {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([Ljava/lang/Object;[Ljava/lang/Object;)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A transformer for a nested reference array value.
         */
        NESTED_ARRAY {
            @Override
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

        @Override
        public boolean isValid() {
            return true;
        }
    }

    /**
     * A byte code appender to implement the {@link EqualsMethod}.
     */
    @EqualsAndHashCode
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

        @Override
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
    @EqualsAndHashCode
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

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
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
}
