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
import net.bytebuddy.utility.JavaInstance;
import net.bytebuddy.utility.JavaType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.utility.ByteBuddyCommons.isValidIdentifier;
import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

/**
 * This instrumentation returns a fixed value for a method. Other than the
 * {@link net.bytebuddy.instrumentation.StubMethod} instrumentation, this implementation allows
 * to determine a specific value which must be assignable to the returning value of any instrumented method. Otherwise,
 * an exception will be thrown.
 */
public abstract class FixedValue implements Instrumentation {

    /**
     * The assigner that is used for assigning the fixed value to a method's return type.
     */
    protected final Assigner assigner;

    /**
     * Determines if the runtime type of a fixed value should be considered for the assignment to a return type.
     */
    protected final boolean dynamicallyTyped;

    /**
     * Creates a new fixed value instrumentation.
     *
     * @param assigner         The assigner to use for assigning the fixed value to the return type of the instrumented value.
     * @param dynamicallyTyped If {@code true}, the runtime type of the given value will be considered for assigning
     *                         the return type.
     */
    protected FixedValue(Assigner assigner, boolean dynamicallyTyped) {
        this.assigner = assigner;
        this.dynamicallyTyped = dynamicallyTyped;
    }

    /**
     * Creates a fixed value instrumentation that returns {@code null} as a fixed value. This value is inlined into
     * the method and does not create a field.
     *
     * @return An instrumentation that represents the {@code null} value.
     */
    public static Instrumentation nullValue() {
        return value((Object) null);
    }

