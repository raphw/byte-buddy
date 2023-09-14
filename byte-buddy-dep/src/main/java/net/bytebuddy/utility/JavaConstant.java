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
package net.bytebuddy.utility;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.constant.*;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import net.bytebuddy.utility.nullability.MaybeNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * Represents a constant-pool constant within a Java class file.
 */
public interface JavaConstant extends ConstantValue {

    /**
     * Returns this constant as a Java {@code java.lang.constant.ConstantDesc} if the current VM is of at least version 12.
     * If the current VM is of an older version and does not support the type, an exception is thrown.
     *
     * @return This constant as a Java {@code java.lang.constant.ConstantDesc}.
     */
    Object toDescription();

    /**
     * Applies the supplied visitor to this constant type with its respective callback.
     *
     * @param visitor The visitor to dispatch.
     * @param <T>     The type of the value that is returned by the visitor.
     * @return The value that is returned by the supplied visitor.
     */
    <T> T accept(Visitor<T> visitor);

    /**
     * A visitor to resolve a {@link JavaConstant} based on its implementation.
     *
     * @param <T> The type of the value that is returned by this visitor.
     */
    interface Visitor<T> {

        /**
         * Invoked on a {@link Simple} constant that represents itself. Such values are of type
         * {@link Integer}, {@link Long}, {@link Float}, {@link Double} or {@link String}.
         *
         * @param constant The simple constant.
         * @return The returned value.
         */
        T onValue(Simple<?> constant);

        /**
         * Invoked on a {@link Simple} constant that represents a {@link TypeDescription}.
         *
         * @param constant The simple constant.
         * @return The returned value.
         */
        T onType(Simple<TypeDescription> constant);

        /**
         * Invoked on a constant that represents a {@link MethodType}.
         *
         * @param constant The method type constant.
         * @return The returned value.
         */
        T onMethodType(MethodType constant);

        /**
         * Invoked on a constant that represents a {@link MethodHandle}.
         *
         * @param constant The method handle constant.
         * @return The returned value.
         */
        T onMethodHandle(MethodHandle constant);

        /**
         * Invoked on a {@link Dynamic} constant.
         *
         * @param constant The dynamic constant.
         * @return The returned value.
         */
        T onDynamic(Dynamic constant);

        /**
         * A non-operational implementation of a {@link Visitor} for a {@link JavaConstant}.
         */
        enum NoOp implements Visitor<JavaConstant> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public JavaConstant onValue(Simple<?> constant) {
                return constant;
            }

            /**
             * {@inheritDoc}
             */
            public JavaConstant onType(Simple<TypeDescription> constant) {
                return constant;
            }

            /**
             * {@inheritDoc}
             */
            public JavaConstant onMethodType(MethodType constant) {
                return constant;
            }

            /**
             * {@inheritDoc}
             */
            public JavaConstant onMethodHandle(MethodHandle constant) {
                return constant;
            }

