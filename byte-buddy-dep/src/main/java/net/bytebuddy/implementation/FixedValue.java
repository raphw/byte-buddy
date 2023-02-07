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

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.*;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.utility.ConstantValue;
import net.bytebuddy.utility.JavaConstant;
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
@HashCodeAndEqualsPlugin.Enhance
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
     * Specifically, an argument that is a {@link JavaConstant}, {@link TypeDescription}, primitive, {@link String} or
     * {@link Class} value is stored in the constant pool.  Since Java 7, {@code MethodHandle} as well as {@code MethodType}
     * references are also supported. Alternatively, the fixed value is stored in a static field.
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
     * @param value The fixed value to return from the method.
     * @return An implementation for the given {@code value}.
     * @see #value(JavaConstant)
     * @see #value(TypeDescription)
     * @see #nullValue()
     */
    public static AssignerConfigurable value(Object value) {
        ConstantValue constant = ConstantValue.Simple.wrapOrNull(value);
        return constant == null
                ? reference(value)
                : new ForConstantValue(constant.toStackManipulation(), constant.getTypeDescription());
    }

    /**
     * Other than {@link net.bytebuddy.implementation.FixedValue#value(Object)}, this function
     * will create a fixed value implementation that will always defined a field in the instrumented class. As a result,
     * object identity will be preserved between the given {@code value} and the value that is returned by
     * instrumented methods. The field name can be explicitly determined. The field name is generated from the fixed value's
     * hash code.
     *
     * @param value The fixed value to be returned by methods that are instrumented by this implementation.
     * @return An implementation for the given {@code value}.
     */
    public static AssignerConfigurable reference(Object value) {
        return reference(value, ForValue.PREFIX + "$" + RandomString.hashOf(value));
    }

    /**
     * Other than {@link net.bytebuddy.implementation.FixedValue#value(Object)}, this function
     * will create a fixed value implementation that will always defined a field in the instrumented class. As a result,
     * object identity will be preserved between the given {@code value} and the value that is returned by
     * instrumented methods. The field name can be explicitly determined.
     *
     * @param value The fixed value to be returned by methods that are instrumented by this implementation.
     * @param name  The name of the field for storing the fixed value.
     * @return An implementation for the given {@code value}.
     */
    public static AssignerConfigurable reference(Object value, String name) {
        return new ForValue(value, name);
    }

    /**
     * Returns the given type in form of a loaded type. The value is loaded from the written class's constant pool.
     *
     * @param type The type to return from the method.
     * @return An implementation for the given {@code value}.
     */
    public static AssignerConfigurable value(TypeDescription type) {
        return new ForConstantValue(ClassConstant.of(type), TypeDescription.ForLoadedType.of(Class.class));
    }

    /**
     * Returns the loaded version of the given {@link JavaConstant}. The value is loaded from the written class's constant pool.
     *
     * @param constant The type to return from the method.
     * @return An implementation for the given {@code value}.
     */
    public static AssignerConfigurable value(ConstantValue constant) {
        return new ForConstantValue(constant.toStackManipulation(), constant.getTypeDescription());
    }

    /**
     * Returns the loaded version of the given {@link JavaConstant}. The value is loaded from the written class's constant pool.
     *
     * @param constant The type to return from the method.
     * @return An implementation for the given {@code value}.
     */
    public static AssignerConfigurable value(JavaConstant constant) {
        return value((ConstantValue) constant);
    }

    /**
     * Returns the argument at the specified index.
     *
     * @param index The index of the argument to return.
     * @return An implementation of a method that returns the argument at the specified index.
     */
    public static AssignerConfigurable argument(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Argument index cannot be negative: " + index);
        }
        return new ForArgument(index);
    }

    /**
     * Returns {@code this} from an instrumented method.
     *
     * @return An implementation that returns {@code this} from a method.
     */
    public static AssignerConfigurable self() {
        return new ForThisValue();
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
     * Returns the origin type from an instrumented method.
     *
     * @return An implementation that returns the origin type of the current instrumented type.
     */
    public static AssignerConfigurable originType() {
        return new ForOriginType();
    }

    /**
     * Blueprint method that for applying the actual implementation.
     *
     * @param methodVisitor         The method visitor to which the implementation is applied to.
     * @param implementationContext The implementation context for the given implementation.
     * @param instrumentedMethod    The instrumented method that is target of the implementation.
     * @param typeDescription       A description of the type of the fixed value that is loaded by the
     *                              {@code valueLoadingInstruction}.
     * @param stackManipulation     A stack manipulation that represents the loading of the fixed value onto the
     *                              operand stack.
     * @return A representation of the stack and variable array sized that are required for this implementation.
     */
    protected ByteCodeAppender.Size apply(MethodVisitor methodVisitor,
                                          Context implementationContext,
                                          MethodDescription instrumentedMethod,
                                          TypeDescription.Generic typeDescription,
                                          StackManipulation stackManipulation) {
        StackManipulation assignment = assigner.assign(typeDescription, instrumentedMethod.getReturnType(), typing);
        if (!assignment.isValid()) {
            throw new IllegalArgumentException("Cannot return value of type " + typeDescription + " for " + instrumentedMethod);
        }
        StackManipulation.Size stackSize = new StackManipulation.Compound(
                stackManipulation,
                assignment,
                MethodReturn.of(instrumentedMethod.getReturnType())
        ).apply(methodVisitor, implementationContext);
        return new ByteCodeAppender.Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
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
     * A fixed value of {@code null}.
     */
    protected enum ForNullValue implements Implementation, ByteCodeAppender {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public ByteCodeAppender appender(Target implementationTarget) {
            return this;
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
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            if (instrumentedMethod.getReturnType().isPrimitive()) {
                throw new IllegalStateException("Cannot return null from " + instrumentedMethod);
            }
            return new ByteCodeAppender.Simple(
                    NullConstant.INSTANCE,
                    MethodReturn.REFERENCE
            ).apply(methodVisitor, implementationContext, instrumentedMethod);
        }
    }

    /**
     * A fixed value that appends the origin type of the instrumented type.
     */
    protected static class ForOriginType extends FixedValue implements AssignerConfigurable {

        /**
         * Creates a new fixed value appender for the origin type.
         */
        protected ForOriginType() {
            this(Assigner.DEFAULT, Assigner.Typing.STATIC);
        }

        /**
         * Creates a new fixed value appender for the origin type.
         *
         * @param assigner The assigner to use for assigning the fixed value to the return type of the instrumented value.
         * @param typing   Indicates if dynamic type castings should be attempted for incompatible assignments.
         */
        private ForOriginType(Assigner assigner, Assigner.Typing typing) {
            super(assigner, typing);
        }

        /**
         * {@inheritDoc}
         */
        public Implementation withAssigner(Assigner assigner, Assigner.Typing typing) {
            return new ForOriginType(assigner, typing);
        }

        /**
         * {@inheritDoc}
         */
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(implementationTarget.getOriginType().asErasure());
        }

        /**
         * {@inheritDoc}
         */
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        /**
         * An appender for writing the origin type.
         */
        @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
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

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                return ForOriginType.this.apply(methodVisitor,
                        implementationContext,
                        instrumentedMethod,
                        TypeDescription.ForLoadedType.of(Class.class).asGenericType(),
                        ClassConstant.of(originType));
            }
        }
    }

    /**
     * A fixed value of {@code this}.
     */
    protected static class ForThisValue extends FixedValue implements AssignerConfigurable {

        /**
         * Creates an implementation that returns the instance of the instrumented type.
         */
        protected ForThisValue() {
            super(Assigner.DEFAULT, Assigner.Typing.STATIC);
        }

        /**
         * Creates an implementation that returns the instance of the instrumented type.
         *
         * @param assigner The assigner to use for assigning the fixed value to the return type of the instrumented value.
         * @param typing   Indicates if dynamic type castings should be attempted for incompatible assignments.
         */
        private ForThisValue(Assigner assigner, Assigner.Typing typing) {
            super(assigner, typing);
        }

        /**
         * {@inheritDoc}
         */
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(implementationTarget.getInstrumentedType());
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
        public Implementation withAssigner(Assigner assigner, Assigner.Typing typing) {
            return new ForThisValue(assigner, typing);
        }

        /**
         * A byte code appender for returning {@code this}.
         */
        @HashCodeAndEqualsPlugin.Enhance
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

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                if (instrumentedMethod.isStatic() || !instrumentedType.isAssignableTo(instrumentedMethod.getReturnType().asErasure())) {
                    throw new IllegalStateException("Cannot return 'this' from " + instrumentedMethod);
                }
                return new ByteCodeAppender.Simple(
                        MethodVariableAccess.loadThis(),
                        MethodReturn.REFERENCE
                ).apply(methodVisitor, implementationContext, instrumentedMethod);
            }
        }
    }

    /**
     * A fixed value implementation that returns a method's argument.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class ForArgument extends FixedValue implements AssignerConfigurable, ByteCodeAppender {

        /**
         * The argument index.
         */
        private final int index;

        /**
         * Creates a new fixed value implementation that returns a method's argument.
         *
         * @param index The argument's index.
         */
        protected ForArgument(int index) {
            this(Assigner.DEFAULT, Assigner.Typing.STATIC, index);
        }

        /**
         * Creates a new fixed value implementation that returns a method's argument.
         *
         * @param assigner The assigner to use for assigning the fixed value to the return type of the
         *                 instrumented value.
         * @param typing   Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @param index    The argument's index.
         */
        private ForArgument(Assigner assigner, Assigner.Typing typing, int index) {
            super(assigner, typing);
            this.index = index;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            if (instrumentedMethod.getParameters().size() <= index) {
                throw new IllegalStateException(instrumentedMethod + " does not define a parameter with index " + index);
            }
            ParameterDescription parameterDescription = instrumentedMethod.getParameters().get(index);
            StackManipulation stackManipulation = new StackManipulation.Compound(
                    MethodVariableAccess.load(parameterDescription),
                    assigner.assign(parameterDescription.getType(), instrumentedMethod.getReturnType(), typing),
                    MethodReturn.of(instrumentedMethod.getReturnType())
            );
            if (!stackManipulation.isValid()) {
                throw new IllegalStateException("Cannot assign " + instrumentedMethod.getReturnType() + " to " + parameterDescription);
            }
            return new Size(stackManipulation.apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
        }

        /**
         * {@inheritDoc}
         */
        public ByteCodeAppender appender(Target implementationTarget) {
            return this;
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
        public Implementation withAssigner(Assigner assigner, Assigner.Typing typing) {
            return new ForArgument(assigner, typing, index);
        }
    }

    /**
     * A fixed value implementation that represents its fixed value as a constant pool value or a byte code instruction.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class ForConstantValue extends FixedValue implements AssignerConfigurable, ByteCodeAppender {

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
         */
        protected ForConstantValue(StackManipulation valueLoadInstruction, Class<?> loadedType) {
            this(valueLoadInstruction, TypeDescription.ForLoadedType.of(loadedType));
        }

        /**
         * Creates a new constant pool fixed value implementation.
         *
         * @param valueLoadInstruction The instruction that is responsible for loading the constant pool value onto the
         *                             operand stack.
         * @param loadedType           A type description representing the loaded type.
         */
        protected ForConstantValue(StackManipulation valueLoadInstruction, TypeDescription loadedType) {
            this(Assigner.DEFAULT, Assigner.Typing.STATIC, valueLoadInstruction, loadedType);
        }

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
        private ForConstantValue(Assigner assigner, Assigner.Typing typing, StackManipulation valueLoadInstruction, TypeDescription loadedType) {
            super(assigner, typing);
            this.valueLoadInstruction = valueLoadInstruction;
            this.loadedType = loadedType;
        }

        /**
         * {@inheritDoc}
         */
        public Implementation withAssigner(Assigner assigner, Assigner.Typing typing) {
            return new ForConstantValue(assigner, typing, valueLoadInstruction, loadedType);
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
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            return apply(methodVisitor, implementationContext, instrumentedMethod, loadedType.asGenericType(), valueLoadInstruction);
        }
    }

    /**
     * A fixed value implementation that represents its fixed value as a static field of the instrumented class.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class ForValue extends FixedValue implements AssignerConfigurable {

        /**
         * The prefix of the static field that is created for storing the fixed value.
         */
        private static final String PREFIX = "value";

        /**
         * The name of the field in which the fixed value is stored.
         */
        private final String name;

        /**
         * The value that is to be stored in the static field.
         */
        private final Object value;

        /**
         * Creates a new static field fixed value implementation.
         *
         * @param value The fixed value to be returned.
         * @param name  The name of the field for storing the fixed value.
         */
        protected ForValue(Object value, String name) {
            this(Assigner.DEFAULT, Assigner.Typing.STATIC, value, name);
        }

        /**
         * Creates a new static field fixed value implementation.
         *
         * @param value    The fixed value to be returned.
         * @param name     The name of the field for storing the fixed value.
         * @param assigner The assigner to use for assigning the fixed value to the return type of the
         *                 instrumented value.
         * @param typing   Indicates if dynamic type castings should be attempted for incompatible assignments.
         */
        private ForValue(Assigner assigner, Assigner.Typing typing, Object value, String name) {
            super(assigner, typing);
            this.name = name;
            this.value = value;
        }

        /**
         * {@inheritDoc}
         */
        public Implementation withAssigner(Assigner assigner, Assigner.Typing typing) {
            return new ForValue(assigner, typing, value, name);
        }

        /**
         * {@inheritDoc}
         */
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType.withAuxiliaryField(new FieldDescription.Token(name,
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE | Opcodes.ACC_SYNTHETIC,
                    TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(value.getClass())), value);
        }

        /**
         * {@inheritDoc}
         */
        public ByteCodeAppender appender(Target implementationTarget) {
            return new StaticFieldByteCodeAppender(implementationTarget.getInstrumentedType());
        }

        /**
         * A byte code appender for returning the fixed value that was stored in a static field.
         */
        @HashCodeAndEqualsPlugin.Enhance
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
                fieldGetAccess = FieldAccess.forField(instrumentedType.getDeclaredFields().filter((named(name))).getOnly()).read();
            }

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                return ForValue.this.apply(methodVisitor,
                        implementationContext,
                        instrumentedMethod,
                        TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(value.getClass()),
                        fieldGetAccess);
            }
        }
    }
}
