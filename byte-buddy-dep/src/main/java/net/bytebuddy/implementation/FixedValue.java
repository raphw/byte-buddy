package net.bytebuddy.implementation;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.*;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.utility.JavaInstance;
import net.bytebuddy.utility.JavaType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.utility.ByteBuddyCommons.isValidIdentifier;
import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

/**
 * This implementation returns a fixed value for a method. Other than the
 * {@link net.bytebuddy.implementation.StubMethod} implementation, this implementation allows
 * to determine a specific value which must be assignable to the returning value of any instrumented method. Otherwise,
 * an exception will be thrown.
 */
public abstract class FixedValue implements Implementation {

    /**
     * The assigner that is used for assigning the fixed value to a method's return type.
     */
    protected final Assigner assigner;

    /**
     * Indicates if dynamic type castings should be attempted for incompatible assignments.
     */
    protected final Assigner.Typing typing;

    /**
     * Creates a new fixed value implementation.
     *
     * @param assigner The assigner to use for assigning the fixed value to the return type of the instrumented value.
     * @param typing   Indicates if dynamic type castings should be attempted for incompatible assignments.
     */
    protected FixedValue(Assigner assigner, Assigner.Typing typing) {
        this.assigner = assigner;
        this.typing = typing;
    }

    /**
     * Creates a fixed value implementation that returns {@code null} as a fixed value. This value is inlined into
     * the method and does not create a field.
     *
     * @return An implementation that represents the {@code null} value.
     */
    public static Implementation nullValue() {
        return value((Object) null);
    }

    /**
     * Creates a fixed value implementation that returns a fixed value. If the value can be inlined into the created
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
     * @param fixedValue The fixed value to be returned by methods that are instrumented by this implementation.
     * @return An implementation for the given {@code fixedValue}.
     */
    public static AssignerConfigurable value(Object fixedValue) {
        if (fixedValue == null) {
            return new ForPoolValue(NullConstant.INSTANCE,
                    TypeDescription.OBJECT,
                    Assigner.DEFAULT,
                    Assigner.Typing.DYNAMIC);
        }
        Class<?> type = fixedValue.getClass();
        if (type == String.class) {
            return new ForPoolValue(new TextConstant((String) fixedValue),
                    TypeDescription.STRING,
                    Assigner.DEFAULT,
                    Assigner.Typing.STATIC);
        } else if (type == Class.class) {
            return new ForPoolValue(ClassConstant.of(new TypeDescription.ForLoadedType((Class<?>) fixedValue)),
                    TypeDescription.CLASS,
                    Assigner.DEFAULT,
                    Assigner.Typing.STATIC);
        } else if (type == Boolean.class) {
            return new ForPoolValue(IntegerConstant.forValue((Boolean) fixedValue),
                    new TypeDescription.ForLoadedType(boolean.class),
                    Assigner.DEFAULT,
                    Assigner.Typing.STATIC);
        } else if (type == Byte.class) {
            return new ForPoolValue(IntegerConstant.forValue((Byte) fixedValue),
                    new TypeDescription.ForLoadedType(byte.class),
                    Assigner.DEFAULT,
                    Assigner.Typing.STATIC);
        } else if (type == Short.class) {
            return new ForPoolValue(IntegerConstant.forValue((Short) fixedValue),
                    new TypeDescription.ForLoadedType(short.class),
                    Assigner.DEFAULT,
                    Assigner.Typing.STATIC);
        } else if (type == Character.class) {
            return new ForPoolValue(IntegerConstant.forValue((Character) fixedValue),
                    new TypeDescription.ForLoadedType(char.class),
                    Assigner.DEFAULT,
                    Assigner.Typing.STATIC);
        } else if (type == Integer.class) {
            return new ForPoolValue(IntegerConstant.forValue((Integer) fixedValue),
                    new TypeDescription.ForLoadedType(int.class),
                    Assigner.DEFAULT,
                    Assigner.Typing.STATIC);
        } else if (type == Long.class) {
            return new ForPoolValue(LongConstant.forValue((Long) fixedValue),
                    new TypeDescription.ForLoadedType(long.class),
                    Assigner.DEFAULT,
                    Assigner.Typing.STATIC);
        } else if (type == Float.class) {
            return new ForPoolValue(FloatConstant.forValue((Float) fixedValue),
                    new TypeDescription.ForLoadedType(float.class),
                    Assigner.DEFAULT,
                    Assigner.Typing.STATIC);
        } else if (type == Double.class) {
            return new ForPoolValue(DoubleConstant.forValue((Double) fixedValue),
                    new TypeDescription.ForLoadedType(double.class),
                    Assigner.DEFAULT,
                    Assigner.Typing.STATIC);
        } else if (JavaType.METHOD_HANDLE.getTypeStub().isAssignableFrom(type)) {
            return new ForPoolValue(MethodHandleConstant.of(JavaInstance.MethodHandle.of(fixedValue)),
                    new TypeDescription.ForLoadedType(type),
                    Assigner.DEFAULT,
                    Assigner.Typing.STATIC);
        } else if (JavaType.METHOD_TYPE.getTypeStub().represents(type)) {
            return new ForPoolValue(MethodTypeConstant.of(JavaInstance.MethodType.of(fixedValue)),
                    new TypeDescription.ForLoadedType(type),
                    Assigner.DEFAULT,
                    Assigner.Typing.STATIC);
        } else {
            return reference(fixedValue);
        }
    }