            /**
             * {@inheritDoc}
             */
            public JavaConstant onDynamic(Dynamic constant) {
                return constant;
            }
        }
    }

    /**
     * Represents a simple Java constant, either a primitive constant, a {@link String} or a {@link Class}.
     *
     * @param <T> The represented type.
     */
    abstract class Simple<T> implements JavaConstant {

        /**
         * A dispatcher for interaction with {@code java.lang.constant.ClassDesc}.
         */
        protected static final Dispatcher CONSTANT_DESC = doPrivileged(JavaDispatcher.of(Dispatcher.class));

        /**
         * A dispatcher for interaction with {@code java.lang.constant.ClassDesc}.
         */
        protected static final Dispatcher.OfClassDesc CLASS_DESC = doPrivileged(JavaDispatcher.of(Dispatcher.OfClassDesc.class));

        /**
         * A dispatcher for interaction with {@code java.lang.constant.MethodTypeDesc}.
         */
        protected static final Dispatcher.OfMethodTypeDesc METHOD_TYPE_DESC = doPrivileged(JavaDispatcher.of(Dispatcher.OfMethodTypeDesc.class));

        /**
         * A dispatcher for interaction with {@code java.lang.constant.MethodHandleDesc}.
         */
        protected static final Dispatcher.OfMethodHandleDesc METHOD_HANDLE_DESC = doPrivileged(JavaDispatcher.of(Dispatcher.OfMethodHandleDesc.class));

        /**
         * A dispatcher for interaction with {@code java.lang.constant.DirectMethodHandleDesc}.
         */
        protected static final Dispatcher.OfDirectMethodHandleDesc DIRECT_METHOD_HANDLE_DESC = doPrivileged(JavaDispatcher.of(Dispatcher.OfDirectMethodHandleDesc.class));

        /**
         * A dispatcher for interaction with {@code java.lang.constant.DirectMethodHandleDesc}.
         */
        protected static final Dispatcher.OfDirectMethodHandleDesc.ForKind DIRECT_METHOD_HANDLE_DESC_KIND = doPrivileged(JavaDispatcher.of(Dispatcher.OfDirectMethodHandleDesc.ForKind.class));

        /**
         * A dispatcher for interaction with {@code java.lang.constant.DirectMethodHandleDesc}.
         */
        protected static final Dispatcher.OfDynamicConstantDesc DYNAMIC_CONSTANT_DESC = doPrivileged(JavaDispatcher.of(Dispatcher.OfDynamicConstantDesc.class));

        /**
         * The represented constant pool value.
         */
        protected final T value;

        /**
         * A description of the type of the constant.
         */
        private final TypeDescription typeDescription;

        /**
         * Creates a simple Java constant.
         *
         * @param value           The represented constant pool value.
         * @param typeDescription A description of the type of the constant.
         */
        protected Simple(T value, TypeDescription typeDescription) {
            this.value = value;
            this.typeDescription = typeDescription;
        }

        /**
         * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
         *
         * @param action The action to execute from a privileged context.
         * @param <T>    The type of the action's resolved value.
         * @return The action's resolved value.
         */
        @AccessControllerPlugin.Enhance
        private static <T> T doPrivileged(PrivilegedAction<T> action) {
            return action.run();
        }

        /**
         * Resolves a loaded Java value to a Java constant representation.
         *
         * @param value The value to represent.
         * @return An appropriate Java constant representation.
         */
        public static JavaConstant ofLoaded(Object value) {
            JavaConstant constant = ofLoadedOrNull(value);
            if (constant == null) {
                throw new IllegalArgumentException("Not a constant: " + value);
            } else {
                return constant;
            }
        }

        /**
         * Resolves a loaded Java value to a Java constant representation.
         *
         * @param value The value to represent.
         * @return An appropriate Java constant representation or {@code null} if the supplied argument is not a compile-time constant.
         */
        @MaybeNull
        protected static JavaConstant ofLoadedOrNull(Object value) {
            if (value instanceof Integer) {
                return new OfTrivialValue.ForInteger((Integer) value);
            } else if (value instanceof Long) {
                return new OfTrivialValue.ForLong((Long) value);
            } else if (value instanceof Float) {
                return new OfTrivialValue.ForFloat((Float) value);
            } else if (value instanceof Double) {
                return new OfTrivialValue.ForDouble((Double) value);
            } else if (value instanceof String) {
                return new OfTrivialValue.ForString((String) value);
            } else if (value instanceof Class<?>) {
                return JavaConstant.Simple.of(TypeDescription.ForLoadedType.of((Class<?>) value));
            } else if (JavaType.METHOD_HANDLE.isInstance(value)) {
                return MethodHandle.ofLoaded(value);
            } else if (JavaType.METHOD_TYPE.isInstance(value)) {
                return MethodType.ofLoaded(value);
            } else {
                return null;
            }
        }

        /**
         * Creates a Java constant value from a {@code java.lang.constant.ConstantDesc}.
         *
         * @param value       The  {@code java.lang.constant.ConstantDesc} to represent.
         * @param classLoader The class loader to use for resolving type information from the supplied value.
         * @return An appropriate Java constant representation.
         */
        public static JavaConstant ofDescription(Object value, @MaybeNull ClassLoader classLoader) {
            return ofDescription(value, ClassFileLocator.ForClassLoader.of(classLoader));
        }

        /**
         * Creates a Java constant value from a {@code java.lang.constant.ConstantDesc}.
         *
         * @param value            The  {@code java.lang.constant.ConstantDesc} to represent.
         * @param classFileLocator The class file locator to use for resolving type information from the supplied value.
         * @return An appropriate Java constant representation.
         */
        public static JavaConstant ofDescription(Object value, ClassFileLocator classFileLocator) {
            return ofDescription(value, TypePool.Default.WithLazyResolution.of(classFileLocator));
        }

        /**
         * Creates a Java constant value from a {@code java.lang.constant.ConstantDesc}.
         *
         * @param value    The  {@code java.lang.constant.ConstantDesc} to represent.
         * @param typePool The type pool to use for resolving type information from the supplied value.
         * @return An appropriate Java constant representation.
         */
        public static JavaConstant ofDescription(Object value, TypePool typePool) {
            if (value instanceof Integer) {
                return new JavaConstant.Simple.OfTrivialValue.ForInteger((Integer) value);
            } else if (value instanceof Long) {
                return new JavaConstant.Simple.OfTrivialValue.ForLong((Long) value);
            } else if (value instanceof Float) {
                return new JavaConstant.Simple.OfTrivialValue.ForFloat((Float) value);
            } else if (value instanceof Double) {
                return new JavaConstant.Simple.OfTrivialValue.ForDouble((Double) value);
            } else if (value instanceof String) {
                return new JavaConstant.Simple.OfTrivialValue.ForString((String) value);
            } else if (CLASS_DESC.isInstance(value)) {
                Type type = Type.getType(CLASS_DESC.descriptorString(value));
                return JavaConstant.Simple.OfTypeDescription.of(typePool.describe(type.getSort() == Type.ARRAY
                        ? type.getInternalName().replace('/', '.')
                        : type.getClassName()).resolve());
            } else if (METHOD_TYPE_DESC.isInstance(value)) {
                Object[] parameterTypes = METHOD_TYPE_DESC.parameterArray(value);
                List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(parameterTypes.length);
                for (Object parameterType : parameterTypes) {
                    Type type = Type.getType(CLASS_DESC.descriptorString(parameterType));
                    typeDescriptions.add(typePool.describe(type.getSort() == Type.ARRAY
                            ? type.getInternalName().replace('/', '.')
                            : type.getClassName()).resolve());
                }
                Type type = Type.getType(CLASS_DESC.descriptorString(METHOD_TYPE_DESC.returnType(value)));
                return MethodType.of(typePool.describe(type.getSort() == Type.ARRAY
                        ? type.getInternalName().replace('/', '.')
                        : type.getClassName()).resolve(), typeDescriptions);
            } else if (DIRECT_METHOD_HANDLE_DESC.isInstance(value)) {
                Object[] parameterTypes = METHOD_TYPE_DESC.parameterArray(METHOD_HANDLE_DESC.invocationType(value));
                List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(parameterTypes.length);
                for (Object parameterType : parameterTypes) {
                    Type type = Type.getType(CLASS_DESC.descriptorString(parameterType));
                    typeDescriptions.add(typePool.describe(type.getSort() == Type.ARRAY
                            ? type.getInternalName().replace('/', '.')
                            : type.getClassName()).resolve());
                }
                Type type = Type.getType(CLASS_DESC.descriptorString(METHOD_TYPE_DESC.returnType(METHOD_HANDLE_DESC.invocationType(value))));
                return new MethodHandle(MethodHandle.HandleType.of(DIRECT_METHOD_HANDLE_DESC.refKind(value)),
                        typePool.describe(Type.getType(CLASS_DESC.descriptorString(DIRECT_METHOD_HANDLE_DESC.owner(value))).getClassName()).resolve(),
                        DIRECT_METHOD_HANDLE_DESC.methodName(value),
                        DIRECT_METHOD_HANDLE_DESC.refKind(value) == Opcodes.H_NEWINVOKESPECIAL
                                ? TypeDescription.ForLoadedType.of(void.class)
                                : typePool.describe(type.getSort() == Type.ARRAY ? type.getInternalName().replace('/', '.') : type.getClassName()).resolve(),
                        typeDescriptions);
            } else if (DYNAMIC_CONSTANT_DESC.isInstance(value)) {
                Type methodType = Type.getMethodType(DIRECT_METHOD_HANDLE_DESC.lookupDescriptor(DYNAMIC_CONSTANT_DESC.bootstrapMethod(value)));
                List<TypeDescription> parameterTypes = new ArrayList<TypeDescription>(methodType.getArgumentTypes().length);
                for (Type type : methodType.getArgumentTypes()) {
                    parameterTypes.add(typePool.describe(type.getSort() == Type.ARRAY
                            ? type.getInternalName().replace('/', '.')
                            : type.getClassName()).resolve());
                }
                Object[] constant = DYNAMIC_CONSTANT_DESC.bootstrapArgs(value);
                List<JavaConstant> arguments = new ArrayList<JavaConstant>(constant.length);
                for (Object aConstant : constant) {
                    arguments.add(ofDescription(aConstant, typePool));
                }
                Type type = Type.getType(CLASS_DESC.descriptorString(DYNAMIC_CONSTANT_DESC.constantType(value)));
                return new Dynamic(DYNAMIC_CONSTANT_DESC.constantName(value),
                        typePool.describe(type.getSort() == Type.ARRAY
                                ? type.getInternalName().replace('/', '.')
                                : type.getClassName()).resolve(),
                        new MethodHandle(MethodHandle.HandleType.of(DIRECT_METHOD_HANDLE_DESC.refKind(DYNAMIC_CONSTANT_DESC.bootstrapMethod(value))),
                                typePool.describe(Type.getType(CLASS_DESC.descriptorString(DIRECT_METHOD_HANDLE_DESC.owner(DYNAMIC_CONSTANT_DESC.bootstrapMethod(value)))).getClassName()).resolve(),
                                DIRECT_METHOD_HANDLE_DESC.methodName(DYNAMIC_CONSTANT_DESC.bootstrapMethod(value)),
                                typePool.describe(methodType.getReturnType().getSort() == Type.ARRAY
                                        ? methodType.getReturnType().getInternalName().replace('/', '.')
                                        : methodType.getReturnType().getClassName()).resolve(),
                                parameterTypes),
                        arguments);
            } else {
                throw new IllegalArgumentException("Not a resolvable constant description or not expressible as a constant pool value: " + value);
            }
        }

        /**
         * Returns a Java constant representation for a {@link TypeDescription}.
         *
         * @param typeDescription The type to represent as a constant.
         * @return An appropriate Java constant representation.
         */
        public static JavaConstant of(TypeDescription typeDescription) {
            if (typeDescription.isPrimitive()) {
                throw new IllegalArgumentException("A primitive type cannot be represented as a type constant: " + typeDescription);
            }
            return new JavaConstant.Simple.OfTypeDescription(typeDescription);
        }

        /**
         * Wraps a value representing a loaded or unloaded constant as {@link JavaConstant} instance.
         *
         * @param value The value to wrap.
         * @return A wrapped Java constant.
         */
        public static JavaConstant wrap(Object value) {
            if (value instanceof JavaConstant) {
                return (JavaConstant) value;
            } else if (value instanceof TypeDescription) {
                return of((TypeDescription) value);
            } else {
                return ofLoaded(value);
            }
        }

        /**
         * Wraps a list of either loaded or unloaded constant representations as {@link JavaConstant} instances.
         *
         * @param values The values to wrap.
         * @return A list of wrapped Java constants.
         */
        public static List<JavaConstant> wrap(List<?> values) {
            List<JavaConstant> constants = new ArrayList<JavaConstant>(values.size());
            for (Object value : values) {
                constants.add(wrap(value));
            }
            return constants;
        }

        /**
         * Returns the represented value.
         *
         * @return The represented value.
         */
        public T getValue() {
            return value;
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getTypeDescription() {
            return typeDescription;
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(@MaybeNull Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            return value.equals(((JavaConstant.Simple<?>) object).value);
        }

        @Override
        public String toString() {
            return value.toString();
        }

        /**
         * Represents a trivial constant value that represents itself.
         *
         * @param <S> The represented type.
         */
        protected abstract static class OfTrivialValue<S> extends JavaConstant.Simple<S> {

            /**
             * Creates a representation of a trivial constant that represents itself.
             *
             * @param value           The represented value.
             * @param typeDescription The represented value's type.
             */
            protected OfTrivialValue(S value, TypeDescription typeDescription) {
                super(value, typeDescription);
            }

            /**
             * {@inheritDoc}
             */
            public Object toDescription() {
                return value;
            }

            /**
             * {@inheritDoc}
             */
            public <T> T accept(Visitor<T> visitor) {
                return visitor.onValue(this);
            }

            protected static class ForInteger extends OfTrivialValue<Integer> {

                public ForInteger(Integer value) {
                    super(value, TypeDescription.ForLoadedType.of(int.class));
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation toStackManipulation() {
                    return IntegerConstant.forValue(value);
                }
            }

            protected static class ForLong extends OfTrivialValue<Long> {

                public ForLong(Long value) {
                    super(value, TypeDescription.ForLoadedType.of(long.class));
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation toStackManipulation() {
                    return LongConstant.forValue(value);
                }
            }

            protected static class ForFloat extends OfTrivialValue<Float> {

                public ForFloat(Float value) {
                    super(value, TypeDescription.ForLoadedType.of(float.class));
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation toStackManipulation() {
                    return FloatConstant.forValue(value);
                }
            }

            protected static class ForDouble extends OfTrivialValue<Double> {

                public ForDouble(Double value) {
                    super(value, TypeDescription.ForLoadedType.of(double.class));
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation toStackManipulation() {
                    return DoubleConstant.forValue(value);
                }
            }

            protected static class ForString extends OfTrivialValue<String> {

                public ForString(String value) {
                    super(value, TypeDescription.ForLoadedType.of(String.class));
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation toStackManipulation() {
                    return new TextConstant(value);
                }
            }
        }

        /**
         * Represents a type constant.
         */
        protected static class OfTypeDescription extends JavaConstant.Simple<TypeDescription> {

            /**
             * Creates a type constant.
             *
             * @param value The represented type.
             */
            protected OfTypeDescription(TypeDescription value) {
                super(value, TypeDescription.ForLoadedType.of(Class.class));
            }

            /**
             * {@inheritDoc}
             */
            public Object toDescription() {
                return CLASS_DESC.ofDescriptor(value.getDescriptor());
            }

            /**
             * {@inheritDoc}
             */
            public StackManipulation toStackManipulation() {
                return ClassConstant.of(value);
            }

            /**
             * {@inheritDoc}
             */
            public <T> T accept(Visitor<T> visitor) {
                return visitor.onType(this);
            }
        }

        /**
         * A dispatcher to represent {@code java.lang.constant.ConstantDesc}.
         */
        @JavaDispatcher.Proxied("java.lang.constant.ConstantDesc")
        protected interface Dispatcher {

            /**
             * Checks if the supplied instance is of the type of this dispatcher.
             *
             * @param instance The instance to verify.
             * @return {@code true} if the instance is of the supplied type.
             */
            @JavaDispatcher.Instance
            boolean isInstance(Object instance);

            /**
             * Returns an array of the dispatcher type.
             *
             * @param length The length of the array.
             * @return An array of the type that is represented by this dispatcher with the given length.
             */
            @JavaDispatcher.Container
            Object[] toArray(int length);

            /**
             * A dispatcher to represent {@code java.lang.constant.ClassDesc}.
             */
            @JavaDispatcher.Proxied("java.lang.constant.ClassDesc")
            interface OfClassDesc extends Dispatcher {

                /**
                 * Resolves a {@code java.lang.constant.ClassDesc} of a descriptor.
                 *
                 * @param descriptor The descriptor to resolve.
                 * @return An appropriate {@code java.lang.constant.ClassDesc}.
                 */
                @JavaDispatcher.IsStatic
                Object ofDescriptor(String descriptor);

                /**
                 * Returns the descriptor of the supplied class description.
                 *
                 * @param value The {@code java.lang.constant.ClassDesc} to resolve.
                 * @return The class's descriptor.
                 */
                String descriptorString(Object value);
            }

            /**
             * A dispatcher to represent {@code java.lang.constant.MethodTypeDesc}.
             */
            @JavaDispatcher.Proxied("java.lang.constant.MethodTypeDesc")
            interface OfMethodTypeDesc extends Dispatcher {

                /**
                 * Resolves a {@code java.lang.constant.MethodTypeDesc} from descriptions of the return type descriptor and parameter types.
                 *
                 * @param returnType    A {@code java.lang.constant.ClassDesc} representing the return type.
                 * @param parameterType An array of {@code java.lang.constant.ClassDesc}s representing the parameter types.
                 * @return An appropriate {@code java.lang.constant.MethodTypeDesc}.
                 */
                @JavaDispatcher.IsStatic
                Object of(@JavaDispatcher.Proxied("java.lang.constant.ClassDesc") Object returnType,
                          @JavaDispatcher.Proxied("java.lang.constant.ClassDesc") Object[] parameterType);

                /**
                 * Returns a {@code java.lang.constant.MethodTypeDesc} for a given descriptor.
                 *
                 * @param descriptor The method type's descriptor.
                 * @return A {@code java.lang.constant.MethodTypeDesc} of the supplied descriptor
                 */
                @JavaDispatcher.IsStatic
                Object ofDescriptor(String descriptor);

                /**
                 * Returns the return type of a {@code java.lang.constant.MethodTypeDesc}.
                 *
                 * @param value The {@code java.lang.constant.MethodTypeDesc} to resolve.
                 * @return A {@code java.lang.constant.ClassDesc} of the supplied {@code java.lang.constant.MethodTypeDesc}'s return type.
                 */
                Object returnType(Object value);

                /**
                 * Returns the parameter types of a {@code java.lang.constant.MethodTypeDesc}.
                 *
                 * @param value The {@code java.lang.constant.MethodTypeDesc} to resolve.
                 * @return An array of {@code java.lang.constant.ClassDesc} of the supplied {@code java.lang.constant.MethodTypeDesc}'s parameter types.
                 */
                Object[] parameterArray(Object value);
            }

            /**
             * A dispatcher to represent {@code java.lang.constant.MethodHandleDesc}.
             */
            @JavaDispatcher.Proxied("java.lang.constant.MethodHandleDesc")
            interface OfMethodHandleDesc extends Dispatcher {

                /**
                 * Resolves a {@code java.lang.constant.MethodHandleDesc}.
                 *
                 * @param kind       The {@code java.lang.constant.DirectMethodHandleDesc$Kind} of the resolved method handle description.
                 * @param owner      The {@code java.lang.constant.ClassDesc} of the resolved method handle description's owner type.
                 * @param name       The name of the method handle to resolve.
                 * @param descriptor A descriptor of the lookup type.
                 * @return An {@code java.lang.constant.MethodTypeDesc} representing the invocation type.
                 */
                @JavaDispatcher.IsStatic
                Object of(@JavaDispatcher.Proxied("java.lang.constant.DirectMethodHandleDesc$Kind") Object kind,
                          @JavaDispatcher.Proxied("java.lang.constant.ClassDesc") Object owner,
                          String name,
                          String descriptor);

                /**
                 * Resolves a {@code java.lang.constant.MethodTypeDesc} representing the invocation type of
                 * the supplied {@code java.lang.constant.DirectMethodHandleDesc}.
                 *
                 * @param value The {@code java.lang.constant.DirectMethodHandleDesc} to resolve.
                 * @return An {@code java.lang.constant.MethodTypeDesc} representing the invocation type.
                 */
                Object invocationType(Object value);
            }

            /**
             * A dispatcher to represent {@code java.lang.constant.DirectMethodHandleDesc}.
             */
            @JavaDispatcher.Proxied("java.lang.constant.DirectMethodHandleDesc")
            interface OfDirectMethodHandleDesc extends Dispatcher {

                /**
                 * Resolves the type of method handle for the supplied method handle description.
                 *
                 * @param value The {@code java.lang.constant.DirectMethodHandleDesc} to resolve.
                 * @return The type of the handle.
                 */
                int refKind(Object value);

                /**
                 * Resolves the method name of the supplied direct method handle.
                 *
                 * @param value The {@code java.lang.constant.DirectMethodHandleDesc} to resolve.
                 * @return The handle's method name.
                 */
                String methodName(Object value);

                /**
                 * Resolves a {@code java.lang.constant.ClassDesc} representing the owner of a direct method handle description.
                 *
                 * @param value The {@code java.lang.constant.DirectMethodHandleDesc} to resolve.
                 * @return A {@code java.lang.constant.ClassDesc} describing the handle's owner.
                 */
                Object owner(Object value);

                /**
                 * Resolves the lookup descriptor of the supplied direct method handle description.
                 *
                 * @param value The {@code java.lang.constant.DirectMethodHandleDesc} to resolve.
                 * @return A descriptor of the supplied direct method handle's lookup.
                 */
                String lookupDescriptor(Object value);

                /**
                 * A dispatcher to represent {@code java.lang.constant.DirectMethodHandleDesc$Kind}.
                 */
                @JavaDispatcher.Proxied("java.lang.constant.DirectMethodHandleDesc$Kind")
                interface ForKind {

                    /**
                     * Resolves a {@code java.lang.constant.DirectMethodHandleDesc$Kind} from an identifier.
                     *
                     * @param identifier  The identifier to resolve.
                     * @param isInterface {@code true} if the handle invokes an interface type.
                     * @return The identifier's {@code java.lang.constant.DirectMethodHandleDesc$Kind}.
                     */
                    @JavaDispatcher.IsStatic
                    Object valueOf(int identifier, boolean isInterface);
                }
            }

            /**
             * A dispatcher to represent {@code java.lang.constant.DynamicConstantDesc}.
             */
            @JavaDispatcher.Proxied("java.lang.constant.DynamicConstantDesc")
            interface OfDynamicConstantDesc extends Dispatcher {

                /**
                 * Resolves a {@code java.lang.constant.DynamicConstantDesc} for a canonical description of the constant.
                 *
                 * @param bootstrap    A {@code java.lang.constant.DirectMethodHandleDesc} describing the boostrap method of the dynamic constant.
                 * @param constantName The constant's name.
                 * @param type         A {@code java.lang.constant.ClassDesc} describing the constant's type.
                 * @param argument     Descriptions of the dynamic constant's arguments.
                 * @return A {@code java.lang.constant.DynamicConstantDesc} for the supplied arguments.
                 */
                @JavaDispatcher.IsStatic
                Object ofCanonical(@JavaDispatcher.Proxied("java.lang.constant.DirectMethodHandleDesc") Object bootstrap,
                                   String constantName,
                                   @JavaDispatcher.Proxied("java.lang.constant.ClassDesc") Object type,
                                   @JavaDispatcher.Proxied("java.lang.constant.ConstantDesc") Object[] argument);

                /**
                 * Resolves a {@code java.lang.constant.DynamicConstantDesc}'s arguments.
                 *
                 * @param value The {@code java.lang.constant.DynamicConstantDesc} to resolve.
                 * @return An array of {@code java.lang.constant.ConstantDesc} describing the arguments of the supplied dynamic constant description.
                 */
                Object[] bootstrapArgs(Object value);

                /**
                 * Resolves the dynamic constant description's name.
                 *
                 * @param value The {@code java.lang.constant.DynamicConstantDesc} to resolve.
                 * @return The dynamic constant description's name.
                 */
                String constantName(Object value);

                /**
                 * Resolves a {@code java.lang.constant.ClassDesc} for the dynamic constant's type.
                 *
                 * @param value The {@code java.lang.constant.DynamicConstantDesc} to resolve.
                 * @return A {@code java.lang.constant.ClassDesc} describing the constant's type.
                 */
                Object constantType(Object value);

                /**
                 * Resolves a {@code java.lang.constant.DirectMethodHandleDesc} representing the dynamic constant's bootstrap method.
                 *
                 * @param value The {@code java.lang.constant.DynamicConstantDesc} to resolve.
                 * @return A {@code java.lang.constant.DirectMethodHandleDesc} representing the dynamic constant's bootstrap method.
                 */
                Object bootstrapMethod(Object value);

            }
        }
    }

    /**
     * Represents a {@code java.lang.invoke.MethodType} object.
     */
    class MethodType implements JavaConstant {

        /**
         * A dispatcher for extracting information from a {@code java.lang.invoke.MethodType} instance.
         */
        private static final Dispatcher DISPATCHER = doPrivileged(JavaDispatcher.of(Dispatcher.class));

        /**
         * The return type of this method type.
         */
        private final TypeDescription returnType;

        /**
         * The parameter types of this method type.
         */
        private final List<? extends TypeDescription> parameterTypes;

        /**
         * Creates a method type for the given types.
         *
         * @param returnType     The return type of the method type.
         * @param parameterTypes The parameter types of the method type.
         */
        protected MethodType(TypeDescription returnType, List<? extends TypeDescription> parameterTypes) {
            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
        }

        /**
         * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
         *
         * @param action The action to execute from a privileged context.
         * @param <T>    The type of the action's resolved value.
         * @return The action's resolved value.
         */
        @AccessControllerPlugin.Enhance
        private static <T> T doPrivileged(PrivilegedAction<T> action) {
            return action.run();
        }

        /**
         * Returns a method type representation of a loaded {@code MethodType} object.
         *
         * @param methodType A method type object to represent as a {@link JavaConstant}.
         * @return The method type represented as a {@link MethodType}.
         */
        public static MethodType ofLoaded(Object methodType) {
            if (!JavaType.METHOD_TYPE.isInstance(methodType)) {
                throw new IllegalArgumentException("Expected method type object: " + methodType);
            }
            return of(DISPATCHER.returnType(methodType), DISPATCHER.parameterArray(methodType));
        }

        /**
         * Returns a method type description of the given return type and parameter types.
         *
         * @param returnType    The return type to represent.
         * @param parameterType The parameter types to represent.
         * @return A method type of the given return type and parameter types.
         */
        public static MethodType of(Class<?> returnType, Class<?>... parameterType) {
            return of(TypeDescription.ForLoadedType.of(returnType), new TypeList.ForLoadedTypes(parameterType));
        }

        /**
         * Returns a method type description of the given return type and parameter types.
         *
         * @param returnType    The return type to represent.
         * @param parameterType The parameter types to represent.
         * @return A method type of the given return type and parameter types.
         */
        public static MethodType of(TypeDescription returnType, TypeDescription... parameterType) {
            return new MethodType(returnType, Arrays.asList(parameterType));
        }

        /**
         * Returns a method type description of the given return type and parameter types.
         *
         * @param returnType     The return type to represent.
         * @param parameterTypes The parameter types to represent.
         * @return A method type of the given return type and parameter types.
         */
        public static MethodType of(TypeDescription returnType, List<? extends TypeDescription> parameterTypes) {
            return new MethodType(returnType, parameterTypes);
        }

        /**
         * Returns a method type description of the given method.
         *
         * @param method The method to extract the method type from.
         * @return The method type of the given method.
         */
        public static MethodType of(Method method) {
            return of(new MethodDescription.ForLoadedMethod(method));
        }

        /**
         * Returns a method type description of the given constructor.
         *
         * @param constructor The constructor to extract the method type from.
         * @return The method type of the given constructor.
         */
        public static MethodType of(Constructor<?> constructor) {
            return of(new MethodDescription.ForLoadedConstructor(constructor));
        }

        /**
         * Returns a method type description of the given method.
         *
         * @param methodDescription The method to extract the method type from.
         * @return The method type of the given method.
         */
        public static MethodType of(MethodDescription methodDescription) {
            return new MethodType(
                    (methodDescription.isConstructor() ? methodDescription.getDeclaringType() : methodDescription.getReturnType()).asErasure(),
                    methodDescription.isStatic() || methodDescription.isConstructor()
                            ? methodDescription.getParameters().asTypeList().asErasures()
                            : CompoundList.of(methodDescription.getDeclaringType().asErasure(), methodDescription.getParameters().asTypeList().asErasures()));
        }

        /**
         * Returns a method type description of the given method's signature without considering the method's actual stack consumption
         * and production.
         *
         * @param method The method to extract the method type from.
         * @return The method type of the given method's signature.
         */
        public static MethodType ofSignature(Method method) {
            return ofSignature(new MethodDescription.ForLoadedMethod(method));
        }

        /**
         * Returns a method type description of the given constructor's signature without considering the constructor's
         * actual stack consumption and production.
         *
         * @param constructor The constructor to extract the method type from.
         * @return The method type of the given method's signature.
         */
        public static MethodType ofSignature(Constructor<?> constructor) {
            return ofSignature(new MethodDescription.ForLoadedConstructor(constructor));
        }

        /**
         * Returns a method type description of the given method's signature without considering the method's actual stack consumption
         * and production.
         *
         * @param methodDescription The method to extract the method type from.
         * @return The method type of the given method's signature.
         */
        public static MethodType ofSignature(MethodDescription methodDescription) {
            return new MethodType(methodDescription.getReturnType().asErasure(), methodDescription.getParameters().asTypeList().asErasures());
        }

        /**
         * Returns a method type for a setter of the given field.
         *
         * @param field The field to extract a setter type for.
         * @return The type of a setter for the given field.
         */
        public static MethodType ofSetter(Field field) {
            return ofSetter(new FieldDescription.ForLoadedField(field));
        }

        /**
         * Returns a method type for a setter of the given field.
         *
         * @param fieldDescription The field to extract a setter type for.
         * @return The type of a setter for the given field.
         */
        @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming declaring type for type member.")
        public static MethodType ofSetter(FieldDescription fieldDescription) {
            return new MethodType(TypeDescription.ForLoadedType.of(void.class), fieldDescription.isStatic()
                    ? Collections.singletonList(fieldDescription.getType().asErasure())
                    : Arrays.asList(fieldDescription.getDeclaringType().asErasure(), fieldDescription.getType().asErasure()));
        }

        /**
         * Returns a method type for a getter of the given field.
         *
         * @param field The field to extract a getter type for.
         * @return The type of a getter for the given field.
         */
        public static MethodType ofGetter(Field field) {
            return ofGetter(new FieldDescription.ForLoadedField(field));
        }

        /**
         * Returns a method type for a getter of the given field.
         *
         * @param fieldDescription The field to extract a getter type for.
         * @return The type of a getter for the given field.
         */
        @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming declaring type for type member.")
        public static MethodType ofGetter(FieldDescription fieldDescription) {
            return new MethodType(fieldDescription.getType().asErasure(), fieldDescription.isStatic()
                    ? Collections.<TypeDescription>emptyList()
                    : Collections.singletonList(fieldDescription.getDeclaringType().asErasure()));
        }

        /**
         * Returns a method type for the given constant.
         *
         * @param instance The constant for which a constant method type should be created.
         * @return A method type for the given constant.
         */
        public static MethodType ofConstant(Object instance) {
            return ofConstant(instance.getClass());
        }

        /**
         * Returns a method type for the given constant type.
         *
         * @param type The constant type for which a constant method type should be created.
         * @return A method type for the given constant type.
         */
        public static MethodType ofConstant(Class<?> type) {
            return ofConstant(TypeDescription.ForLoadedType.of(type));
        }

        /**
         * Returns a method type for the given constant type.
         *
         * @param typeDescription The constant type for which a constant method type should be created.
         * @return A method type for the given constant type.
         */
        public static MethodType ofConstant(TypeDescription typeDescription) {
            return new MethodType(typeDescription, Collections.<TypeDescription>emptyList());
        }

        /**
         * Returns the return type of this method type.
         *
         * @return The return type of this method type.
         */
        public TypeDescription getReturnType() {
            return returnType;
        }

        /**
         * Returns the parameter types of this method type.
         *
         * @return The parameter types of this method type.
         */
        public TypeList getParameterTypes() {
            return new TypeList.Explicit(parameterTypes);
        }

        /**
         * Returns the method descriptor of this method type representation.
         *
         * @return The method descriptor of this method type representation.
         */
        public String getDescriptor() {
            StringBuilder stringBuilder = new StringBuilder("(");
            for (TypeDescription parameterType : parameterTypes) {
                stringBuilder.append(parameterType.getDescriptor());
            }
            return stringBuilder.append(')').append(returnType.getDescriptor()).toString();
        }

        /**
         * {@inheritDoc}
         */
        public Object toDescription() {
            Object[] parameterType = JavaConstant.Simple.CLASS_DESC.toArray(parameterTypes.size());
            for (int index = 0; index < parameterTypes.size(); index++) {
                parameterType[index] = JavaConstant.Simple.CLASS_DESC.ofDescriptor(parameterTypes.get(index).getDescriptor());
            }
            return JavaConstant.Simple.METHOD_TYPE_DESC.of(JavaConstant.Simple.CLASS_DESC.ofDescriptor(returnType.getDescriptor()), parameterType);
        }

        /**
         * {@inheritDoc}
         */
        public <T> T accept(Visitor<T> visitor) {
            return visitor.onMethodType(this);
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getTypeDescription() {
            return JavaType.METHOD_TYPE.getTypeStub();
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation toStackManipulation() {
            return new JavaConstantValue(this);
        }

        @Override
        public int hashCode() {
            int result = returnType.hashCode();
            result = 31 * result + parameterTypes.hashCode();
            return result;
        }

        @Override
        public boolean equals(@MaybeNull Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof MethodType)) {
                return false;
            }
            MethodType methodType = (MethodType) other;
            return parameterTypes.equals(methodType.parameterTypes) && returnType.equals(methodType.returnType);

        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder().append('(');
            boolean first = true;
            for (TypeDescription typeDescription : parameterTypes) {
                if (first) {
                    first = false;
                } else {
                    stringBuilder.append(',');
                }
                stringBuilder.append(typeDescription.getSimpleName());
            }
            return stringBuilder.append(')').append(returnType.getSimpleName()).toString();
        }

        /**
         * A dispatcher for extracting information from a {@code java.lang.invoke.MethodType} instance.
         */
        @JavaDispatcher.Proxied("java.lang.invoke.MethodType")
        protected interface Dispatcher {

            /**
             * Extracts the return type of the supplied method type.
             *
             * @param methodType An instance of {@code java.lang.invoke.MethodType}.
             * @return The return type that is described by the supplied instance.
             */
            Class<?> returnType(Object methodType);

            /**
             * Extracts the parameter types of the supplied method type.
             *
             * @param methodType An instance of {@code java.lang.invoke.MethodType}.
             * @return The parameter types that are described by the supplied instance.
             */
            Class<?>[] parameterArray(Object methodType);
        }
    }

    /**
     * Represents a {@code java.lang.invoke.MethodHandle} object. Note that constant {@code MethodHandle}s cannot
     * be represented within the constant pool of a Java class and can therefore not be represented as an instance of
     * this representation order.
     */
    class MethodHandle implements JavaConstant {

        /**
         * A dispatcher to interact with {@code java.lang.invoke.MethodHandleInfo}.
         */
        protected static final MethodHandleInfo METHOD_HANDLE_INFO = doPrivileged(JavaDispatcher.of(MethodHandleInfo.class));

        /**
         * A dispatcher to interact with {@code java.lang.invoke.MethodType}.
         */
        protected static final MethodType METHOD_TYPE = doPrivileged(JavaDispatcher.of(MethodType.class));

        /**
         * A dispatcher to interact with {@code java.lang.invoke.MethodHandles}.
         */
        protected static final MethodHandles METHOD_HANDLES = doPrivileged(JavaDispatcher.of(MethodHandles.class));

        /**
         * A dispatcher to interact with {@code java.lang.invoke.MethodHandles$Lookup}.
         */
        protected static final MethodHandles.Lookup METHOD_HANDLES_LOOKUP = doPrivileged(JavaDispatcher.of(MethodHandles.Lookup.class));

        /**
         * The handle type that is represented by this instance.
         */
        private final HandleType handleType;

        /**
         * The owner type that is represented by this instance.
         */
        private final TypeDescription ownerType;

        /**
         * The name that is represented by this instance.
         */
        private final String name;

        /**
         * The return type that is represented by this instance.
         */
        private final TypeDescription returnType;

        /**
         * The parameter types that is represented by this instance.
         */
        private final List<? extends TypeDescription> parameterTypes;

        /**
         * Creates a method handle representation.
         *
         * @param handleType     The handle type that is represented by this instance.
         * @param ownerType      The owner type that is represented by this instance.
         * @param name           The name that is represented by this instance.
         * @param returnType     The return type that is represented by this instance.
         * @param parameterTypes The parameter types that is represented by this instance.
         */
        public MethodHandle(HandleType handleType,
                            TypeDescription ownerType,
                            String name,
                            TypeDescription returnType,
                            List<? extends TypeDescription> parameterTypes) {
            this.handleType = handleType;
            this.ownerType = ownerType;
            this.name = name;
            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
        }

        /**
         * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
         *
         * @param action The action to execute from a privileged context.
         * @param <T>    The type of the action's resolved value.
         * @return The action's resolved value.
         */
        @AccessControllerPlugin.Enhance
        private static <T> T doPrivileged(PrivilegedAction<T> action) {
            return action.run();
        }

        /**
         * Creates a method handles representation of a loaded method handle which is analyzed using a public {@code MethodHandles.Lookup} object.
         * A method handle can only be analyzed on virtual machines that support the corresponding API (Java 7+). For virtual machines before Java 8+,
         * a method handle instance can only be analyzed by taking advantage of private APIs what might require a access context.
         *
         * @param methodHandle The loaded method handle to represent.
         * @return A representation of the loaded method handle
         */
        public static MethodHandle ofLoaded(Object methodHandle) {
            return ofLoaded(methodHandle, METHOD_HANDLES.publicLookup());
        }

        /**
         * Creates a method handles representation of a loaded method handle which is analyzed using the given lookup context.
         * A method handle can only be analyzed on virtual machines that support the corresponding API (Java 7+). For virtual machines before Java 8+,
         * a method handle instance can only be analyzed by taking advantage of private APIs what might require a access context.
         *
         * @param methodHandle The loaded method handle to represent.
         * @param lookup       The lookup object to use for analyzing the method handle.
         * @return A representation of the loaded method handle
         */
        public static MethodHandle ofLoaded(Object methodHandle, Object lookup) {
            if (!JavaType.METHOD_HANDLE.isInstance(methodHandle)) {
                throw new IllegalArgumentException("Expected method handle object: " + methodHandle);
            } else if (!JavaType.METHOD_HANDLES_LOOKUP.isInstance(lookup)) {
                throw new IllegalArgumentException("Expected method handle lookup object: " + lookup);
            }
            Object methodHandleInfo = ClassFileVersion.ofThisVm(ClassFileVersion.JAVA_V8).isAtMost(ClassFileVersion.JAVA_V7)
                    ? METHOD_HANDLE_INFO.revealDirect(methodHandle)
                    : METHOD_HANDLES_LOOKUP.revealDirect(lookup, methodHandle);
            Object methodType = METHOD_HANDLE_INFO.getMethodType(methodHandleInfo);
            return new MethodHandle(HandleType.of(METHOD_HANDLE_INFO.getReferenceKind(methodHandleInfo)),
                    TypeDescription.ForLoadedType.of(METHOD_HANDLE_INFO.getDeclaringClass(methodHandleInfo)),
                    METHOD_HANDLE_INFO.getName(methodHandleInfo),
                    TypeDescription.ForLoadedType.of(METHOD_TYPE.returnType(methodType)),
                    new TypeList.ForLoadedTypes(METHOD_TYPE.parameterArray(methodType)));
        }

        /**
         * Creates a method handle representation of the given method.
         *
         * @param method The method ro represent.
         * @return A method handle representing the given method.
         */
        public static MethodHandle of(Method method) {
            return of(new MethodDescription.ForLoadedMethod(method));
        }

        /**
         * Creates a method handle representation of the given constructor.
         *
         * @param constructor The constructor ro represent.
         * @return A method handle representing the given constructor.
         */
        public static MethodHandle of(Constructor<?> constructor) {
            return of(new MethodDescription.ForLoadedConstructor(constructor));
        }

        /**
         * Creates a method handle representation of the given method.
         *
         * @param methodDescription The method ro represent.
         * @return A method handle representing the given method.
         */
        public static MethodHandle of(MethodDescription.InDefinedShape methodDescription) {
            return new MethodHandle(HandleType.of(methodDescription),
                    methodDescription.getDeclaringType().asErasure(),
                    methodDescription.getInternalName(),
                    methodDescription.getReturnType().asErasure(),
                    methodDescription.getParameters().asTypeList().asErasures());
        }

        /**
         * Creates a method handle representation of the given method for an explicit special method invocation of an otherwise virtual method.
         *
         * @param method The method ro represent.
         * @param type   The type on which the method is to be invoked on as a special method invocation.
         * @return A method handle representing the given method as special method invocation.
         */
        public static MethodHandle ofSpecial(Method method, Class<?> type) {
            return ofSpecial(new MethodDescription.ForLoadedMethod(method), TypeDescription.ForLoadedType.of(type));
        }

        /**
         * Creates a method handle representation of the given method for an explicit special method invocation of an otherwise virtual method.
         *
         * @param methodDescription The method ro represent.
         * @param typeDescription   The type on which the method is to be invoked on as a special method invocation.
         * @return A method handle representing the given method as special method invocation.
         */
        public static MethodHandle ofSpecial(MethodDescription.InDefinedShape methodDescription, TypeDescription typeDescription) {
            if (!methodDescription.isSpecializableFor(typeDescription)) {
                throw new IllegalArgumentException("Cannot specialize " + methodDescription + " for " + typeDescription);
            }
            return new MethodHandle(HandleType.ofSpecial(methodDescription),
                    typeDescription,
                    methodDescription.getInternalName(),
                    methodDescription.getReturnType().asErasure(),
                    methodDescription.getParameters().asTypeList().asErasures());
        }

        /**
         * Returns a method handle for a setter of the given field.
         *
         * @param field The field to represent.
         * @return A method handle for a setter of the given field.
         */
        public static MethodHandle ofGetter(Field field) {
            return ofGetter(new FieldDescription.ForLoadedField(field));
        }

        /**
         * Returns a method handle for a setter of the given field.
         *
         * @param fieldDescription The field to represent.
         * @return A method handle for a setter of the given field.
         */
        public static MethodHandle ofGetter(FieldDescription.InDefinedShape fieldDescription) {
            return new MethodHandle(HandleType.ofGetter(fieldDescription),
                    fieldDescription.getDeclaringType().asErasure(),
                    fieldDescription.getInternalName(),
                    fieldDescription.getType().asErasure(),
                    Collections.<TypeDescription>emptyList());
        }

        /**
         * Returns a method handle for a getter of the given field.
         *
         * @param field The field to represent.
         * @return A method handle for a getter of the given field.
         */
        public static MethodHandle ofSetter(Field field) {
            return ofSetter(new FieldDescription.ForLoadedField(field));
        }

        /**
         * Returns a method handle for a getter of the given field.
         *
         * @param fieldDescription The field to represent.
         * @return A method handle for a getter of the given field.
         */
        public static MethodHandle ofSetter(FieldDescription.InDefinedShape fieldDescription) {
            return new MethodHandle(HandleType.ofSetter(fieldDescription),
                    fieldDescription.getDeclaringType().asErasure(),
                    fieldDescription.getInternalName(),
                    TypeDescription.ForLoadedType.of(void.class),
                    Collections.singletonList(fieldDescription.getType().asErasure()));
        }

        /**
         * Returns the lookup type of the provided {@code java.lang.invoke.MethodHandles$Lookup} instance.
         *
         * @param callerClassLookup An instance of {@code java.lang.invoke.MethodHandles$Lookup}.
         * @return The instance's lookup type.
         */
        public static Class<?> lookupType(Object callerClassLookup) {
            return METHOD_HANDLES_LOOKUP.lookupClass(callerClassLookup);
        }

        /**
         * {@inheritDoc}
         */
        public Object toDescription() {
            return JavaConstant.Simple.METHOD_HANDLE_DESC.of(JavaConstant.Simple.DIRECT_METHOD_HANDLE_DESC_KIND.valueOf(handleType.getIdentifier(), ownerType.isInterface()),
                    JavaConstant.Simple.CLASS_DESC.ofDescriptor(ownerType.getDescriptor()),
                    name,
                    getDescriptor());
        }

        /**
         * {@inheritDoc}
         */
        public <T> T accept(Visitor<T> visitor) {
            return visitor.onMethodHandle(this);
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getTypeDescription() {
            return JavaType.METHOD_HANDLE.getTypeStub();
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation toStackManipulation() {
            return new JavaConstantValue(this);
        }

        /**
         * Returns the handle type represented by this instance.
         *
         * @return The handle type represented by this instance.
         */
        public HandleType getHandleType() {
            return handleType;
        }

        /**
         * Returns the owner type of this instance.
         *
         * @return The owner type of this instance.
         */
        public TypeDescription getOwnerType() {
            return ownerType;
        }

        /**
         * Returns the name represented by this instance.
         *
         * @return The name represented by this instance.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the return type represented by this instance.
         *
         * @return The return type represented by this instance.
         */
        public TypeDescription getReturnType() {
            return returnType;
        }

        /**
         * Returns the parameter types represented by this instance.
         *
         * @return The parameter types represented by this instance.
         */
        public TypeList getParameterTypes() {
            return new TypeList.Explicit(parameterTypes);
        }

        /**
         * Returns the method descriptor of this method handle representation.
         *
         * @return The method descriptor of this method handle representation.
         */
        public String getDescriptor() {
            switch (handleType) {
                case GET_FIELD:
                case GET_STATIC_FIELD:
                    return returnType.getDescriptor();
                case PUT_FIELD:
                case PUT_STATIC_FIELD:
                    return parameterTypes.get(0).getDescriptor();
                default:
                    StringBuilder stringBuilder = new StringBuilder().append('(');
                    for (TypeDescription parameterType : parameterTypes) {
                        stringBuilder.append(parameterType.getDescriptor());
                    }
                    return stringBuilder.append(')').append(returnType.getDescriptor()).toString();
            }
        }

        @Override
        public int hashCode() {
            int result = handleType.hashCode();
            result = 31 * result + ownerType.hashCode();
            result = 31 * result + name.hashCode();
            result = 31 * result + returnType.hashCode();
            result = 31 * result + parameterTypes.hashCode();
            return result;
        }

        @Override
        public boolean equals(@MaybeNull Object other) {
            if (this == other) {
                return true;
            } else if (!(other instanceof MethodHandle)) {
                return false;
            }
            MethodHandle methodHandle = (MethodHandle) other;
            return handleType == methodHandle.handleType
                    && name.equals(methodHandle.name)
                    && ownerType.equals(methodHandle.ownerType)
                    && parameterTypes.equals(methodHandle.parameterTypes)
                    && returnType.equals(methodHandle.returnType);
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder()
                    .append(handleType.name())
                    .append(ownerType.isInterface() && !handleType.isField() && handleType != HandleType.INVOKE_INTERFACE
                            ? "@interface"
                            : "")
                    .append('/')
                    .append(ownerType.getSimpleName())
                    .append("::")
                    .append(name)
                    .append('(');
            boolean first = true;
            for (TypeDescription typeDescription : parameterTypes) {
                if (first) {
                    first = false;
                } else {
                    stringBuilder.append(',');
                }
                stringBuilder.append(typeDescription.getSimpleName());
            }
            return stringBuilder.append(')').append(returnType.getSimpleName()).toString();
        }

        /**
         * A representation of a method handle's type.
         */
        public enum HandleType {

            /**
             * A handle representing an invokevirtual invocation.
             */
            INVOKE_VIRTUAL(Opcodes.H_INVOKEVIRTUAL, false),

            /**
             * A handle representing an invokestatic invocation.
             */
            INVOKE_STATIC(Opcodes.H_INVOKESTATIC, false),

            /**
             * A handle representing an invokespecial invocation for a non-constructor.
             */
            INVOKE_SPECIAL(Opcodes.H_INVOKESPECIAL, false),

            /**
             * A handle representing an invokeinterface invocation.
             */
            INVOKE_INTERFACE(Opcodes.H_INVOKEINTERFACE, false),

            /**
             * A handle representing an invokespecial invocation for a constructor.
             */
            INVOKE_SPECIAL_CONSTRUCTOR(Opcodes.H_NEWINVOKESPECIAL, false),

            /**
             * A handle representing a write of a non-static field invocation.
             */
            PUT_FIELD(Opcodes.H_PUTFIELD, true),

            /**
             * A handle representing a read of a non-static field invocation.
             */
            GET_FIELD(Opcodes.H_GETFIELD, true),

            /**
             * A handle representing a write of a static field invocation.
             */
            PUT_STATIC_FIELD(Opcodes.H_PUTSTATIC, true),

            /**
             * A handle representing a read of a static field invocation.
             */
            GET_STATIC_FIELD(Opcodes.H_GETSTATIC, true);

            /**
             * The represented identifier.
             */
            private final int identifier;

            /**
             * {@code} true if this handle type represents a field handle.
             */
            private final boolean field;

            /**
             * Creates a new handle type.
             *
             * @param identifier The represented identifier.
             * @param field      {@code} true if this handle type represents a field handle.
             */
            HandleType(int identifier, boolean field) {
                this.identifier = identifier;
                this.field = field;
            }

            /**
             * Extracts a handle type for invoking the given method.
             *
             * @param methodDescription The method for which a handle type should be found.
             * @return The handle type for the given method.
             */
            protected static HandleType of(MethodDescription.InDefinedShape methodDescription) {
                if (methodDescription.isTypeInitializer()) {
                    throw new IllegalArgumentException("Cannot create handle of type initializer " + methodDescription);
                } else if (methodDescription.isStatic()) {
                    return INVOKE_STATIC;
                } else if (methodDescription.isConstructor()) { // Private constructors must use this handle type.
                    return INVOKE_SPECIAL_CONSTRUCTOR;
                } else if (methodDescription.isPrivate()) {
                    return INVOKE_SPECIAL;
                } else if (methodDescription.getDeclaringType().isInterface()) {
                    return INVOKE_INTERFACE;
                } else {
                    return INVOKE_VIRTUAL;
                }
            }

            /**
             * Extracts a handle type for the given identifier.
             *
             * @param identifier The identifier to extract a handle type for.
             * @return The representing handle type.
             */
            protected static HandleType of(int identifier) {
                for (HandleType handleType : HandleType.values()) {
                    if (handleType.getIdentifier() == identifier) {
                        return handleType;
                    }
                }
                throw new IllegalArgumentException("Unknown handle type: " + identifier);
            }

            /**
             * Extracts a handle type for invoking the given method via invokespecial.
             *
             * @param methodDescription The method for which a handle type should be found.
             * @return The handle type for the given method.
             */
            protected static HandleType ofSpecial(MethodDescription.InDefinedShape methodDescription) {
                if (methodDescription.isStatic() || methodDescription.isAbstract()) {
                    throw new IllegalArgumentException("Cannot invoke " + methodDescription + " via invokespecial");
                }
                return methodDescription.isConstructor()
                        ? INVOKE_SPECIAL_CONSTRUCTOR
                        : INVOKE_SPECIAL;
            }

            /**
             * Extracts a handle type for a getter of the given field.
             *
             * @param fieldDescription The field for which to create a getter handle.
             * @return The corresponding handle type.
             */
            protected static HandleType ofGetter(FieldDescription.InDefinedShape fieldDescription) {
                return fieldDescription.isStatic()
                        ? GET_STATIC_FIELD
                        : GET_FIELD;
            }

            /**
             * Extracts a handle type for a setter of the given field.
             *
             * @param fieldDescription The field for which to create a setter handle.
             * @return The corresponding handle type.
             */
            protected static HandleType ofSetter(FieldDescription.InDefinedShape fieldDescription) {
                return fieldDescription.isStatic()
                        ? PUT_STATIC_FIELD
                        : PUT_FIELD;
            }

            /**
             * Returns the represented identifier.
             *
             * @return The represented identifier.
             */
            public int getIdentifier() {
                return identifier;
            }

            /**
             * Returns {@code} true if this handle type represents a field handle.
             *
             * @return {@code} true if this handle type represents a field handle.
             */
            public boolean isField() {
                return field;
            }
        }

        /**
         * A dispatcher to interact with {@code java.lang.invoke.MethodHandleInfo}.
         */
        @JavaDispatcher.Proxied("java.lang.invoke.MethodHandleInfo")
        protected interface MethodHandleInfo {

            /**
             * Returns the name of the method handle info.
             *
             * @param value The {@code java.lang.invoke.MethodHandleInfo} to resolve.
             * @return The name of the method handle info.
             */
            String getName(Object value);

            /**
             * Returns the declaring type of the method handle info.
             *
             * @param value The {@code java.lang.invoke.MethodHandleInfo} to resolve.
             * @return The declaring type of the method handle info.
             */
            Class<?> getDeclaringClass(Object value);

            /**
             * Returns the reference kind of the method handle info.
             *
             * @param value The {@code java.lang.invoke.MethodHandleInfo} to resolve.
             * @return The reference kind of the method handle info.
             */
            int getReferenceKind(Object value);

            /**
             * Returns the {@code java.lang.invoke.MethodType} of the method handle info.
             *
             * @param value The {@code java.lang.invoke.MethodHandleInfo} to resolve.
             * @return The {@code java.lang.invoke.MethodType} of the method handle info.
             */
            Object getMethodType(Object value);

            /**
             * Returns the {@code java.lang.invoke.MethodHandleInfo} of the provided method handle. This method
             * was available on Java 7 but replaced by a lookup-based method in Java 8 and later.
             *
             * @param handle The {@code java.lang.invoke.MethodHandle} to resolve.
             * @return A {@code java.lang.invoke.MethodHandleInfo} to describe the supplied method handle.
             */
            @JavaDispatcher.IsConstructor
            Object revealDirect(@JavaDispatcher.Proxied("java.lang.invoke.MethodHandle") Object handle);
        }

        /**
         * A dispatcher to interact with {@code java.lang.invoke.MethodType}.
         */
        @JavaDispatcher.Proxied("java.lang.invoke.MethodType")
        protected interface MethodType {

            /**
             * Resolves a method type's return type.
             *
             * @param value The {@code java.lang.invoke.MethodType} to resolve.
             * @return The method type's return type.
             */
            Class<?> returnType(Object value);

            /**
             * Resolves a method type's parameter type.
             *
             * @param value The {@code java.lang.invoke.MethodType} to resolve.
             * @return The method type's parameter types.
             */
            Class<?>[] parameterArray(Object value);
        }

        /**
         * A dispatcher to interact with {@code java.lang.invoke.MethodHandles}.
         */
        @JavaDispatcher.Proxied("java.lang.invoke.MethodHandles")
        protected interface MethodHandles {

            /**
             * Resolves the public {@code java.lang.invoke.MethodHandles$Lookup}.
             *
             * @return The public {@code java.lang.invoke.MethodHandles$Lookup}.
             */
            @JavaDispatcher.IsStatic
            Object publicLookup();

            /**
             * A dispatcher to interact with {@code java.lang.invoke.MethodHandles$Lookup}.
             */
            @JavaDispatcher.Proxied("java.lang.invoke.MethodHandles$Lookup")
            interface Lookup {

                /**
                 * Resolves the lookup type for a given lookup instance.
                 *
                 * @param value The {@code java.lang.invoke.MethodHandles$Lookup} to resolve.
                 * @return The lookup's lookup class.
                 */
                Class<?> lookupClass(Object value);

                /**
                 * Reveals the {@code java.lang.invoke.MethodHandleInfo} for the supplied method handle.
                 *
                 * @param value  The {@code java.lang.invoke.MethodHandles$Lookup} to use for resolving the supplied handle
                 * @param handle The {@code java.lang.invoke.MethodHandle} to resolve.
                 * @return A {@code java.lang.invoke.MethodHandleInfo} representing the supplied method handle.
                 */
                Object revealDirect(Object value, @JavaDispatcher.Proxied("java.lang.invoke.MethodHandle") Object handle);
            }
        }
    }

    /**
     * Represents a dynamically resolved constant pool entry of a class file. This feature is supported for class files in version 11 and newer.
     */
    class Dynamic implements JavaConstant {

        /**
         * The default name of a dynamic constant.
         */
        public static final String DEFAULT_NAME = "_";

        /**
         * The name of the dynamic constant.
         */
        private final String name;

        /**
         * A description of the represented value's type.
         */
        private final TypeDescription typeDescription;

        /**
         * A handle representation of the bootstrap method.
         */
        private final MethodHandle bootstrap;

        /**
         * A list of the arguments to the dynamic constant.
         */
        private final List<JavaConstant> arguments;

        /**
         * Creates a dynamic resolved constant.
         *
         * @param name            The name of the dynamic constant.
         * @param typeDescription A description of the represented value's type.
         * @param bootstrap       A handle representation of the bootstrap method.
         * @param arguments       A list of the arguments to the dynamic constant.
         */
        protected Dynamic(String name, TypeDescription typeDescription, MethodHandle bootstrap, List<JavaConstant> arguments) {
            this.name = name;
            this.typeDescription = typeDescription;
            this.bootstrap = bootstrap;
            this.arguments = arguments;
        }

        /**
         * Returns a constant {@code null} value of type {@link Object}.
         *
         * @return A dynamically resolved null constant.
         */
        public static Dynamic ofNullConstant() {
            return new Dynamic(DEFAULT_NAME,
                    TypeDescription.ForLoadedType.of(Object.class),
                    new MethodHandle(MethodHandle.HandleType.INVOKE_STATIC,
                            JavaType.CONSTANT_BOOTSTRAPS.getTypeStub(),
                            "nullConstant",
                            TypeDescription.ForLoadedType.of(Object.class),
                            Arrays.asList(JavaType.METHOD_HANDLES_LOOKUP.getTypeStub(), TypeDescription.ForLoadedType.of(String.class), TypeDescription.ForLoadedType.of(Class.class))),
                    Collections.<JavaConstant>emptyList());
        }

        /**
         * Returns a {@link Class} constant for a primitive type.
         *
         * @param type The primitive type to represent.
         * @return A dynamically resolved primitive type constant.
         */
        public static JavaConstant ofPrimitiveType(Class<?> type) {
            return ofPrimitiveType(TypeDescription.ForLoadedType.of(type));
        }

        /**
         * Returns a {@link Class} constant for a primitive type.
         *
         * @param typeDescription The primitive type to represent.
         * @return A dynamically resolved primitive type constant.
         */
        public static JavaConstant ofPrimitiveType(TypeDescription typeDescription) {
            if (!typeDescription.isPrimitive()) {
                throw new IllegalArgumentException("Not a primitive type: " + typeDescription);
            }
            return new Dynamic(typeDescription.getDescriptor(),
                    TypeDescription.ForLoadedType.of(Class.class),
                    new MethodHandle(MethodHandle.HandleType.INVOKE_STATIC,
                            JavaType.CONSTANT_BOOTSTRAPS.getTypeStub(),
                            "primitiveClass",
                            TypeDescription.ForLoadedType.of(Class.class),
                            Arrays.asList(JavaType.METHOD_HANDLES_LOOKUP.getTypeStub(), TypeDescription.ForLoadedType.of(String.class), TypeDescription.ForLoadedType.of(Class.class))),
                    Collections.<JavaConstant>emptyList());
        }

        /**
         * Returns a {@link Enum} value constant.
         *
         * @param enumeration The enumeration value to represent.
         * @return A dynamically resolved enumeration constant.
         */
        public static JavaConstant ofEnumeration(Enum<?> enumeration) {
            return ofEnumeration(new EnumerationDescription.ForLoadedEnumeration(enumeration));
        }

        /**
         * Returns a {@link Enum} value constant.
         *
         * @param enumerationDescription The enumeration value to represent.
         * @return A dynamically resolved enumeration constant.
         */
        public static JavaConstant ofEnumeration(EnumerationDescription enumerationDescription) {
            return new Dynamic(enumerationDescription.getValue(),
                    enumerationDescription.getEnumerationType(),
                    new MethodHandle(MethodHandle.HandleType.INVOKE_STATIC,
                            JavaType.CONSTANT_BOOTSTRAPS.getTypeStub(),
                            "enumConstant",
                            TypeDescription.ForLoadedType.of(Enum.class),
                            Arrays.asList(JavaType.METHOD_HANDLES_LOOKUP.getTypeStub(), TypeDescription.ForLoadedType.of(String.class), TypeDescription.ForLoadedType.of(Class.class))),
                    Collections.<JavaConstant>emptyList());
        }

        /**
         * Returns a {@code static}, {@code final} field constant.
         *
         * @param field The field to represent a value of.
         * @return A dynamically resolved field value constant.
         */
        public static Dynamic ofField(Field field) {
            return ofField(new FieldDescription.ForLoadedField(field));
        }

        /**
         * Returns a {@code static}, {@code final} field constant.
         *
         * @param fieldDescription The field to represent a value of.
         * @return A dynamically resolved field value constant.
         */
        public static Dynamic ofField(FieldDescription.InDefinedShape fieldDescription) {
            if (!fieldDescription.isStatic() || !fieldDescription.isFinal()) {
                throw new IllegalArgumentException("Field must be static and final: " + fieldDescription);
            }
            boolean selfDeclared = fieldDescription.getType().isPrimitive()
                    ? fieldDescription.getType().asErasure().asBoxed().equals(fieldDescription.getType().asErasure())
                    : fieldDescription.getDeclaringType().equals(fieldDescription.getType().asErasure());
            return new Dynamic(fieldDescription.getInternalName(),
                    fieldDescription.getType().asErasure(),
                    new MethodHandle(MethodHandle.HandleType.INVOKE_STATIC,
                            JavaType.CONSTANT_BOOTSTRAPS.getTypeStub(),
                            "getStaticFinal",
                            TypeDescription.ForLoadedType.of(Object.class),
                            selfDeclared
                                    ? Arrays.asList(JavaType.METHOD_HANDLES_LOOKUP.getTypeStub(), TypeDescription.ForLoadedType.of(String.class), TypeDescription.ForLoadedType.of(Class.class))
                                    : Arrays.asList(JavaType.METHOD_HANDLES_LOOKUP.getTypeStub(), TypeDescription.ForLoadedType.of(String.class), TypeDescription.ForLoadedType.of(Class.class), TypeDescription.ForLoadedType.of(Class.class))),
                    selfDeclared
                            ? Collections.<JavaConstant>emptyList()
                            : Collections.singletonList(JavaConstant.Simple.of(fieldDescription.getDeclaringType())));
        }

        /**
         * Represents a constant that is resolved by invoking a {@code static} factory method.
         *
         * @param method   The method to invoke to create the represented constant value.
         * @param constant The method's constant arguments.
         * @return A dynamic constant that is resolved by the supplied factory method.
         */
        public static Dynamic ofInvocation(Method method, Object... constant) {
            return ofInvocation(method, Arrays.asList(constant));
        }

        /**
         * Represents a constant that is resolved by invoking a {@code static} factory method.
         *
         * @param method    The method to invoke to create the represented constant value.
         * @param constants The constant values passed to the bootstrap method. Values can be represented either
         *                  as {@link TypeDescription}, as {@link JavaConstant}, as {@link String} or a primitive
         *                  {@code int}, {@code long}, {@code float} or {@code double} represented as wrapper type.
         * @return A dynamic constant that is resolved by the supplied factory method.
         */
        public static Dynamic ofInvocation(Method method, List<?> constants) {
            return ofInvocation(new MethodDescription.ForLoadedMethod(method), constants);
        }

        /**
         * Represents a constant that is resolved by invoking a constructor.
         *
         * @param constructor The constructor to invoke to create the represented constant value.
         * @param constant    The constant values passed to the bootstrap method. Values can be represented either
         *                    as {@link TypeDescription}, as {@link JavaConstant}, as {@link String} or a primitive
         *                    {@code int}, {@code long}, {@code float} or {@code double} represented as wrapper type.
         * @return A dynamic constant that is resolved by the supplied constuctor.
         */
        public static Dynamic ofInvocation(Constructor<?> constructor, Object... constant) {
            return ofInvocation(constructor, Arrays.asList(constant));
        }

        /**
         * Represents a constant that is resolved by invoking a constructor.
         *
         * @param constructor The constructor to invoke to create the represented constant value.
         * @param constants   The constant values passed to the bootstrap method. Values can be represented either
         *                    as {@link TypeDescription}, as {@link JavaConstant}, as {@link String} or a primitive
         *                    {@code int}, {@code long}, {@code float} or {@code double} represented as wrapper type.
         * @return A dynamic constant that is resolved by the supplied constuctor.
         */
        public static Dynamic ofInvocation(Constructor<?> constructor, List<?> constants) {
            return ofInvocation(new MethodDescription.ForLoadedConstructor(constructor), constants);
        }

        /**
         * Represents a constant that is resolved by invoking a {@code static} factory method or a constructor.
         *
         * @param methodDescription The method or constructor to invoke to create the represented constant value.
         * @param constant          The constant values passed to the bootstrap method. Values can be represented either
         *                          as {@link TypeDescription}, as {@link JavaConstant}, as {@link String} or a primitive
         *                          {@code int}, {@code long}, {@code float} or {@code double} represented as wrapper type.
         * @return A dynamic constant that is resolved by the supplied factory method or constructor.
         */
        public static Dynamic ofInvocation(MethodDescription.InDefinedShape methodDescription, Object... constant) {
            return ofInvocation(methodDescription, Arrays.asList(constant));
        }

        /**
         * Represents a constant that is resolved by invoking a {@code static} factory method or a constructor.
         *
         * @param methodDescription The method or constructor to invoke to create the represented constant value.
         * @param constants         The constant values passed to the bootstrap method. Values can be represented either
         *                          as {@link TypeDescription}, as {@link JavaConstant}, as {@link String} or a primitive
         *                          {@code int}, {@code long}, {@code float} or {@code double} represented as wrapper type.
         * @return A dynamic constant that is resolved by the supplied factory method or constructor.
         */
        public static Dynamic ofInvocation(MethodDescription.InDefinedShape methodDescription, List<?> constants) {
            if (!methodDescription.isConstructor() && methodDescription.getReturnType().represents(void.class)) {
                throw new IllegalArgumentException("Bootstrap method is no constructor or non-void static factory: " + methodDescription);
            } else if (methodDescription.isVarArgs()
                    ? methodDescription.getParameters().size() + (methodDescription.isStatic() || methodDescription.isConstructor() ? 0 : 1) > constants.size() + 1
                    : methodDescription.getParameters().size() + (methodDescription.isStatic() || methodDescription.isConstructor() ? 0 : 1) != constants.size()) {
                throw new IllegalArgumentException("Cannot assign " + constants + " to " + methodDescription);
            }
            List<TypeDescription> parameters = (methodDescription.isStatic() || methodDescription.isConstructor()
                    ? methodDescription.getParameters().asTypeList().asErasures()
                    : CompoundList.of(methodDescription.getDeclaringType(), methodDescription.getParameters().asTypeList().asErasures()));
            Iterator<TypeDescription> iterator;
            if (methodDescription.isVarArgs()) {
                iterator = CompoundList.of(parameters.subList(0, parameters.size() - 1), Collections.nCopies(
                        constants.size() - parameters.size() + 1,
                        parameters.get(parameters.size() - 1).getComponentType())).iterator();
            } else {
                iterator = parameters.iterator();
            }
            List<JavaConstant> arguments = new ArrayList<JavaConstant>(constants.size() + 1);
            arguments.add(MethodHandle.of(methodDescription));
            for (Object constant : constants) {
                JavaConstant argument = JavaConstant.Simple.wrap(constant);
                if (!argument.getTypeDescription().isAssignableTo(iterator.next())) {
                    throw new IllegalArgumentException("Cannot assign " + constants + " to " + methodDescription);
                }
                arguments.add(argument);
            }
            return new Dynamic(DEFAULT_NAME,
                    methodDescription.isConstructor()
                            ? methodDescription.getDeclaringType()
                            : methodDescription.getReturnType().asErasure(),
                    new MethodHandle(MethodHandle.HandleType.INVOKE_STATIC,
                            JavaType.CONSTANT_BOOTSTRAPS.getTypeStub(),
                            "invoke",
                            TypeDescription.ForLoadedType.of(Object.class),
                            Arrays.asList(JavaType.METHOD_HANDLES_LOOKUP.getTypeStub(),
                                    TypeDescription.ForLoadedType.of(String.class),
                                    TypeDescription.ForLoadedType.of(Class.class),
                                    JavaType.METHOD_HANDLE.getTypeStub(),
                                    TypeDescription.ArrayProjection.of(TypeDescription.ForLoadedType.of(Object.class)))),
                    arguments);
        }

        /**
         * Resolves a var handle constant for a field.
         *
         * @param field The field to represent a var handle for.
         * @return A dynamic constant that represents the created var handle constant.
         */
        public static JavaConstant ofVarHandle(Field field) {
            return ofVarHandle(new FieldDescription.ForLoadedField(field));
        }

        /**
         * Resolves a var handle constant for a field.
         *
         * @param fieldDescription The field to represent a var handle for.
         * @return A dynamic constant that represents the created var handle constant.
         */
        public static JavaConstant ofVarHandle(FieldDescription.InDefinedShape fieldDescription) {
            return new Dynamic(fieldDescription.getInternalName(),
                    JavaType.VAR_HANDLE.getTypeStub(),
                    new MethodHandle(MethodHandle.HandleType.INVOKE_STATIC,
                            JavaType.CONSTANT_BOOTSTRAPS.getTypeStub(),
                            fieldDescription.isStatic()
                                    ? "staticFieldVarHandle"
                                    : "fieldVarHandle",
                            JavaType.VAR_HANDLE.getTypeStub(),
                            Arrays.asList(JavaType.METHOD_HANDLES_LOOKUP.getTypeStub(),
                                    TypeDescription.ForLoadedType.of(String.class),
                                    TypeDescription.ForLoadedType.of(Class.class),
                                    TypeDescription.ForLoadedType.of(Class.class),
                                    TypeDescription.ForLoadedType.of(Class.class))),
                    Arrays.asList(JavaConstant.Simple.of(fieldDescription.getDeclaringType()), JavaConstant.Simple.of(fieldDescription.getType().asErasure())));
        }

        /**
         * Resolves a var handle constant for an array.
         *
         * @param type The array type for which the var handle is resolved.
         * @return A dynamic constant that represents the created var handle constant.
         */
        public static JavaConstant ofArrayVarHandle(Class<?> type) {
            return ofArrayVarHandle(TypeDescription.ForLoadedType.of(type));
        }

        /**
         * Resolves a var handle constant for an array.
         *
         * @param typeDescription The array type for which the var handle is resolved.
         * @return A dynamic constant that represents the created var handle constant.
         */
        public static JavaConstant ofArrayVarHandle(TypeDescription typeDescription) {
            if (!typeDescription.isArray()) {
                throw new IllegalArgumentException("Not an array type: " + typeDescription);
            }
            return new Dynamic(DEFAULT_NAME,
                    JavaType.VAR_HANDLE.getTypeStub(),
                    new MethodHandle(MethodHandle.HandleType.INVOKE_STATIC,
                            JavaType.CONSTANT_BOOTSTRAPS.getTypeStub(),
                            "arrayVarHandle",
                            JavaType.VAR_HANDLE.getTypeStub(),
                            Arrays.asList(JavaType.METHOD_HANDLES_LOOKUP.getTypeStub(),
                                    TypeDescription.ForLoadedType.of(String.class),
                                    TypeDescription.ForLoadedType.of(Class.class),
                                    TypeDescription.ForLoadedType.of(Class.class))),
                    Collections.singletonList(JavaConstant.Simple.of(typeDescription)));
        }

        /**
         * Binds the supplied bootstrap method for the resolution of a dynamic constant.
         *
         * @param name     The name of the bootstrap constant that is provided to the bootstrap method or constructor.
         * @param method   The bootstrap method to invoke.
         * @param constant The arguments for the bootstrap method represented as primitive wrapper types,
         *                 {@link String}, {@link TypeDescription} or {@link JavaConstant} values or their loaded forms.
         * @return A dynamic constant that represents the bootstrapped method's result.
         */
        public static Dynamic bootstrap(String name, Method method, Object... constant) {
            return bootstrap(name, method, Arrays.asList(constant));
        }

        /**
         * Binds the supplied bootstrap method for the resolution of a dynamic constant.
         *
         * @param name      The name of the bootstrap constant that is provided to the bootstrap method or constructor.
         * @param method    The bootstrap method to invoke.
         * @param constants The constant values passed to the bootstrap method. Values can be represented either
         *                  as {@link TypeDescription}, as {@link JavaConstant}, as {@link String} or a primitive
         *                  {@code int}, {@code long}, {@code float} or {@code double} represented as wrapper type.
         * @return A dynamic constant that represents the bootstrapped method's result.
         */
        public static Dynamic bootstrap(String name, Method method, List<?> constants) {
            return bootstrap(name, new MethodDescription.ForLoadedMethod(method), constants);
        }

        /**
         * Binds the supplied bootstrap constructor for the resolution of a dynamic constant.
         *
         * @param name        The name of the bootstrap constant that is provided to the bootstrap method or constructor.
         * @param constructor The bootstrap constructor to invoke.
         * @param constant    The constant values passed to the bootstrap method. Values can be represented either
         *                    as {@link TypeDescription}, as {@link JavaConstant}, as {@link String} or a primitive
         *                    {@code int}, {@code long}, {@code float} or {@code double} represented as wrapper type.
         * @return A dynamic constant that represents the bootstrapped constructor's result.
         */
        public static Dynamic bootstrap(String name, Constructor<?> constructor, Object... constant) {
            return bootstrap(name, constructor, Arrays.asList(constant));
        }

        /**
         * Binds the supplied bootstrap constructor for the resolution of a dynamic constant.
         *
         * @param name        The name of the bootstrap constant that is provided to the bootstrap method or constructor.
         * @param constructor The bootstrap constructor to invoke.
         * @param constants   The constant values passed to the bootstrap method. Values can be represented either
         *                    as {@link TypeDescription}, as {@link JavaConstant}, as {@link String} or a primitive
         *                    {@code int}, {@code long}, {@code float} or {@code double} represented as wrapper type.
         * @return A dynamic constant that represents the bootstrapped constructor's result.
         */
        public static Dynamic bootstrap(String name, Constructor<?> constructor, List<?> constants) {
            return bootstrap(name, new MethodDescription.ForLoadedConstructor(constructor), constants);
        }

        /**
         * Binds the supplied bootstrap method or constructor for the resolution of a dynamic constant.
         *
         * @param name            The name of the bootstrap constant that is provided to the bootstrap method or constructor.
         * @param bootstrapMethod The bootstrap method or constructor to invoke.
         * @param constant        The constant values passed to the bootstrap method. Values can be represented either
         *                        as {@link TypeDescription}, as {@link JavaConstant}, as {@link String} or a primitive
         *                        {@code int}, {@code long}, {@code float} or {@code double} represented as wrapper type.
         * @return A dynamic constant that represents the bootstrapped method's or constructor's result.
         */
        public static Dynamic bootstrap(String name, MethodDescription.InDefinedShape bootstrapMethod, Object... constant) {
            return bootstrap(name, bootstrapMethod, Arrays.asList(constant));
        }

        /**
         * Binds the supplied bootstrap method or constructor for the resolution of a dynamic constant.
         *
         * @param name      The name of the bootstrap constant that is provided to the bootstrap method or constructor.
         * @param bootstrap The bootstrap method or constructor to invoke.
         * @param arguments The constant values passed to the bootstrap method. Values can be represented either
         *                  as {@link TypeDescription}, as {@link JavaConstant}, as {@link String} or a primitive
         *                  {@code int}, {@code long}, {@code float} or {@code double} represented as wrapper type.
         * @return A dynamic constant that represents the bootstrapped method's or constructor's result.
         */
        public static Dynamic bootstrap(String name, MethodDescription.InDefinedShape bootstrap, List<?> arguments) {
            if (name.length() == 0 || name.contains(".")) {
                throw new IllegalArgumentException("Not a valid field name: " + name);
            }
            List<JavaConstant> constants = new ArrayList<JavaConstant>(arguments.size());
            for (Object argument : arguments) {
                constants.add(JavaConstant.Simple.wrap(argument));
            }
            if (!bootstrap.isConstantBootstrap(TypeList.Explicit.of(constants))) {
                throw new IllegalArgumentException("Not a valid bootstrap method " + bootstrap + " for " + arguments);
            }
            return new Dynamic(name,
                    bootstrap.isConstructor()
                            ? bootstrap.getDeclaringType()
                            : bootstrap.getReturnType().asErasure(),
                    new MethodHandle(bootstrap.isConstructor() ? MethodHandle.HandleType.INVOKE_SPECIAL_CONSTRUCTOR : MethodHandle.HandleType.INVOKE_STATIC,
                            bootstrap.getDeclaringType(),
                            bootstrap.getInternalName(),
                            bootstrap.getReturnType().asErasure(),
                            bootstrap.getParameters().asTypeList().asErasures()),
                    constants);
        }

        /**
         * Returns the name of the dynamic constant.
         *
         * @return The name of the dynamic constant.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns a handle representation of the bootstrap method.
         *
         * @return A handle representation of the bootstrap method.
         */
        public MethodHandle getBootstrap() {
            return bootstrap;
        }

        /**
         * Returns a list of the arguments to the dynamic constant.
         *
         * @return A list of the arguments to the dynamic constant.
         */
        public List<JavaConstant> getArguments() {
            return arguments;
        }

        /**
         * Resolves this {@link Dynamic} constant to resolve the returned instance to the supplied type. The type must be a subtype of the
         * bootstrap method's return type. Constructors cannot be resolved to a different type.
         *
         * @param type The type to resolve the bootstrapped value to.
         * @return This dynamic constant but resolved to the supplied type.
         */
        public JavaConstant withType(Class<?> type) {
            return withType(TypeDescription.ForLoadedType.of(type));
        }

        /**
         * Resolves this {@link Dynamic} constant to resolve the returned instance to the supplied type. The type must be a subtype of the
         * bootstrap method's return type. Constructors cannot be resolved to a different type.
         *
         * @param typeDescription The type to resolve the bootstrapped value to.
         * @return This dynamic constant but resolved to the supplied type.
         */
        public JavaConstant withType(TypeDescription typeDescription) {
            if (typeDescription.represents(void.class)) {
                throw new IllegalArgumentException("Constant value cannot represent void");
            } else if (getBootstrap().getName().equals(MethodDescription.CONSTRUCTOR_INTERNAL_NAME)
                    ? !getTypeDescription().isAssignableTo(typeDescription)
                    : (!typeDescription.asBoxed().isInHierarchyWith(getTypeDescription().asBoxed()))) {
                throw new IllegalArgumentException(typeDescription + " is not compatible with bootstrapped type " + getTypeDescription());
            }
            return new Dynamic(getName(), typeDescription, getBootstrap(), getArguments());
        }

        /**
         * {@inheritDoc}
         */
        public Object toDescription() {
            Object[] argument = JavaConstant.Simple.CONSTANT_DESC.toArray(arguments.size());
            for (int index = 0; index < argument.length; index++) {
                argument[index] = arguments.get(index).toDescription();
            }
            return JavaConstant.Simple.DYNAMIC_CONSTANT_DESC.ofCanonical(JavaConstant.Simple.METHOD_HANDLE_DESC.of(
                    JavaConstant.Simple.DIRECT_METHOD_HANDLE_DESC_KIND.valueOf(bootstrap.getHandleType().getIdentifier(), bootstrap.getOwnerType().isInterface()),
                    JavaConstant.Simple.CLASS_DESC.ofDescriptor(bootstrap.getOwnerType().getDescriptor()),
                    bootstrap.getName(),
                    bootstrap.getDescriptor()), getName(), JavaConstant.Simple.CLASS_DESC.ofDescriptor(typeDescription.getDescriptor()), argument);
        }

        /**
         * {@inheritDoc}
         */
        public <T> T accept(Visitor<T> visitor) {
            return visitor.onDynamic(this);
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getTypeDescription() {
            return typeDescription;
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation toStackManipulation() {
            return new JavaConstantValue(this);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + typeDescription.hashCode();
            result = 31 * result + bootstrap.hashCode();
            result = 31 * result + arguments.hashCode();
            return result;
        }

        @Override
        public boolean equals(@MaybeNull Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            Dynamic dynamic = (Dynamic) object;
            if (!name.equals(dynamic.name)) return false;
            if (!typeDescription.equals(dynamic.typeDescription)) return false;
            if (!bootstrap.equals(dynamic.bootstrap)) return false;
            return arguments.equals(dynamic.arguments);
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder()
                    .append(bootstrap.getOwnerType().getSimpleName())
                    .append("::")
                    .append(bootstrap.getName())
                    .append('(')
                    .append(name.equals(DEFAULT_NAME) ? "" : name)
                    .append('/');
            boolean first = true;
            for (JavaConstant constant : arguments) {
                if (first) {
                    first = false;
                } else {
                    stringBuilder.append(',');
                }
                stringBuilder.append(constant.toString());
            }
            return stringBuilder.append(')').append(typeDescription.getSimpleName()).toString();
        }
    }
}
