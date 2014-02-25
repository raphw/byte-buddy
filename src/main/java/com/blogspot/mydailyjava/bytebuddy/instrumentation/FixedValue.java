package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.PrimitiveTypeAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.VoidAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.reference.ReferenceTypeAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.constant.*;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.FieldAccess;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Field;
import java.util.Random;

public abstract class FixedValue implements Instrumentation {

    public static interface AssignerConfigurable extends Instrumentation {

        Instrumentation withAssigner(Assigner assigner, boolean considerRuntimeType);
    }

    protected static class ForPoolValue extends FixedValue implements AssignerConfigurable, ByteCodeAppender {

        private final StackManipulation valueLoadInstruction;
        private final TypeDescription loadedType;

        protected ForPoolValue(StackManipulation valueLoadInstruction,
                               Class<?> loadedType,
                               Assigner assigner,
                               boolean considerRuntimeType) {
            this(valueLoadInstruction, new TypeDescription.ForLoadedType(loadedType), assigner, considerRuntimeType);
        }

        private ForPoolValue(StackManipulation valueLoadInstruction,
                             TypeDescription loadedType,
                             Assigner assigner,
                             boolean considerRuntimeType) {
            super(assigner, considerRuntimeType);
            this.valueLoadInstruction = valueLoadInstruction;
            this.loadedType = loadedType;
        }

        @Override
        public Instrumentation withAssigner(Assigner assigner, boolean considerRuntimeType) {
            return new ForPoolValue(valueLoadInstruction, loadedType, assigner, considerRuntimeType);
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(TypeDescription instrumentedType) {
            return this;
        }

        @Override
        public boolean appendsCode() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Context instrumentationContext, MethodDescription instrumentedMethod) {
            return apply(methodVisitor, instrumentationContext, instrumentedMethod, loadedType, valueLoadInstruction);
        }
    }

    protected static class ForStaticField extends FixedValue implements AssignerConfigurable, TypeInitializer {

        private static final Object STATIC_FIELD = null;
        private static final String PREFIX = "fixedValue";

        private class StaticFieldByteCodeAppender implements ByteCodeAppender {

            private final StackManipulation fieldGetAccess;

            private StaticFieldByteCodeAppender(TypeDescription instrumentedType) {
                fieldGetAccess = FieldAccess.forField(instrumentedType.getDeclaredFields().named(fieldName)).getter();
            }

            @Override
            public boolean appendsCode() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Context instrumentationContext, MethodDescription instrumentedMethod) {
                return ForStaticField.this.apply(methodVisitor, instrumentationContext, instrumentedMethod, fieldType, fieldGetAccess);
            }
        }

        private final String fieldName;
        private final Object fixedValue;
        private final TypeDescription fieldType;

        protected ForStaticField(Object fixedValue, Assigner assigner, boolean considerRuntimeType) {
            this(String.format("%s$%d", PREFIX, Math.abs(new Random().nextInt())), fixedValue, assigner, considerRuntimeType);
        }

        private ForStaticField(String fieldName, Object fixedValue, Assigner assigner, boolean considerRuntimeType) {
            super(assigner, considerRuntimeType);
            this.fieldName = fieldName;
            this.fixedValue = fixedValue;
            fieldType = new TypeDescription.ForLoadedType(fixedValue == null ? Object.class : fixedValue.getClass());
        }

        @Override
        public Instrumentation withAssigner(Assigner assigner, boolean considerRuntimeType) {
            return new ForStaticField(fieldName, fixedValue, assigner, considerRuntimeType);
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType
                    .withField(fieldName, fieldType, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC)
                    .withInitializer(this);
        }

        @Override
        public ByteCodeAppender appender(TypeDescription instrumentedType) {
            return new StaticFieldByteCodeAppender(instrumentedType);
        }

        @Override
        public void onLoad(Class<?> type) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(STATIC_FIELD, fixedValue);
            } catch (Exception e) {
                throw new IllegalStateException("Cannot set static field " + fieldName + " on " + type, e);
            }
        }
    }

    public static AssignerConfigurable value(Object fixedValue) {
        if (fixedValue == null) {
            return new ForPoolValue(NullConstant.INSTANCE,
                    Object.class,
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        }
        Class<?> type = fixedValue.getClass();
        if (type == String.class) {
            return new ForPoolValue(new TextConstant((String) fixedValue),
                    String.class,
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else if (type == Boolean.class) {
            return new ForPoolValue(IntegerConstant.forValue((Boolean) fixedValue),
                    boolean.class,
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else if (type == Byte.class) {
            return new ForPoolValue(IntegerConstant.forValue((Byte) fixedValue),
                    byte.class,
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else if (type == Short.class) {
            return new ForPoolValue(IntegerConstant.forValue((Short) fixedValue),
                    short.class,
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else if (type == Character.class) {
            return new ForPoolValue(IntegerConstant.forValue((Character) fixedValue),
                    char.class,
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else if (type == Integer.class) {
            return new ForPoolValue(IntegerConstant.forValue((Integer) fixedValue),
                    int.class,
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else if (type == Long.class) {
            return new ForPoolValue(LongConstant.forValue((Long) fixedValue),
                    long.class,
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else if (type == Float.class) {
            return new ForPoolValue(FloatConstant.forValue((Float) fixedValue),
                    float.class,
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else if (type == Double.class) {
            return new ForPoolValue(DoubleConstant.forValue((Double) fixedValue),
                    double.class,
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else {
            return reference(fixedValue);
        }
    }

    public static AssignerConfigurable reference(Object fixedValue) {
        return new ForStaticField(fixedValue, defaultAssigner(), defaultConsiderRuntimeType());
    }

    private static Assigner defaultAssigner() {
        return new VoidAwareAssigner(new PrimitiveTypeAwareAssigner(ReferenceTypeAwareAssigner.INSTANCE), false);
    }

    private static boolean defaultConsiderRuntimeType() {
        return false;
    }

    private final Assigner assigner;
    private final boolean considerRuntimeType;

    protected FixedValue(Assigner assigner, boolean considerRuntimeType) {
        this.assigner = assigner;
        this.considerRuntimeType = considerRuntimeType;
    }

    protected ByteCodeAppender.Size apply(MethodVisitor methodVisitor,
                                          Context instrumentationContext,
                                          MethodDescription instrumentedMethod,
                                          TypeDescription loadedValueType,
                                          StackManipulation valueLoadingInstruction) {
        StackManipulation.Size stackSize = new StackManipulation.Compound(
                valueLoadingInstruction,
                assigner.assign(loadedValueType, instrumentedMethod.getReturnType(), considerRuntimeType),
                MethodReturn.returning(instrumentedMethod.getReturnType())
        ).apply(methodVisitor, instrumentationContext);
        return new ByteCodeAppender.Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
    }
}
