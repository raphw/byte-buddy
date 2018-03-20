package net.bytebuddy.implementation;

import jdk.nashorn.internal.codegen.types.Type;
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

import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

@EqualsAndHashCode
public class EqualsMethod implements Implementation {

    private static final MethodDescription.InDefinedShape EQUALS = new TypeDescription.ForLoadedType(Object.class)
            .getDeclaredMethods()
            .filter(isEquals())
            .getOnly();

    private final BaselineEquality baselineEquality;

    private final CompatibilityCheck compatibilityCheck;

    private final ElementMatcher.Junction<? super FieldDescription.InDefinedShape> ignored;

    private final ElementMatcher.Junction<? super FieldDescription.InDefinedShape> nonNullable;

    protected EqualsMethod(BaselineEquality offsetProvider) {
        this(offsetProvider, CompatibilityCheck.EXACT, none(), none());
    }

    private EqualsMethod(BaselineEquality baselineEquality,
                         CompatibilityCheck compatibilityCheck,
                         ElementMatcher.Junction<? super FieldDescription.InDefinedShape> ignored,
                         ElementMatcher.Junction<? super FieldDescription.InDefinedShape> nonNullable) {
        this.baselineEquality = baselineEquality;
        this.compatibilityCheck = compatibilityCheck;
        this.ignored = ignored;
        this.nonNullable = nonNullable;
    }

    public static EqualsMethod requiringSuperClassEquality() {
        return new EqualsMethod(BaselineEquality.FOR_SUPER_METHOD_CALL);
    }

    public static EqualsMethod isolated() {
        return new EqualsMethod(BaselineEquality.NONE);
    }

    public EqualsMethod withIgnoredFields(ElementMatcher<? super FieldDescription.InDefinedShape> ignored) {
        return new EqualsMethod(baselineEquality, compatibilityCheck, this.ignored.or(ignored), nonNullable);
    }

    public EqualsMethod withNonNullableFields(ElementMatcher<? super FieldDescription.InDefinedShape> nonNullable) {
        return new EqualsMethod(baselineEquality, compatibilityCheck, ignored, this.nonNullable.or(nonNullable));
    }

