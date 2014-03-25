package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.PrimitiveTypeAwareAssigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.VoidAwareAssigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.reference.ReferenceTypeAwareAssigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.*;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.FieldAccess;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Field;

/**
 * This instrumentation returns a fixed value for a method. Other than the
 * {@link net.bytebuddy.instrumentation.StubMethod} instrumentation, this implementation allows
 * to determine a specific value which must be assignable to the returning value of any instrumented method. Otherwise,
 * an exception will be thrown.
 */
public abstract class FixedValue implements Instrumentation {

    /**
     * Represents a fixed value instrumentation that is using a default assigner for attempting to assign
     * the fixed value to the return type of the instrumented method.
     */
    public static interface AssignerConfigurable extends Instrumentation {

        /**
         * Defines an explicit assigner to this fixed value instrumentation.
         *
         * @param assigner            The assigner to use for assigning the fixed value to the return type of the
         *                            instrumented value.
         * @param considerRuntimeType If {@code true}, the runtime type of the given value will be considered for
         *                            assigning the return type.
         * @return A fixed value instrumentation that makes use of the given assigner.
         */
        Instrumentation withAssigner(Assigner assigner, boolean considerRuntimeType);
    }

    /**
     * A fixed value instrumentation that represents its fixed value as a value that is written to the instrumented
     * class's constant pool.
     */
    protected static class ForPoolValue extends FixedValue implements AssignerConfigurable, ByteCodeAppender {

        private final StackManipulation valueLoadInstruction;
        private final TypeDescription loadedType;

        /**
         * Creates a new constant pool fixed value instrumentation.
         *
         * @param valueLoadInstruction The instruction that is responsible for loading the constant pool value onto the
         *                             operand stack.
         * @param loadedType           A type description representing the loaded type.
         * @param assigner             The assigner to use for assigning the fixed value to the return type of the
         *                             instrumented value.
         * @param considerRuntimeType  If {@code true}, the runtime type of the given value will be considered for
         *                             assigning the return type.
         */
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

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && loadedType.equals(((ForPoolValue) other).loadedType)
                    && valueLoadInstruction.equals(((ForPoolValue) other).valueLoadInstruction)
                    && super.equals(other);
        }

        @Override
        public int hashCode() {
            return 31 * 31 * super.hashCode() + 31 * valueLoadInstruction.hashCode() + loadedType.hashCode();
        }

