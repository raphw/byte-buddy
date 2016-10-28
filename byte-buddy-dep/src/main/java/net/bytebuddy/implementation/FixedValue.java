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
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * This implementation returns a fixed value for a method. Other than the {@link net.bytebuddy.implementation.StubMethod}
 * implementation, this implementation allows to determine a specific value which must be assignable to the returning value
 * of any instrumented method. Otherwise, an exception will be thrown.
 *
 * @see FieldAccessor
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
     * <p>
     * Returns a fixed value from any intercepted method. The fixed value is stored in the constant pool if this is possible.
     * Java is capable of storing any primitive value, {@link String} values and {@link Class} references in the constant pool.
     * Since Java 7, {@code MethodHandle} as well as {@code MethodType} references are also supported. Alternatively, the fixed
     * value is stored in a static field.
     * </p>
     * <p>
     * When a value is stored in the class's constant pool, its identity is lost. If an object's identity is important, the
     * {@link FixedValue#reference(Object)} method should be used instead.
     * </p>
     * <p>
     * <b>Important</b>: When supplying a method handle or a method type, all types that are implied must be visible to the instrumented
     * type or an {@link IllegalAccessException} will be thrown at runtime.
     * </p>
     *
     * @param fixedValue The fixed value to return from the method.
     * @return An implementation for the given {@code fixedValue}.
     */
    public static AssignerConfigurable value(Object fixedValue) {
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
            return new ForPoolValue(new JavaConstantValue(JavaConstant.MethodHandle.ofLoaded(fixedValue)),
                    new TypeDescription.ForLoadedType(type),
                    Assigner.DEFAULT,
                    Assigner.Typing.STATIC);
        } else if (JavaType.METHOD_TYPE.getTypeStub().represents(type)) {
            return new ForPoolValue(new JavaConstantValue(JavaConstant.MethodType.ofLoaded(fixedValue)),
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
     * instrumented methods. The field name can be explicitly determined. The field name is generated from the fixed value's
     * hash code.
     *
     * @param fixedValue The fixed value to be returned by methods that are instrumented by this implementation.
     * @return An implementation for the given {@code fixedValue}.
     */
    public static AssignerConfigurable reference(Object fixedValue) {
        return new ForStaticField(fixedValue, Assigner.DEFAULT, Assigner.Typing.STATIC);
    }

    /**
     * Other than {@link net.bytebuddy.implementation.FixedValue#value(Object)}, this function
     * will create a fixed value implementation that will always defined a field in the instrumented class. As a result,
     * object identity will be preserved between the given {@code fixedValue} and the value that is returned by
     * instrumented methods. The field name can be explicitly determined.
     *
     * @param fixedValue The fixed value to be returned by methods that are instrumented by this implementation.
     * @param fieldName  The name of the field for storing the fixed value.
     * @return An implementation for the given {@code fixedValue}.
     */
    public static AssignerConfigurable reference(Object fixedValue, String fieldName) {
        return new ForStaticField(fieldName, fixedValue, Assigner.DEFAULT, Assigner.Typing.STATIC);
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
     * Returns the loaded version of the given {@link JavaConstant}. The value is loaded from the written class's constant pool.
     *
     * @param fixedValue The type to return from the method.
     * @return An implementation for the given {@code fixedValue}.
     */
    public static AssignerConfigurable value(JavaConstant fixedValue) {
        return new ForPoolValue(fixedValue.asStackManipulation(),
                fixedValue.getType(),
                Assigner.DEFAULT,
                Assigner.Typing.STATIC);
    }

    /**
     * Returns a {@code null} value from an instrumented method.
     *
     * @return An implementation that returns {@code null} from a method.
     */
    public static Implementation nullValue() {
        return ForNullValue.INSTANCE;
    }

    /**
     * Returns {@code this} from an instrumented method.
     *
     * @return An implementation that returns {@code this} from a method.
     */
    public static Implementation self() {
        return ForThisValue.INSTANCE;
    }

    /**
     * Returns the origin type from an instrumented method.
     *
     * @return An implementation that returns the origin type of the current instrumented type.
     */
    public static AssignerConfigurable originType() {
        return new ForOriginType(Assigner.DEFAULT, Assigner.Typing.STATIC);
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
                                          TypeDescription.Generic fixedValueType,
                                          StackManipulation valueLoadingInstruction) {
        StackManipulation assignment = assigner.assign(fixedValueType, instrumentedMethod.getReturnType(), typing);
        if (!assignment.isValid()) {
            throw new IllegalArgumentException("Cannot return value of type " + fixedValueType + " for " + instrumentedMethod);
        }
        StackManipulation.Size stackSize = new StackManipulation.Compound(
                valueLoadingInstruction,
                assignment,
                MethodReturn.of(instrumentedMethod.getReturnType().asErasure())
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
     * A fixed value that appends the origin type of the instrumented type.
     */
    protected static class ForOriginType extends FixedValue implements AssignerConfigurable {

        /**
         * Creates a new fixed value appender for the origin type.
         *
         * @param assigner The assigner to use for assigning the fixed value to the return type of the instrumented value.
         * @param typing   Indicates if dynamic type castings should be attempted for incompatible assignments.
         */
        protected ForOriginType(Assigner assigner, Assigner.Typing typing) {
            super(assigner, typing);
        }

        @Override
        public Implementation withAssigner(Assigner assigner, Assigner.Typing typing) {
            return new ForOriginType(assigner, typing);
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(implementationTarget.getOriginType().asErasure());
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public String toString() {
            return "FixedValue.ForOriginType{" +
                    "assigner=" + assigner +
                    ", typing=" + typing +
                    '}';
        }

        /**
         * An appender for writing the origin type.
         */
        protected class Appender implements ByteCodeAppender {

            /**
             * The instrumented type.
             */
            private final TypeDescription originType;

            /**
             * Creates a new appender.
             *
             * @param originType The instrumented type.
             */
            protected Appender(TypeDescription originType) {
                this.originType = originType;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                return ForOriginType.this.apply(methodVisitor,
                        implementationContext,
                        instrumentedMethod,
                        TypeDescription.CLASS.asGenericType(),
                        ClassConstant.of(originType));
            }

            /**
             * Returns the outer instance.
             *
             * @return The outer instance.
             */
            private ForOriginType getOuter() {
                return ForOriginType.this;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Appender appender = (Appender) o;
                return originType.equals(appender.originType)
                        && getOuter().equals(appender.getOuter());
            }

            @Override
            public int hashCode() {
                return 31 * getOuter().hashCode() + originType.hashCode();
            }

            @Override
            public String toString() {
                return "FixedValue.ForOriginType.Appender{" +
                        "outer=" + getOuter() +
                        ", originType=" + originType +
                        '}';
            }
        }
    }

    /**
     * A fixed value of {@code null}.
     */
    protected enum ForNullValue implements Implementation, ByteCodeAppender {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return this;
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            if (instrumentedMethod.getReturnType().isPrimitive()) {
                throw new IllegalStateException("Cannot return null from " + instrumentedMethod);
            }
            return new ByteCodeAppender.Simple(
                    NullConstant.INSTANCE,
                    MethodReturn.REFERENCE
            ).apply(methodVisitor, implementationContext, instrumentedMethod);
        }

        @Override
        public String toString() {
            return "FixedValue.ForNullValue." + name();
        }
    }

    /**
     * A fixed value of {@code this}.
     */
    protected enum ForThisValue implements Implementation {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(implementationTarget.getInstrumentedType());
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public String toString() {
            return "FixedValue.ForThisValue." + name();
        }

        /**
         * A byte code appender for returning {@code this}.
         */
        protected static class Appender implements ByteCodeAppender {

            /**
             * The instrumented type.
             */
            private final TypeDescription instrumentedType;

            /**
             * Creates a new byte code appender for returning {@code this}.
             *
             * @param instrumentedType The instrumented type.
             */
            protected Appender(TypeDescription instrumentedType) {
                this.instrumentedType = instrumentedType;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                if (instrumentedMethod.isStatic() || !instrumentedType.isAssignableTo(instrumentedMethod.getReturnType().asErasure())) {
                    throw new IllegalStateException("Cannot return 'this' from " + instrumentedMethod);
                }
                return new ByteCodeAppender.Simple(
                        MethodVariableAccess.REFERENCE.loadOffset(0),
                        MethodReturn.REFERENCE
                ).apply(methodVisitor, implementationContext, instrumentedMethod);
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                Appender appender = (Appender) object;
                return instrumentedType.equals(appender.instrumentedType);
            }

            @Override
            public int hashCode() {
                return instrumentedType.hashCode();
            }

            @Override
            public String toString() {
                return "FixedValue.ForThisValue.Appender{" +
                        "instrumentedType=" + instrumentedType +
                        '}';
            }
        }
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
            return new ForPoolValue(valueLoadInstruction, loadedType, assigner, typing);
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
            return apply(methodVisitor, implementationContext, instrumentedMethod, loadedType.asGenericType(), valueLoadInstruction);
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
        private final TypeDescription.Generic fieldType;

        /**
         * Creates a new static field fixed value implementation with a random name for the field containing the fixed
         * value.
         *
         * @param fixedValue The fixed value to be returned.
         * @param assigner   The assigner to use for assigning the fixed value to the return type of the instrumented value.
         * @param typing     Indicates if dynamic type castings should be attempted for incompatible assignments.
         */
        protected ForStaticField(Object fixedValue, Assigner assigner, Assigner.Typing typing) {
            this(String.format("%s$%s", PREFIX, RandomString.make()), fixedValue, assigner, typing);
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
            fieldType = new TypeDescription.Generic.OfNonGenericType.ForLoadedType(fixedValue.getClass());
        }

        @Override
        public Implementation withAssigner(Assigner assigner, Assigner.Typing typing) {
            return new ForStaticField(fieldName, fixedValue, assigner, typing);
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType
                    .withField(new FieldDescription.Token(fieldName, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, fieldType))
                    .withInitializer(new LoadedTypeInitializer.ForStaticField(fieldName, fixedValue));
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new StaticFieldByteCodeAppender(implementationTarget.getInstrumentedType());
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