    /**
     * Creates a fixed value instrumentation that returns a fixed value. If the value can be inlined into the created
     * class, i.e. can be added to the constant pool of a class, no explicit type initialization will be required for
     * the created dynamic class. Otherwise, a static field will be created in the dynamic type which will be initialized
     * with the given value. The following Java types can be inlined:
     * <ul>
     * <li>The {@link java.lang.String} type.</li>
     * <li>The {@link java.lang.Class} type.</li>
     * <li>All primitive types and their wrapper type, i.e. {@link java.lang.Boolean}, {@link java.lang.Byte},
     * {@link java.lang.Short}, {@link java.lang.Character}, {@link java.lang.Integer}, {@link java.lang.Long},
     * {@link java.lang.Float} and {@link java.lang.Double}.</li>
     * <li>A {@code null} reference.</li>
     * <li>From Java 7 on, instances of {@code java.lang.invoke.MethodType} and {@code java.lang.invoke.MethodHandle}.</li>
     * </ul>
     * <p>&nbsp;</p>
     * If possible, the constant pool value is substituted by a byte code instruction that creates the value. (This is
     * possible for integer types and types that are presented by integers inside the JVM ({@code boolean}, {@code byte},
     * {@code short}, {@code char}) and for the {@code null} value. Additionally, several common constants of
     * the {@code float}, {@code double} and {@code long} types can be represented by opcode constants.
     * <p>&nbsp;</p>
     * Note that method handles or (method) types require to be visible to a class's class loader.
     *
     * @param fixedValue The fixed value to be returned by methods that are instrumented by this instrumentation.
     * @return An instrumentation for the given {@code fixedValue}.
     */
    public static AssignerConfigurable value(Object fixedValue) {
        if (fixedValue == null) {
            return new ForPoolValue(NullConstant.INSTANCE,
                    TypeDescription.OBJECT,
                    defaultAssigner(),
                    true);
        }
        Class<?> type = fixedValue.getClass();
        if (type == String.class) {
            return new ForPoolValue(new TextConstant((String) fixedValue),
                    TypeDescription.STRING,
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else if (type == Class.class) {
            return new ForPoolValue(ClassConstant.of(new TypeDescription.ForLoadedType(type)),
                    TypeDescription.CLASS,
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else if (type == Boolean.class) {
            return new ForPoolValue(IntegerConstant.forValue((Boolean) fixedValue),
                    new TypeDescription.ForLoadedType(boolean.class),
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else if (type == Byte.class) {
            return new ForPoolValue(IntegerConstant.forValue((Byte) fixedValue),
                    new TypeDescription.ForLoadedType(byte.class),
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else if (type == Short.class) {
            return new ForPoolValue(IntegerConstant.forValue((Short) fixedValue),
                    new TypeDescription.ForLoadedType(short.class),
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else if (type == Character.class) {
            return new ForPoolValue(IntegerConstant.forValue((Character) fixedValue),
                    new TypeDescription.ForLoadedType(char.class),
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else if (type == Integer.class) {
            return new ForPoolValue(IntegerConstant.forValue((Integer) fixedValue),
                    new TypeDescription.ForLoadedType(int.class),
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else if (type == Long.class) {
            return new ForPoolValue(LongConstant.forValue((Long) fixedValue),
                    new TypeDescription.ForLoadedType(long.class),
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else if (type == Float.class) {
            return new ForPoolValue(FloatConstant.forValue((Float) fixedValue),
                    new TypeDescription.ForLoadedType(float.class),
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else if (type == Double.class) {
            return new ForPoolValue(DoubleConstant.forValue((Double) fixedValue),
                    new TypeDescription.ForLoadedType(double.class),
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else if (JavaType.METHOD_HANDLE.getTypeStub().isAssignableFrom(type)) {
            return new ForPoolValue(MethodHandleConstant.of(JavaInstance.MethodHandle.of(fixedValue)),
                    new TypeDescription.ForLoadedType(type),
                    defaultAssigner(),
                    defaultConsiderRuntimeType());
        } else if (JavaType.METHOD_TYPE.getTypeStub().represents(type)) {
            return new ForPoolValue(MethodTypeConstant.of(JavaInstance.MethodType.of(fixedValue)),
                    new TypeDescription.ForLoadedType(type),
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
                    TypeDescription.OBJECT,
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
        return new ForStaticField(isValidIdentifier(fieldName), fixedValue, defaultAssigner(), defaultConsiderRuntimeType());
    }

    /**
     * Returns the given type in form of a loaded type. The value is loaded from the written class's constant pool.
     *
     * @param fixedValue The type to return from the method.
     * @return An instrumentation for the given {@code fixedValue}.
     */
    public static AssignerConfigurable value(TypeDescription fixedValue) {
        return new ForPoolValue(ClassConstant.of(fixedValue),
                TypeDescription.CLASS,
                defaultAssigner(),
                defaultConsiderRuntimeType());
    }

    /**
     * Returns the loaded version of the given {@link JavaInstance}. The value is loaded from the written class's constant pool.
     *
     * @param fixedValue The type to return from the method.
     * @return An instrumentation for the given {@code fixedValue}.
     */
    public static AssignerConfigurable value(JavaInstance fixedValue) {
        return new ForPoolValue(fixedValue.asStackManipulation(),
                fixedValue.getInstanceType(),
                defaultAssigner(),
                defaultConsiderRuntimeType());
    }

    /**
     * Returns the default assigner that is to be used if no other assigner was explicitly specified.
     *
     * @return The default assigner that is to be used if no other assigner was explicitly specified.
     */
    private static Assigner defaultAssigner() {
        return new VoidAwareAssigner(new PrimitiveTypeAwareAssigner(ReferenceTypeAwareAssigner.INSTANCE));
    }

    /**
     * Determines if the runtime type should be considered by the given assigner by default.
     *
     * @return {@code true} if the runtime type should be considered by the given assigner by default.
     */
    private static boolean defaultConsiderRuntimeType() {
        return false;
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
        StackManipulation assignment = assigner.assign(fixedValueType, instrumentedMethod.getReturnType(),
                dynamicallyTyped);
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
        return this == other || !(other == null || getClass() != other.getClass())
                && dynamicallyTyped == ((FixedValue) other).dynamicallyTyped
                && assigner.equals(((FixedValue) other).assigner);
    }

    @Override
    public int hashCode() {
        return 31 * assigner.hashCode() + (dynamicallyTyped ? 1 : 0);
    }

    /**
     * Represents a fixed value instrumentation that is using a default assigner for attempting to assign
     * the fixed value to the return type of the instrumented method.
     */
    public interface AssignerConfigurable extends Instrumentation {

        /**
         * Defines an explicit assigner to this fixed value instrumentation.
         *
         * @param assigner         The assigner to use for assigning the fixed value to the return type of the
         *                         instrumented value.
         * @param dynamicallyTyped If {@code true}, the runtime type of the given value will be considered for
         *                         assigning the return type.
         * @return A fixed value instrumentation that makes use of the given assigner.
         */
        Instrumentation withAssigner(Assigner assigner, boolean dynamicallyTyped);
    }

    /**
     * A fixed value instrumentation that represents its fixed value as a value that is written to the instrumented
     * class's constant pool.
     */
    protected static class ForPoolValue extends FixedValue implements AssignerConfigurable, ByteCodeAppender {

        /**
         * The stack manipulation which is responsible for loading the fixed value onto the operand stack.
         */
        private final StackManipulation valueLoadInstruction;

        /**
         * The type of the fixed value.
         */
        private final TypeDescription loadedType;

        /**
         * Creates a new constant pool fixed value instrumentation.
         *
         * @param valueLoadInstruction The instruction that is responsible for loading the constant pool value onto the
         *                             operand stack.
         * @param loadedType           A type description representing the loaded type.
         * @param assigner             The assigner to use for assigning the fixed value to the return type of the
         *                             instrumented value.
         * @param dynamicallyTyped     If {@code true}, the runtime type of the given value will be considered for
         *                             assigning the return type.
         */
        private ForPoolValue(StackManipulation valueLoadInstruction,
                             TypeDescription loadedType,
                             Assigner assigner,
                             boolean dynamicallyTyped) {
            super(assigner, dynamicallyTyped);
            this.valueLoadInstruction = valueLoadInstruction;
            this.loadedType = loadedType;
        }

        @Override
        public Instrumentation withAssigner(Assigner assigner, boolean dynamicallyTyped) {
            return new ForPoolValue(valueLoadInstruction, loadedType, nonNull(assigner), dynamicallyTyped);
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(Target instrumentationTarget) {
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
                    && super.equals(other)
                    && loadedType.equals(((ForPoolValue) other).loadedType)
                    && valueLoadInstruction.equals(((ForPoolValue) other).valueLoadInstruction);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + valueLoadInstruction.hashCode();
            result = 31 * result + loadedType.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "FixedValue.ForPoolValue{" +
                    "valueLoadInstruction=" + valueLoadInstruction +
                    ", loadedType=" + loadedType +
                    ", assigner=" + assigner +
                    ", dynamicallyTyped=" + dynamicallyTyped +
                    '}';
        }
    }

    /**
     * A fixed value instrumentation that represents its fixed value as a static field of the instrumented class.
     */
    protected static class ForStaticField extends FixedValue implements AssignerConfigurable {

        /**
         * The prefix of the static field that is created for storing the fixed value.
         */
        private static final String PREFIX = "fixedValue";

        /**
         * The name of the field in which the fixed value is stored.
         */
        private final String fieldName;

        /**
         * The value that is to be stored in the static field.
         */
        private final Object fixedValue;

        /**
         * The type if the field for storing the fixed value.
         */
        private final TypeDescription fieldType;

        /**
         * Creates a new static field fixed value instrumentation with a random name for the field containing the fixed
         * value.
         *
         * @param fixedValue       The fixed value to be returned.
         * @param assigner         The assigner to use for assigning the fixed value to the return type of the
         *                         instrumented value.
         * @param dynamicallyTyped If {@code true}, the runtime type of the given value will be considered for
         *                         assigning the return type.
         */
        protected ForStaticField(Object fixedValue, Assigner assigner, boolean dynamicallyTyped) {
            this(String.format("%s$%d", PREFIX, Math.abs(fixedValue.hashCode())), fixedValue, assigner, dynamicallyTyped);
        }

        /**
         * Creates a new static field fixed value instrumentation.
         *
         * @param fieldName        The name of the field for storing the fixed value.
         * @param fixedValue       The fixed value to be returned.
         * @param assigner         The assigner to use for assigning the fixed value to the return type of the
         *                         instrumented value.
         * @param dynamicallyTyped If {@code true}, the runtime type of the given value will be considered for
         *                         assigning the return type.
         */
        protected ForStaticField(String fieldName, Object fixedValue, Assigner assigner, boolean dynamicallyTyped) {
            super(assigner, dynamicallyTyped);
            this.fieldName = fieldName;
            this.fixedValue = fixedValue;
            fieldType = new TypeDescription.ForLoadedType(fixedValue.getClass());
        }

        @Override
        public Instrumentation withAssigner(Assigner assigner, boolean dynamicallyTyped) {
            return new ForStaticField(fieldName, fixedValue, nonNull(assigner), dynamicallyTyped);
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType
                    .withField(fieldName, fieldType, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC)
                    .withInitializer(LoadedTypeInitializer.ForStaticField.nonAccessible(fieldName, fixedValue));
        }

        @Override
        public ByteCodeAppender appender(Target instrumentationTarget) {
            return new StaticFieldByteCodeAppender(instrumentationTarget.getTypeDescription());
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
                    ", fieldType=" + fieldType +
                    ", fixedValue=" + fixedValue +
                    ", assigner=" + assigner +
                    ", dynamicallyTyped=" + dynamicallyTyped +
                    '}';
        }

        /**
         * A byte code appender for returning the fixed value that was stored in a static field.
         */
        private class StaticFieldByteCodeAppender implements ByteCodeAppender {

            /**
             * The stack manipulation that loads the fixed value onto the operand stack.
             */
            private final StackManipulation fieldGetAccess;

            /**
             * Creates a new byte code appender for returning a value of a static field from an instrumented method.
             *
             * @param instrumentedType The instrumented type that is subject of the instrumentation.
             */
            private StaticFieldByteCodeAppender(TypeDescription instrumentedType) {
                fieldGetAccess = FieldAccess.forField(instrumentedType.getDeclaredFields()
                        .filter((named(fieldName))).getOnly()).getter();
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
    }
}