    public Implementation withSubclassEquality() {
        return new EqualsMethod(baselineEquality, CompatibilityCheck.SUBCLASS, ignored, nonNullable);
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
                baselineEquality.resolve(implementationTarget.getInstrumentedType()),
                compatibilityCheck.resolve(implementationTarget.getInstrumentedType())
        ), implementationTarget.getInstrumentedType().getDeclaredFields().filter(not(isStatic().or(ignored))));
    }

    protected enum BaselineEquality {

        NONE {
            @Override
            protected StackManipulation resolve(TypeDescription instrumentedType) {
                return StackManipulation.Trivial.INSTANCE;
            }
        },

        FOR_SUPER_METHOD_CALL {
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

        protected abstract StackManipulation resolve(TypeDescription instrumentedType);
    }

    protected enum CompatibilityCheck {

        EXACT {

            private final MethodDescription.InDefinedShape getClass = new TypeDescription.ForLoadedType(Object.class)
                    .getDeclaredMethods()
                    .filter(named("getClass"))
                    .getOnly();

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType) {
                return new StackManipulation.Compound(
                        MethodVariableAccess.REFERENCE.loadFrom(1),
                        ConditionalReturn.onNullValue(),
                        MethodVariableAccess.REFERENCE.loadFrom(1),
                        MethodInvocation.invoke(getClass),
                        ClassConstant.of(instrumentedType),
                        ConditionalReturn.onNonIdentity()
                );
            }
        },

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

        protected abstract StackManipulation resolve(TypeDescription instrumentedType);
    }

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
        StackSize getRequiredVariablePadding();

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
            public StackSize getRequiredVariablePadding() {
                return StackSize.ZERO;
            }
        }

        /**
         * A null value guard that expects a reference type and that uses a jump if a field value is {@code null}.
         */
        @EqualsAndHashCode
        class UsingJump implements NullValueGuard {

            /**
             * An empty array.
             */
            private static final Object[] EMPTY = new Object[0];

            private static final Object[] REFERENCE = new Object[]{Type.getInternalName(Object.class)};

            /**
             * The instrumented method.
             */
            private final MethodDescription instrumentedMethod;

            private final Label nullValue;

            /**
             * Creates a new null value guard using a jump instruction for {@code null} values.
             *
             * @param instrumentedMethod The instrumented method.
             */
            protected UsingJump(MethodDescription instrumentedMethod) {
                this.instrumentedMethod = instrumentedMethod;
                nullValue = new Label();
            }

            @Override
            public StackManipulation before() {
                return new UsingJump.BeforeInstruction();
            }

            @Override
            public StackManipulation after() {
                return new UsingJump.AfterInstruction();
            }

            @Override
            public StackSize getRequiredVariablePadding() {
                return StackSize.SINGLE;
            }

            /**
             * The stack manipulation to apply before the hash value computation.
             */
            protected class BeforeInstruction implements StackManipulation {

                @Override
                public boolean isValid() {
                    return true;
                }

                @Override
                public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                    methodVisitor.visitVarInsn(Opcodes.ASTORE, instrumentedMethod.getStackSize());
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, instrumentedMethod.getStackSize());
                    methodVisitor.visitJumpInsn(Opcodes.IFNULL, nullValue);
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, instrumentedMethod.getStackSize());
                    return new Size(0, 0);
                }
            }

            /**
             * The stack manipulation to apply after the hash value computation.
             */
            protected class AfterInstruction implements StackManipulation {

                @Override
                public boolean isValid() {
                    return true;
                }

                @Override
                public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                    Label equal = new Label();
                    methodVisitor.visitJumpInsn(Opcodes.GOTO, equal);
                    methodVisitor.visitLabel(nullValue);
                    if (implementationContext.getClassFileVersion().isAtLeast(ClassFileVersion.JAVA_V6)) {
                        methodVisitor.visitFrame(Opcodes.F_SAME1, EMPTY.length, EMPTY, REFERENCE.length, REFERENCE);
                    }
                    methodVisitor.visitJumpInsn(Opcodes.IFNULL, equal);
                    methodVisitor.visitInsn(Opcodes.ICONST_0);
                    methodVisitor.visitInsn(Opcodes.IRETURN);
                    methodVisitor.visitLabel(equal);
                    if (implementationContext.getClassFileVersion().isAtLeast(ClassFileVersion.JAVA_V6)) {
                        methodVisitor.visitFrame(Opcodes.F_SAME, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
                    }
                    return new Size(0, 0);
                }
            }
        }
    }

    /**
     * A value transformer that is responsible for resolving a field value to an {@code int} value.
     */
    protected enum ValueComparator implements StackManipulation {

        /**
         * A transformer for a {@code long} value.
         */
        LONG {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitInsn(Opcodes.LCMP);
                return new Size(-2, 0);
            }
        },

        /**
         * A transformer for a {@code float} value.
         */
        FLOAT {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitInsn(Opcodes.FCMPL);
                return new Size(-1, 0);
            }
        },

        /**
         * A transformer for a {@code double} value.
         */
        DOUBLE {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitInsn(Opcodes.DCMPL);
                return new Size(-2, 0);
            }
        },

        /**
         * A transformer for a {@code boolean[]} value.
         */
        BOOLEAN_ARRAY {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([Z[Z)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A transformer for a {@code byte[]} value.
         */
        BYTE_ARRAY {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([B[B)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A transformer for a {@code short[]} value.
         */
        SHORT_ARRAY {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([S[S)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A transformer for a {@code char[]} value.
         */
        CHARACTER_ARRAY {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([C[C)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A transformer for an {@code int[]} value.
         */
        INTEGER_ARRAY {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([I[I)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A transformer for a {@code long[]} value.
         */
        LONG_ARRAY {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "([J[J)Z", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A transformer for a {@code float[]} value.
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
         * Resolves a type definition to a hash code.
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

    @EqualsAndHashCode
    protected class Appender implements ByteCodeAppender {

        private final TypeDescription instrumentedType;

        private final StackManipulation baseline;

        private final List<FieldDescription.InDefinedShape> fieldDescriptions;

        protected Appender(TypeDescription instrumentedType, StackManipulation baseline, List<FieldDescription.InDefinedShape> fieldDescriptions) {
            this.instrumentedType = instrumentedType;
            this.baseline = baseline;
            this.fieldDescriptions = fieldDescriptions;
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
            StackSize padding = StackSize.ZERO;
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
                padding = padding.maximum(nullValueGuard.getRequiredVariablePadding());
            }
            stackManipulations.add(IntegerConstant.forValue(true));
            stackManipulations.add(MethodReturn.INTEGER);
            return new Size(new StackManipulation.Compound(stackManipulations).apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize() + padding.getSize());
        }
    }

    protected static class ConditionalReturn implements StackManipulation {

        private static final Object[] EMPTY = new Object[0];

        private final int opcode;

        protected ConditionalReturn(int opcode) {
            this.opcode = opcode;
        }

        protected static StackManipulation onZeroInteger() {
            return new ConditionalReturn(Opcodes.IFNE);
        }

        protected static StackManipulation onNonZeroInteger() {
            return new ConditionalReturn(Opcodes.IFEQ);
        }

        protected static StackManipulation onNullValue() {
            return new ConditionalReturn(Opcodes.IFNONNULL);
        }

        protected static StackManipulation onNonIdentity() {
            return new ConditionalReturn(Opcodes.IF_ACMPEQ);
        }

        protected static StackManipulation onNonEqualInteger() {
            return new ConditionalReturn(Opcodes.IF_ICMPEQ);
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
            Label label = new Label();
            methodVisitor.visitJumpInsn(opcode, label);
            methodVisitor.visitInsn(Opcodes.ICONST_0);
            methodVisitor.visitInsn(Opcodes.IRETURN);
            methodVisitor.visitLabel(label);
            if (implementationContext.getClassFileVersion().isAtLeast(ClassFileVersion.JAVA_V6)) {
                methodVisitor.visitFrame(Opcodes.F_SAME, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
            }
            return new Size(-1, 1);
        }
    }
}
