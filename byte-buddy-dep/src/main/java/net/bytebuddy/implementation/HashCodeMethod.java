package net.bytebuddy.implementation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.*;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class HashCodeMethod implements Implementation {

    private static final MethodDescription.InDefinedShape HASH_CODE = new TypeDescription.ForLoadedType(Object.class)
            .getDeclaredMethods()
            .filter(isHashCode())
            .getOnly();

    private final HashCodeInitializer hashCodeInitializer;

    private final int multiplier;

    private final ElementMatcher.Junction<? super FieldDescription.InDefinedShape> ignored;

    private final ElementMatcher.Junction<? super FieldDescription.InDefinedShape> nonNullable;

    protected HashCodeMethod(HashCodeInitializer hashCodeInitializer) {
        this(hashCodeInitializer, 31, none(), none());
    }

    private HashCodeMethod(HashCodeInitializer hashCodeInitializer,
                             int multiplier,
                             ElementMatcher.Junction<? super FieldDescription.InDefinedShape> ignored,
                             ElementMatcher.Junction<? super FieldDescription.InDefinedShape> nonNullable) {
        this.hashCodeInitializer = hashCodeInitializer;
        this.multiplier = multiplier;
        this.ignored = ignored;
        this.nonNullable = nonNullable;
    }

    public static HashCodeMethod invokingSuperMethod() {
        return new HashCodeMethod(HashCodeInitializer.ForSuperMethodCall.INSTANCE);
    }

    public static HashCodeMethod usingDefaultOffset() {
        return usingInitialOffset(17);
    }

    public static HashCodeMethod usingInitialOffset(int value) {
        return new HashCodeMethod(new HashCodeInitializer.ForDefaultValue(value));
    }

    public HashCodeMethod withIgnoredFields(ElementMatcher<? super FieldDescription.InDefinedShape> ignored) {
        return new HashCodeMethod(hashCodeInitializer, multiplier, this.ignored.or(ignored), nonNullable);
    }

    public HashCodeMethod withNonNullableFields(ElementMatcher<? super FieldDescription.InDefinedShape> nonNullable) {
        return new HashCodeMethod(hashCodeInitializer, multiplier, ignored, this.nonNullable.or(nonNullable));
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
    }

    @Override
    public ByteCodeAppender appender(Target implementationTarget) {
        if (implementationTarget.getInstrumentedType().isInterface()) {
            throw new IllegalStateException();
        }
        return new Appender(hashCodeInitializer.resolve(implementationTarget.getInstrumentedType()),
                implementationTarget.getInstrumentedType().getDeclaredFields().filter(not(isStatic().or(ignored))));
    }

    protected class Appender implements ByteCodeAppender {

        private final StackManipulation initialValue;

        private final List<FieldDescription.InDefinedShape> fieldDescriptions;

        protected Appender(StackManipulation initialValue, List<FieldDescription.InDefinedShape> fieldDescriptions) {
            this.initialValue = initialValue;
            this.fieldDescriptions = fieldDescriptions;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            if (!instrumentedMethod.getReturnType().represents(int.class)) {
                throw new IllegalStateException();
            }
            List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(2 + fieldDescriptions.size() * 8);
            stackManipulations.add(initialValue);
            StackSize padding = StackSize.ZERO;
            for (FieldDescription.InDefinedShape fieldDescription : fieldDescriptions) {
                stackManipulations.add(IntegerConstant.forValue(multiplier));
                stackManipulations.add(Multiplication.INTEGER);
                stackManipulations.add(MethodVariableAccess.loadThis());
                stackManipulations.add(FieldAccess.forField(fieldDescription).read());
                NullValueGuard nullValueGuard = fieldDescription.getType().isPrimitive() || nonNullable.matches(fieldDescription)
                        ? NullValueGuard.NoOp.INSTANCE
                        : new NullValueGuard.UsingJump(instrumentedMethod);
                stackManipulations.add(nullValueGuard.before());
                stackManipulations.add(ValueTransformer.of(fieldDescription.getType()));
                stackManipulations.add(Addition.INTEGER);
                stackManipulations.add(nullValueGuard.after());
                padding = padding.maximum(nullValueGuard.getRequiredVariablePadding());
            }
            stackManipulations.add(MethodReturn.INTEGER);
            return new Size(new StackManipulation.Compound(stackManipulations).apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize() + padding.getSize());
        }
    }

    protected interface HashCodeInitializer {

        StackManipulation resolve(TypeDescription instrumentedType);

        class ForDefaultValue implements HashCodeInitializer {

            private final int value;

            protected ForDefaultValue(int value) {
                this.value = value;
            }

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType) {
                return IntegerConstant.forValue(value);
            }
        }

        enum ForSuperMethodCall implements HashCodeInitializer {

            INSTANCE;

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType) {
                return MethodInvocation.invoke(HASH_CODE).special(instrumentedType.getSuperClass().asErasure());
            }
        }
    }

    protected interface NullValueGuard {

        StackManipulation before();

        StackManipulation after();

        StackSize getRequiredVariablePadding();

        enum NoOp implements NullValueGuard {

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

        class UsingJump implements NullValueGuard {

            private static final Object[] EMPTY = new Object[0];

            private static final Object[] INTEGER = new Object[]{Opcodes.INTEGER};

            private final MethodDescription instrumentedMethod;

            private final Label label;

            protected UsingJump(MethodDescription instrumentedMethod) {
                this.instrumentedMethod = instrumentedMethod;
                label = new Label();
            }

            @Override
            public StackManipulation before() {
                return new BeforeInstruction();
            }

            @Override
            public StackManipulation after() {
                return new AfterInstruction();
            }

            @Override
            public StackSize getRequiredVariablePadding() {
                return StackSize.SINGLE;
            }

            protected class BeforeInstruction implements StackManipulation {

                @Override
                public boolean isValid() {
                    return true;
                }

                @Override
                public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                    methodVisitor.visitIntInsn(Opcodes.ASTORE, instrumentedMethod.getStackSize());
                    methodVisitor.visitIntInsn(Opcodes.ALOAD, instrumentedMethod.getStackSize());
                    methodVisitor.visitJumpInsn(Opcodes.IFNULL, label);
                    methodVisitor.visitIntInsn(Opcodes.ALOAD, instrumentedMethod.getStackSize());
                    return new Size(0, 0);
                }
            }

            protected class AfterInstruction implements StackManipulation {

                @Override
                public boolean isValid() {
                    return true;
                }

                @Override
                public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                    methodVisitor.visitLabel(label);
                    if (implementationContext.getClassFileVersion().isAtLeast(ClassFileVersion.JAVA_V6)) {
                        methodVisitor.visitFrame(Opcodes.F_SAME1, EMPTY.length, EMPTY, INTEGER.length, INTEGER);
                    }
                    return new Size(0, 0);
                }
            }
        }
    }

    protected enum ValueTransformer implements StackManipulation {

        LONG {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitInsn(Opcodes.DUP2);
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 32);
                methodVisitor.visitInsn(Opcodes.LUSHR);
                methodVisitor.visitInsn(Opcodes.LXOR);
                methodVisitor.visitInsn(Opcodes.L2I);
                return new Size(-1, 3);
            }
        },

        FLOAT {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "floatToIntBits", "(F)I", false);
                return new Size(0, 0);
            }
        },

        DOUBLE {
            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "doubleToLongBits", "(D)J", false);
                methodVisitor.visitInsn(Opcodes.DUP2);
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 32);
                methodVisitor.visitInsn(Opcodes.LUSHR);
                methodVisitor.visitInsn(Opcodes.LXOR);
                methodVisitor.visitInsn(Opcodes.L2I);
                return new Size(-1, 3);
            }
        };

        public static StackManipulation of(TypeDefinition typeDefinition) { // TODO: Arrays!
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
            } else {
                return MethodInvocation.invoke(HASH_CODE).virtual(typeDefinition.asErasure());
            }
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }
}