    /**
     * Other than {@link net.bytebuddy.implementation.FixedValue#value(Object)}, this function
     * will create a fixed value implementation that will always defined a field in the instrumented class. As a result,
     * object identity will be preserved between the given {@code fixedValue} and the value that is returned by
     * instrumented methods.
     * <p>&nbsp;</p>
     * As an exception, the {@code null} value is always presented by a constant value and is never stored in a static
     * field.
     *
     * @param fixedValue The fixed value to be returned by methods that are instrumented by this implementation.
     * @return An implementation for the given {@code fixedValue}.
     */
    public static AssignerConfigurable reference(Object fixedValue) {
        return fixedValue == null
                ? new ForPoolValue(NullConstant.INSTANCE, TypeDescription.OBJECT, Assigner.DEFAULT, Assigner.Typing.DYNAMIC)
                : new ForStaticField(fixedValue, Assigner.DEFAULT, Assigner.Typing.STATIC);
    }

    /**
     * Other than {@link net.bytebuddy.implementation.FixedValue#value(Object)}, this function
     * will create a fixed value implementation that will always defined a field in the instrumented class. As a result,
     * object identity will be preserved between the given {@code fixedValue} and the value that is returned by
     * instrumented methods. The field name can be explicitly determined.
     * <p>&nbsp;</p>
     * As an exception, the {@code null} value cannot be used for this implementation but will cause an exception.
     *
     * @param fixedValue The fixed value to be returned by methods that are instrumented by this implementation.
     * @param fieldName  The name of the field for storing the fixed value.
     * @return An implementation for the given {@code fixedValue}.
     */
    public static AssignerConfigurable reference(Object fixedValue, String fieldName) {
        if (fixedValue == null) {
            throw new IllegalArgumentException("The fixed value must not be null");
        }
        return new ForStaticField(isValidIdentifier(fieldName), fixedValue, Assigner.DEFAULT, Assigner.Typing.STATIC);
    }

    /**
     * Returns the given type in form of a loaded type. The value is loaded from the written class's constant pool.
     *
     * @param fixedValue The type to return from the method.
     * @return An implementation for the given {@code fixedValue}.
     */
    public static AssignerConfigurable value(TypeDescription fixedValue) {
        return new ForPoolValue(ClassConstant.of(fixedValue),
                TypeDescription.CLASS,
                Assigner.DEFAULT,
                Assigner.Typing.STATIC);
    }

    /**
     * Returns the loaded version of the given {@link JavaInstance}. The value is loaded from the written class's constant pool.
     *
     * @param fixedValue The type to return from the method.
     * @return An implementation for the given {@code fixedValue}.
     */
    public static AssignerConfigurable value(JavaInstance fixedValue) {
        return new ForPoolValue(fixedValue.asStackManipulation(),
                fixedValue.getInstanceType(),
                Assigner.DEFAULT,
                Assigner.Typing.STATIC);
    }

    /**
     * Blueprint method that for applying the actual implementation.
     *
     * @param methodVisitor           The method visitor to which the implementation is applied to.
     * @param implementationContext   The implementation context for the given implementation.
     * @param instrumentedMethod      The instrumented method that is target of the implementation.
     * @param fixedValueType          A description of the type of the fixed value that is loaded by the
     *                                {@code valueLoadingInstruction}.
     * @param valueLoadingInstruction A stack manipulation that represents the loading of the fixed value onto the
     *                                operand stack.
     * @return A representation of the stack and variable array sized that are required for this implementation.
     */
    protected ByteCodeAppender.Size apply(MethodVisitor methodVisitor,
                                          Context implementationContext,
                                          MethodDescription instrumentedMethod,
                                          TypeDescription fixedValueType,
                                          StackManipulation valueLoadingInstruction) {
        StackManipulation assignment = assigner.assign(fixedValueType, instrumentedMethod.getReturnType().asErasure(), typing);
        if (!assignment.isValid()) {
            throw new IllegalArgumentException("Cannot return value of type " + fixedValueType + " for " + instrumentedMethod);
        }
        StackManipulation.Size stackSize = new StackManipulation.Compound(
                valueLoadingInstruction,
                assignment,
                MethodReturn.returning(instrumentedMethod.getReturnType().asErasure())
        ).apply(methodVisitor, implementationContext);
        return new ByteCodeAppender.Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && typing == ((FixedValue) other).typing
                && assigner.equals(((FixedValue) other).assigner);
    }