        @Override
        public String toString() {
            return "FixedValue.ForPoolValue{" +
                    "valueLoadInstruction=" + valueLoadInstruction +
                    ", loadedType=" + loadedType +
                    ", assigner=" + assigner +
                    ", considerRuntimeType=" + considerRuntimeType +
                    '}';
        }
    }

    /**
     * A fixed value instrumentation that represents its fixed value as a static field of the instrumented class.
     */
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

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && fieldGetAccess.equals(((StaticFieldByteCodeAppender) other).fieldGetAccess);
            }

            @Override
            public int hashCode() {
                return fieldGetAccess.hashCode();
            }

            @Override
            public String toString() {
                return "StaticFieldByteCodeAppender{fieldGetAccess=" + fieldGetAccess + '}';
            }
        }

        private final String fieldName;
        private final Object fixedValue;

        private final TypeDescription fieldType;

        /**
         * Creates a new static field fixed value instrumentation with a random name for the field containing the fixed
         * value.
         *
         * @param fixedValue          The fixed value to be returned.
         * @param assigner            The assigner to use for assigning the fixed value to the return type of the
         *                            instrumented value.
         * @param considerRuntimeType If {@code true}, the runtime type of the given value will be considered for
         *                            assigning the return type.
         */
        protected ForStaticField(Object fixedValue, Assigner assigner, boolean considerRuntimeType) {
            this(String.format("%s$%d", PREFIX, Math.abs(fixedValue.hashCode())), fixedValue, assigner, considerRuntimeType);
        }

        /**
         * Creates a new static field fixed value instrumentation.
         *
         * @param fieldName           The name of the field for storing the fixed value.
         * @param fixedValue          The fixed value to be returned.
         * @param assigner            The assigner to use for assigning the fixed value to the return type of the
         *                            instrumented value.
         * @param considerRuntimeType If {@code true}, the runtime type of the given value will be considered for
         *                            assigning the return type.
         */
        protected ForStaticField(String fieldName, Object fixedValue, Assigner assigner, boolean considerRuntimeType) {
            super(assigner, considerRuntimeType);
            this.fieldName = fieldName;
            this.fixedValue = fixedValue;
            fieldType = new TypeDescription.ForLoadedType(fixedValue.getClass());
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

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && fieldName.equals(((ForStaticField) other).fieldName)
                    && fixedValue.equals(((ForStaticField) other).fixedValue)
                    && super.equals(other);
        }

        @Override
        public int hashCode() {
            return 31 * 31 * super.hashCode() + 31 * fieldName.hashCode() + fixedValue.hashCode();
        }

        @Override
        public String toString() {
            return "FixedValue.ForStaticField{" +
                    "fieldName='" + fieldName + '\'' +
                    ", fixedValue=" + fixedValue +
                    ", assigner=" + assigner +
                    ", considerRuntimeType=" + considerRuntimeType +
                    '}';
        }
    }

    /**
     * Creates a fixed value instrumentation that returns a fixed value. If the value can be inlined into the created
     * class, i.e. can be added to the constant pool of a class, no explicit type initialization will be required for
     * the created dynamic class. Otherwise, a static field will be created in the dynamic type which will be initialized
     * with the given value. The following Java types can be inlined:
     * <ul>
     * <li>The {@link java.lang.String} type.</li>
     * <li>All primitive types and their wrapper type, i.e. {@link java.lang.Boolean}, {@link java.lang.Byte},
     * {@link java.lang.Short}, {@link java.lang.Character}, {@link java.lang.Integer}, {@link java.lang.Long},
     * {@link java.lang.Float} and {@link java.lang.Double}.</li>
     * <li>A {@code null} reference.</li>
     * </ul>
     * <p>&nbsp;</p>
     * If possible, the constant pool value is substituted by a byte code instruction that creates the value. (This is
     * possible for integer types and types that are presented by integers inside the JVM ({@code boolean}, {@code byte},
     * {@code short}, {@code char}) and for the {@code null} value. Additionally, several common constants of
     * the {@code float}, {@code double} and {@code long} types can be represented by opcode constants.
     *
     * @param fixedValue The fixed value to be returned by methods that are instrumented by this instrumentation.
     * @return An instrumentation for the given {@code fixedValue}.
     */
    public static AssignerConfigurable value(Object fixedValue) {
        if (fixedValue == null) {
            return new ForPoolValue(NullConstant.INSTANCE,
                    Object.class,
                    defaultAssigner(),
                    true);
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

    /**
     * Other than {@link net.bytebuddy.instrumentation.FixedValue#value(Object)}, this function
     * will create a fixed value instrumentation that will always defined a field in the instrumented class. As a result,
     * object identity will be preserved between the given {@code fixedValue} and the value that is returned by
     * instrumented methods.
     * <p>&nbsp;</p>
     * As an exception, the {@code null} value is always presented by a constant value and is never stored in a static
     * field.
     *
     * @param fixedValue The fixed value to be returned by methods that are instrumented by this instrumentation.
     * @return An instrumentation for the given {@code fixedValue}.
     */
    public static AssignerConfigurable reference(Object fixedValue) {
        if (fixedValue == null) {
            return new ForPoolValue(NullConstant.INSTANCE,
                    Object.class,
                    defaultAssigner(),
                    true);
        }
        return new ForStaticField(fixedValue, defaultAssigner(), defaultConsiderRuntimeType());
    }

    /**
     * Other than {@link net.bytebuddy.instrumentation.FixedValue#value(Object)}, this function
     * will create a fixed value instrumentation that will always defined a field in the instrumented class. As a result,
     * object identity will be preserved between the given {@code fixedValue} and the value that is returned by
     * instrumented methods. The field name can be explicitly determined.
     * <p>&nbsp;</p>
     * As an exception, the {@code null} value cannot be used for this instrumentation but will cause an exception.
     *
     * @param fixedValue The fixed value to be returned by methods that are instrumented by this instrumentation.
     * @param fieldName  The name of the field for storing the fixed value.
     * @return An instrumentation for the given {@code fixedValue}.
     */
    public static AssignerConfigurable reference(Object fixedValue, String fieldName) {
        if (fixedValue == null) {
            throw new IllegalArgumentException("The fixed value must not be null");
        }
        return new ForStaticField(fieldName, fixedValue, defaultAssigner(), defaultConsiderRuntimeType());
    }

    private static Assigner defaultAssigner() {
        return new VoidAwareAssigner(new PrimitiveTypeAwareAssigner(ReferenceTypeAwareAssigner.INSTANCE), false);
    }

    private static boolean defaultConsiderRuntimeType() {
        return false;
    }

    /**
     * The assigner that is used for assigning the fixed value to a method's return type.
     */
    protected final Assigner assigner;

    /**
     * Determines if the runtime type of a fixed value should be considered for the assignment to a return type.
     */
    protected final boolean considerRuntimeType;

    /**
     * Creates a new fixed value instrumentation.
     *
     * @param assigner            The assigner to use for assigning the fixed value to the return type of the instrumented value.
     * @param considerRuntimeType If {@code true}, the runtime type of the given value will be considered for assigning
     *                            the return type.
     */
    protected FixedValue(Assigner assigner, boolean considerRuntimeType) {
        this.assigner = assigner;
        this.considerRuntimeType = considerRuntimeType;
    }

    /**
     * Blueprint method that for applying the actual instrumentation.
     *
     * @param methodVisitor           The method visitor to which the instrumentation is applied to.
     * @param instrumentationContext  The instrumentation context for the given instrumentation.
     * @param instrumentedMethod      The instrumented method that is target of the instrumentation.
     * @param fixedValueType          A description of the type of the fixed value that is loaded by the
     *                                {@code valueLoadingInstruction}.
     * @param valueLoadingInstruction A stack manipulation that represents the loading of the fixed value onto the
     *                                operand stack.
     * @return A representation of the stack and variable array sized that are required for this instrumentation.
     */
    protected ByteCodeAppender.Size apply(MethodVisitor methodVisitor,
                                          Context instrumentationContext,
                                          MethodDescription instrumentedMethod,
                                          TypeDescription fixedValueType,
                                          StackManipulation valueLoadingInstruction) {
        StackManipulation assignment = assigner.assign(fixedValueType, instrumentedMethod.getReturnType(), considerRuntimeType);
        if (!assignment.isValid()) {
            throw new IllegalArgumentException("Cannot return value of type " + fixedValueType + " for " + instrumentedMethod);
        }
        StackManipulation.Size stackSize = new StackManipulation.Compound(
                valueLoadingInstruction,
                assignment,
                MethodReturn.returning(instrumentedMethod.getReturnType())
        ).apply(methodVisitor, instrumentationContext);
        return new ByteCodeAppender.Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        FixedValue that = (FixedValue) other;
        return considerRuntimeType == that.considerRuntimeType && assigner.equals(that.assigner);
    }

    @Override
    public int hashCode() {
        return 31 * assigner.hashCode() + (considerRuntimeType ? 1 : 0);
    }
}