    @Override
    public int hashCode() {
        return 31 * assigner.hashCode() + typing.hashCode();
    }

    /**
     * Represents a fixed value implementation that is using a default assigner for attempting to assign
     * the fixed value to the return type of the instrumented method.
     */
    public interface AssignerConfigurable extends Implementation {

        /**
         * Defines an explicit assigner to this fixed value implementation.
         *
         * @param assigner The assigner to use for assigning the fixed value to the return type of the
         *                 instrumented value.
         * @param typing   Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @return A fixed value implementation that makes use of the given assigner.
         */
        Implementation withAssigner(Assigner assigner, Assigner.Typing typing);
    }

    /**
     * A fixed value implementation that represents its fixed value as a value that is written to the instrumented
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
         * Creates a new constant pool fixed value implementation.
         *
         * @param valueLoadInstruction The instruction that is responsible for loading the constant pool value onto the
         *                             operand stack.
         * @param loadedType           A type description representing the loaded type.
         * @param assigner             The assigner to use for assigning the fixed value to the return type of the
         *                             instrumented value.
         * @param typing               Indicates if dynamic type castings should be attempted for incompatible assignments.
         */
        private ForPoolValue(StackManipulation valueLoadInstruction, TypeDescription loadedType, Assigner assigner, Assigner.Typing typing) {
            super(assigner, typing);
            this.valueLoadInstruction = valueLoadInstruction;
            this.loadedType = loadedType;
        }

        @Override
        public Implementation withAssigner(Assigner assigner, Assigner.Typing typing) {
            return new ForPoolValue(valueLoadInstruction, loadedType, nonNull(assigner), nonNull(typing));
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return this;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            return apply(methodVisitor, implementationContext, instrumentedMethod, loadedType, valueLoadInstruction);
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
                    ", typing=" + typing +
                    '}';
        }
    }

    /**
     * A fixed value implementation that represents its fixed value as a static field of the instrumented class.
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
         * Creates a new static field fixed value implementation with a random name for the field containing the fixed
         * value.
         *
         * @param fixedValue The fixed value to be returned.
         * @param assigner   The assigner to use for assigning the fixed value to the return type of the
         *                   instrumented value.
         * @param typing     Indicates if dynamic type castings should be attempted for incompatible assignments.
         */
        protected ForStaticField(Object fixedValue, Assigner assigner, Assigner.Typing typing) {
            this(String.format("%s$%d", PREFIX, Math.abs(fixedValue.hashCode() % Integer.MAX_VALUE)), fixedValue, assigner, typing);
        }

        /**
         * Creates a new static field fixed value implementation.
         *
         * @param fieldName  The name of the field for storing the fixed value.
         * @param fixedValue The fixed value to be returned.
         * @param assigner   The assigner to use for assigning the fixed value to the return type of the
         *                   instrumented value.
         * @param typing     Indicates if dynamic type castings should be attempted for incompatible assignments.
         */
        protected ForStaticField(String fieldName, Object fixedValue, Assigner assigner, Assigner.Typing typing) {
            super(assigner, typing);
            this.fieldName = fieldName;
            this.fixedValue = fixedValue;
            fieldType = new TypeDescription.ForLoadedType(fixedValue.getClass());
        }

        @Override
        public Implementation withAssigner(Assigner assigner, Assigner.Typing typing) {
            return new ForStaticField(fieldName, fixedValue, nonNull(assigner), typing);
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType
                    .withField(new FieldDescription.Token(fieldName, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, fieldType))
                    .withInitializer(LoadedTypeInitializer.ForStaticField.nonAccessible(fieldName, fixedValue));
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new StaticFieldByteCodeAppender(implementationTarget.getTypeDescription());
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
                    ", typing=" + typing +
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
                fieldGetAccess = FieldAccess.forField(instrumentedType.getDeclaredFields().filter((named(fieldName))).getOnly()).getter();
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                return ForStaticField.this.apply(methodVisitor, implementationContext, instrumentedMethod, fieldType, fieldGetAccess);
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
