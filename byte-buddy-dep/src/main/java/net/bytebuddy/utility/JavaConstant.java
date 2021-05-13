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
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.constant.*;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * Returns a Java instance of an object that has a special meaning to the Java virtual machine and that is not
 * available to Java in versions 6.
 */
public interface JavaConstant {

    /**
     * Returns the represented instance as a constant pool value.
     *
     * @return The constant pool value in a format that can be written by ASM.
     */
    Object asConstantPoolValue();

    /**
     * Returns this constant as a Java {@code java.lang.constant.ConstantDesc} if the current VM is of at least version 12.
     * If the current VM is of an older version and does not support the type, an exception is thrown.
     *
     * @return This constant as a Java {@code java.lang.constant.ConstantDesc}.
     */
    Object asConstantDescription();

    /**
     * Returns a description of the type of the represented instance or at least a stub.
     *
     * @return A description of the type of the represented instance or at least a stub.
     */
    TypeDescription getTypeDescription();

    /**
     * Represents a simple Java constant, either a primitive constant, a {@link String} or a {@link Class}.
     */
    class Simple implements JavaConstant {

        /**
         * A dispatcher for interaction with {@code java.lang.constant.ConstantDesc} types.
         */
        protected static final Dispatcher DISPATCHER = AccessController.doPrivileged(Dispatcher.CreationAction.INSTANCE);

        /**
         * The represented constant pool value.
         */
        private final Object value;

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
        protected Simple(Object value, TypeDescription typeDescription) {
            this.value = value;
            this.typeDescription = typeDescription;
        }

        /**
         * Resolves a loaded Java value to a Java constant representation.
         *
         * @param value The value to represent.
         * @return An appropriate Java constant representation.
         */
        public static JavaConstant ofLoaded(Object value) {
            if (value instanceof Integer) {
                return new Simple(value, TypeDescription.ForLoadedType.of(int.class));
            } else if (value instanceof Long) {
                return new Simple(value, TypeDescription.ForLoadedType.of(long.class));
            } else if (value instanceof Float) {
                return new Simple(value, TypeDescription.ForLoadedType.of(float.class));
            } else if (value instanceof Double) {
                return new Simple(value, TypeDescription.ForLoadedType.of(double.class));
            } else if (value instanceof String) {
                return new Simple(value, TypeDescription.STRING);
            } else if (value instanceof Class<?>) {
                return new Simple(Type.getType((Class<?>) value), TypeDescription.CLASS);
            } else if (JavaType.METHOD_HANDLE.isInstance(value)) {
                return MethodHandle.ofLoaded(value);
            } else if (JavaType.METHOD_TYPE.isInstance(value)) {
                return MethodType.ofLoaded(value);
            } else {
                throw new IllegalArgumentException("Not a loaded Java constant value: " + value);
            }
        }

        /**
         * Creates a Java constant value from a {@code java.lang.constant.ConstantDesc}.
         *
         * @param value       The  {@code java.lang.constant.ConstantDesc} to represent.
         * @param classLoader The class loader to use for resolving type information from the supplied value.
         * @return An appropriate Java constant representation.
         */
        public static JavaConstant ofDescription(Object value, ClassLoader classLoader) {
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
                return new Simple(value, TypeDescription.ForLoadedType.of(int.class));
            } else if (value instanceof Long) {
                return new Simple(value, TypeDescription.ForLoadedType.of(long.class));
            } else if (value instanceof Float) {
                return new Simple(value, TypeDescription.ForLoadedType.of(float.class));
            } else if (value instanceof Double) {
                return new Simple(value, TypeDescription.ForLoadedType.of(double.class));
            } else if (value instanceof String) {
                return new Simple(value, TypeDescription.STRING);
            } else if (DISPATCHER.isClassDesc(value)) {
                return new Simple(Type.getType(DISPATCHER.toClassDescDescriptor(value)), TypeDescription.CLASS);
            } else if (DISPATCHER.isMethodTypeDesc(value)) {
                List<?> parameterTypes = DISPATCHER.toMethodTypeDescParameterTypes(value);
                List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(parameterTypes.size());
                for (Object parameterType : parameterTypes) {
                    typeDescriptions.add(typePool.describe(Type.getType(DISPATCHER.toClassDescDescriptor(parameterType)).getClassName()).resolve());
                }
                return MethodType.of(typePool.describe(Type.getType(DISPATCHER.toClassDescDescriptor(DISPATCHER.toMethodTypeDescReturnType(value))).getClassName()).resolve(), typeDescriptions);
            } else if (DISPATCHER.isDirectMethodHandleDesc(value)) {
                List<?> parameterTypes = DISPATCHER.toMethodTypeDescParameterTypes(DISPATCHER.toDirectMethodHandleDescInvocationType(value));
                List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(parameterTypes.size());
                for (Object parameterType : parameterTypes) {
                    typeDescriptions.add(typePool.describe(Type.getType(DISPATCHER.toClassDescDescriptor(parameterType)).getClassName()).resolve());
                }
                return new MethodHandle(MethodHandle.HandleType.of(((DirectMethodHandleDesc) value).refKind()),
                        typePool.describe(Type.getType(((DirectMethodHandleDesc) value).owner().descriptorString()).getClassName()).resolve(),
                        ((DirectMethodHandleDesc) value).methodName(),
                        ((DirectMethodHandleDesc) value).refKind() == Opcodes.H_NEWINVOKESPECIAL
                            ? TypeDescription.VOID
                            : typePool.describe(Type.getType(((DirectMethodHandleDesc) value).invocationType().returnType().descriptorString()).getClassName()).resolve(),
                        typeDescriptions);
            } else if (DISPATCHER.isDynamicConstantDesc(value)) {
                List<ConstantDesc> constants = ((DynamicConstantDesc<?>) value).bootstrapArgsList();
                Object[] argument = new Object[constants.size()];
                for (int index = 0; index < constants.size(); index++) {
                    argument[index] = ofDescription(constants.get(index), typePool).asConstantPoolValue();
                }
                return new Dynamic(new ConstantDynamic(((DynamicConstantDesc<?>) value).constantName(),
                        ((DynamicConstantDesc<?>) value).constantType().descriptorString(),
                        new Handle(((DynamicConstantDesc<?>) value).bootstrapMethod().refKind(),
                                ((DynamicConstantDesc<?>) value).bootstrapMethod().owner().descriptorString(),
                                ((DynamicConstantDesc<?>) value).bootstrapMethod().methodName(),
                                ((DynamicConstantDesc<?>) value).bootstrapMethod().lookupDescriptor(),
                                ((DynamicConstantDesc<?>) value).bootstrapMethod().isOwnerInterface()),
                        argument), typePool.describe(Type.getType(((DynamicConstantDesc<?>) value).constantType().descriptorString()).getClassName()).resolve());
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
            return new Simple(Type.getType(typeDescription.getDescriptor()), TypeDescription.CLASS);
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
         * {@inheritDoc}
         */
        public Object asConstantPoolValue() {
            return value;
        }

        /**
         * {@inheritDoc}
         */
        public Object asConstantDescription() {
            if (value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof Double || value instanceof String) {
                return value;
            } else if (value instanceof Type) {
                return DISPATCHER.toClassDescOfDescriptor(((Type) value).getDescriptor());
            } else {
                throw new IllegalStateException("Cannot resolve to a description: " + value);
            }
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getTypeDescription() {
            return typeDescription;
        }

        @Override
        public int hashCode() {
            int result = value.hashCode();
            result = 31 * result + typeDescription.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            Simple simple = (Simple) object;
            if (!value.equals(simple.value)) return false;
            return typeDescription.equals(simple.typeDescription);
        }

        protected interface Dispatcher {

            /**
             * Returns {@code true} if the supplied instance is of type {@code java.lang.constant.ClassDesc}.
             *
             * @param value The instance to check.
             * @return {@code true} if the supplied instance is of type {@code java.lang.constant.ClassDesc}.
             */
            boolean isClassDesc(Object value);

            /**
             * Returns {@code true} if the supplied instance is of type {@code java.lang.constant.MethodTypeDesc}.
             *
             * @param value The instance to check.
             * @return {@code true} if the supplied instance is of type {@code java.lang.constant.MethodTypeDesc}.
             */
            boolean isMethodTypeDesc(Object value);

            /**
             * Returns {@code true} if the supplied instance is of type {@code java.lang.constant.DirectMethodHandleDesc}.
             *
             * @param value The instance to check.
             * @return {@code true} if the supplied instance is of type {@code java.lang.constant.DirectMethodHandleDesc}.
             */
            boolean isDirectMethodHandleDesc(Object value);

            /**
             * Returns {@code true} if the supplied instance is of type {@code java.lang.constant.DynamicConstantDesc}.
             *
             * @param value The instance to check.
             * @return {@code true} if the supplied instance is of type {@code java.lang.constant.DynamicConstantDesc}.
             */
            boolean isDynamicConstantDesc(Object value);

            /**
             * Returns the descriptor of the supplied class description.
             *
             * @param value The {@code java.lang.constant.ClassDesc} to resolve.
             * @return The class's descriptor.
             */
            String toClassDescDescriptor(Object value);

            /**
             * Resolves a {@code java.lang.constant.ClassDesc} of a descriptor.
             *
             * @param descriptor The descriptor to resolve.
             * @return An appropriate {@code java.lang.constant.ClassDesc}.
             */
            Object toClassDescOfDescriptor(String descriptor);

            /**
             * Returns an array of component type {@code java.lang.constant.ClassDesc}.
             *
             * @param length The length of the array.
             * @return An array of type {@code java.lang.constant.ClassDesc} with the given length.
             */
            Object[] toClassDescArray(int length);

            /**
             * Returns the return type of a {@code java.lang.constant.MethodTypeDesc}.
             *
             * @param value The {@code java.lang.constant.MethodTypeDesc} to resolve.
             * @return A {@code java.lang.constant.ClassDesc} of the supplied {@code java.lang.constant.MethodTypeDesc}'s return type.
             */
            Object toMethodTypeDescReturnType(Object value);

            /**
             * Returns the parameter types of a {@code java.lang.constant.MethodTypeDesc}.
             *
             * @param value The {@code java.lang.constant.MethodTypeDesc} to resolve.
             * @return A list of {@code java.lang.constant.ClassDesc} of the supplied {@code java.lang.constant.MethodTypeDesc}'s parameter types.
             */
            List<?> toMethodTypeDescParameterTypes(Object value);

            /**
             * Resolves a {@code java.lang.constant.MethodTypeDesc} from descriptions of the return type descriptor and parameter types.
             *
             * @param returnType    A {@code java.lang.constant.ClassDesc} representing the return type.
             * @param parameterType An array of {@code java.lang.constant.ClassDesc}s representing the parameter types.
             * @return An appropriate {@code java.lang.constant.MethodTypeDesc}.
             */
            Object toMethodTypeDesc(Object returnType, Object[] parameterType);

            /**
             * Resolves a {@code java.lang.constant.MethodTypeDesc} representing the invocation type of
             * the supplied {@code java.lang.constant.DirectMethodHandleDesc}.
             *
             * @param value The {@code java.lang.constant.DirectMethodHandleDesc} to resolve.
             * @return An {@code java.lang.constant.MethodTypeDesc} representing the invocation type.
             */
            Object toDirectMethodHandleDescInvocationType(Object value);

            /**
             * An action to create an appropriate {@link Dispatcher}.
             */
            enum CreationAction implements PrivilegedAction<Dispatcher> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Dispatcher run() {
                    try {
                        Class<?> classDesc = Class.forName("java.lang.constant.ClassDesc");
                        Class<?> methodTypeDesc = Class.forName("java.lang.constant.MethodTypeDesc");
                        Class<?> directMethodHandleDesc = Class.forName("java.lang.constant.DirectMethodHandleDesc");
                        Class<?> dynamicConstantDesc = Class.forName("java.lang.constant.DynamicConstantDesc");
                        return new ForJava12CapableVm(classDesc,
                                methodTypeDesc,
                                directMethodHandleDesc,
                                dynamicConstantDesc,
                                classDesc.getMethod("descriptorString"),
                                classDesc.getMethod("ofDescriptor", String.class),
                                methodTypeDesc.getMethod("returnType"),
                                methodTypeDesc.getMethod("parameterList"),
                                methodTypeDesc.getMethod("of", classDesc, Class.forName("[L" + classDesc.getName() + ";")),
                                directMethodHandleDesc.getMethod("invocationType"));
                    } catch (Exception ignored) {
                        return ForLegacyVm.INSTANCE;
                    }
                }
            }

            /**
             * A dispatcher for a VM that does not support {@code java.lang.constant.ConstantDesc} types.
             */
            enum ForLegacyVm implements Dispatcher {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public boolean isClassDesc(Object value) {
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isMethodTypeDesc(Object value) {
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isDirectMethodHandleDesc(Object value) {
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isDynamicConstantDesc(Object value) {
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                public String toClassDescDescriptor(Object value) {
                    throw new IllegalStateException("The current VM does not support constant descriptions");
                }

                /**
                 * {@inheritDoc}
                 */
                public Object toClassDescOfDescriptor(String descriptor) {
                    throw new IllegalStateException("The current VM does not support constant descriptions");
                }

                /**
                 * {@inheritDoc}
                 */
                public Object[] toClassDescArray(int length) {
                    throw new IllegalStateException("The current VM does not support constant descriptions");
                }

                /**
                 * {@inheritDoc}
                 */
                public Object toMethodTypeDescReturnType(Object value) {
                    throw new IllegalStateException("The current VM does not support constant descriptions");
                }

                /**
                 * {@inheritDoc}
                 */
                public List<?> toMethodTypeDescParameterTypes(Object value) {
                    throw new IllegalStateException("The current VM does not support constant descriptions");
                }

                /**
                 * {@inheritDoc}
                 */
                public Object toMethodTypeDesc(Object returnType, Object[] parameterType) {
                    throw new IllegalStateException("The current VM does not support constant descriptions");
                }

                /**
                 * {@inheritDoc}
                 */
                public Object toDirectMethodHandleDescInvocationType(Object value) {
                    throw new IllegalStateException("The current VM does not support constant descriptions");
                }
            }

            class ForJava12CapableVm implements Dispatcher {

                private final Class<?> classDesc;

                private final Class<?> methodTypeDesc;

                private final Class<?> directMethodHandleDesc;

                private final Class<?> dynamicConstantDesc;

                private final Method descriptorString;

                private final Method toDescriptor;

                private final Method returnType;

                private final Method parameterList;

                private final Method toMethodType;

                private final Method invocationType;

                protected ForJava12CapableVm(Class<?> classDesc,
                                             Class<?> methodTypeDesc,
                                             Class<?> directMethodHandleDesc,
                                             Class<?> dynamicConstantDesc,
                                             Method descriptorString,
                                             Method toDescriptor,
                                             Method returnType,
                                             Method parameterList,
                                             Method toMethodType,
                                             Method invocationType) {
                    this.classDesc = classDesc;
                    this.methodTypeDesc = methodTypeDesc;
                    this.directMethodHandleDesc = directMethodHandleDesc;
                    this.dynamicConstantDesc = dynamicConstantDesc;
                    this.descriptorString = descriptorString;
                    this.toDescriptor = toDescriptor;
                    this.returnType = returnType;
                    this.parameterList = parameterList;
                    this.toMethodType = toMethodType;
                    this.invocationType = invocationType;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isClassDesc(Object value) {
                    return classDesc.isInstance(value);
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isMethodTypeDesc(Object value) {
                    return methodTypeDesc.isInstance(value);
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isDirectMethodHandleDesc(Object value) {
                    return directMethodHandleDesc.isInstance(value);
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isDynamicConstantDesc(Object value) {
                    return dynamicConstantDesc.isInstance(value);
                }

                /**
                 * {@inheritDoc}
                 */
                public String toClassDescDescriptor(Object value) {
                    try {
                        return (String) descriptorString.invoke(value);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.constant.ClassDesc#descriptorString", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.constant.ClassDesc#descriptorString", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Object toClassDescOfDescriptor(String descriptor) {
                    try {
                        return toDescriptor.invoke(null, descriptor);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.constant.ClassDesc#ofDescriptor", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.constant.ClassDesc#ofDescriptor", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Object[] toClassDescArray(int length) {
                    return (Object[]) Array.newInstance(classDesc, length);
                }

                /**
                 * {@inheritDoc}
                 */
                public Object toMethodTypeDescReturnType(Object value) {
                    try {
                        return returnType.invoke(value);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.constant.MethodTypeDesc#returnType", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.constant.MethodTypeDesc#returnType", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public List<?> toMethodTypeDescParameterTypes(Object value) {
                    try {
                        return (List<?>) parameterList.invoke(value);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.constant.MethodTypeDesc#parameterList", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.constant.MethodTypeDesc#parameterList", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Object toMethodTypeDesc(Object returnType, Object[] parameterType) {
                    try {
                        return toMethodType.invoke(null, returnType, parameterType);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.constant.MethodTypeDesc#of", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.constant.MethodTypeDesc#of", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Object toDirectMethodHandleDescInvocationType(Object value) {
                    try {
                        return invocationType.invoke(value);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.constant.DirectMethodHandleDesc#invocationType", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.constant.DirectMethodHandleDesc#invocationType", exception.getCause());
                    }
                }
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
        private static final Dispatcher DISPATCHER = AccessController.doPrivileged(JavaDispatcher.of(Dispatcher.class));

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
        public static MethodType ofSetter(FieldDescription fieldDescription) {
            return new MethodType(TypeDescription.VOID, Collections.singletonList(fieldDescription.getType().asErasure()));
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
        public static MethodType ofGetter(FieldDescription fieldDescription) {
            return new MethodType(fieldDescription.getType().asErasure(), Collections.<TypeDescription>emptyList());
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
        public Object asConstantPoolValue() {
            StringBuilder stringBuilder = new StringBuilder().append('(');
            for (TypeDescription parameterType : getParameterTypes()) {
                stringBuilder.append(parameterType.getDescriptor());
            }
            return Type.getMethodType(stringBuilder.append(')').append(getReturnType().getDescriptor()).toString());
        }

        /**
         * {@inheritDoc}
         */
        public Object asConstantDescription() {
            Object[] parameterTypes = Simple.DISPATCHER.toClassDescArray(getParameterTypes().size());
            for (int index = 0; index < getParameterTypes().size(); index++) {
                parameterTypes[index] = Simple.DISPATCHER.toClassDescOfDescriptor(getParameterTypes().get(index).getDescriptor());
            }
            return Simple.DISPATCHER.toMethodTypeDesc(Simple.DISPATCHER.toClassDescOfDescriptor(getReturnType().getDescriptor()), parameterTypes);
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getTypeDescription() {
            return JavaType.METHOD_TYPE.getTypeStub();
        }

        @Override
        public int hashCode() {
            int result = returnType.hashCode();
            result = 31 * result + parameterTypes.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof MethodType)) {
                return false;
            }
            MethodType methodType = (MethodType) other;
            return parameterTypes.equals(methodType.parameterTypes) && returnType.equals(methodType.returnType);

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
         * A dispatcher for receiving the type information that is represented by a {@code java.lang.invoke.MethodHandle} instance.
         */
        private static final Dispatcher.Initializable DISPATCHER = AccessController.doPrivileged(Dispatcher.CreationAction.INSTANCE);

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
        protected MethodHandle(HandleType handleType,
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
         * Creates a method handles representation of a loaded method handle which is analyzed using a public {@code MethodHandles.Lookup} object.
         * A method handle can only be analyzed on virtual machines that support the corresponding API (Java 7+). For virtual machines before Java 8+,
         * a method handle instance can only be analyzed by taking advantage of private APIs what might require a access context.
         *
         * @param methodHandle The loaded method handle to represent.
         * @return A representation of the loaded method handle
         */
        public static MethodHandle ofLoaded(Object methodHandle) {
            return ofLoaded(methodHandle, DISPATCHER.publicLookup());
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
            Dispatcher dispatcher = DISPATCHER.initialize();
            Object methodHandleInfo = dispatcher.reveal(lookup, methodHandle);
            Object methodType = dispatcher.getMethodType(methodHandleInfo);
            return new MethodHandle(HandleType.of(dispatcher.getReferenceKind(methodHandleInfo)),
                    TypeDescription.ForLoadedType.of(dispatcher.getDeclaringClass(methodHandleInfo)),
                    dispatcher.getName(methodHandleInfo),
                    TypeDescription.ForLoadedType.of(dispatcher.returnType(methodType)),
                    new TypeList.ForLoadedTypes(dispatcher.parameterArray(methodType)));
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
                    TypeDescription.VOID,
                    Collections.singletonList(fieldDescription.getType().asErasure()));
        }

        /**
         * {@inheritDoc}
         */
        public Object asConstantPoolValue() {
            return new Handle(getHandleType().getIdentifier(),
                    getOwnerType().getInternalName(),
                    getName(),
                    getDescriptor(),
                    getOwnerType().isInterface());
        }

        /**
         * {@inheritDoc}
         */
        public Object asConstantDescription() {
            return MethodHandleDesc.of(DirectMethodHandleDesc.Kind.valueOf(getHandleType().getIdentifier()),
                    ClassDesc.ofDescriptor(getOwnerType().getDescriptor()),
                    getName(),
                    getDescriptor());
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getTypeDescription() {
            return JavaType.METHOD_HANDLE.getTypeStub();
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
        public boolean equals(Object other) {
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

        /**
         * Returns the lookup type of the provided {@code java.lang.invoke.MethodHandles$Lookup} instance.
         *
         * @param callerClassLookup An instance of {@code java.lang.invoke.MethodHandles$Lookup}.
         * @return The instance's lookup type.
         */
        public static Class<?> lookupType(Object callerClassLookup) {
            return DISPATCHER.lookupType(callerClassLookup);
        }

        /**
         * A dispatcher for analyzing a {@code java.lang.invoke.MethodHandle} instance.
         */
        protected interface Dispatcher {

            /**
             * Reveals a method handle's information object.
             *
             * @param lookup       The lookup to be used for introspecting the instance.
             * @param methodHandle The method handle to be introspected.
             * @return The {@code java.lang.invoke.MethodHandleInfo} object that describes the instance.
             */
            Object reveal(Object lookup, Object methodHandle);

            /**
             * Returns a method handle info's method type.
             *
             * @param methodHandleInfo The method handle info to introspect.
             * @return The {@code java.lang.invoke.MethodType} instance representing the method handle's type.
             */
            Object getMethodType(Object methodHandleInfo);

            /**
             * Returns the reference kind of the supplied method handle info.
             *
             * @param methodHandleInfo The method handle to be introspected.
             * @return The method handle info's reference type.
             */
            int getReferenceKind(Object methodHandleInfo);

            /**
             * Returns the declaring class of the supplied method handle info.
             *
             * @param methodHandleInfo The method handle to be introspected.
             * @return The method handle info's declaring class.
             */
            Class<?> getDeclaringClass(Object methodHandleInfo);

            /**
             * Returns the method name of the supplied method handle info.
             *
             * @param methodHandleInfo The method handle to be introspected.
             * @return The method handle info's method name.
             */
            String getName(Object methodHandleInfo);

            /**
             * Returns the return type of the supplied method type.
             *
             * @param methodType The method type to be introspected.
             * @return The method type's return type.
             */
            Class<?> returnType(Object methodType);

            /**
             * Returns the parameter types of the supplied method type.
             *
             * @param methodType The method type to be introspected.
             * @return The method type's parameter types.
             */
            List<? extends Class<?>> parameterArray(Object methodType);

            /**
             * An initializable version of a dispatcher that is not yet made accessible.
             */
            interface Initializable {

                /**
                 * Initializes the dispatcher, if required.
                 *
                 * @return The initialized dispatcher.
                 */
                Dispatcher initialize();

                /**
                 * Returns a public {@code java.lang.invoke.MethodHandles.Lookup} instance.
                 *
                 * @return A public {@code java.lang.invoke.MethodHandles.Lookup} instance.
                 */
                Object publicLookup();

                /**
                 * Returns the lookup type of a given {@code java.lang.invoke.MethodHandles$Lookup} instance.
                 *
                 * @param lookup A {@code java.lang.invoke.MethodHandles$Lookup} instance.
                 * @return The provided instance's lookup type.
                 */
                Class<?> lookupType(Object lookup);
            }

            /**
             * A creation action for a dispatcher.
             */
            enum CreationAction implements PrivilegedAction<Initializable> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
                public Initializable run() {
                    try {
                        try {
                            return new Dispatcher.ForJava8CapableVm(Class.forName("java.lang.invoke.MethodHandles").getMethod("publicLookup"),
                                    Class.forName("java.lang.invoke.MethodHandleInfo").getMethod("getName"),
                                    Class.forName("java.lang.invoke.MethodHandleInfo").getMethod("getDeclaringClass"),
                                    Class.forName("java.lang.invoke.MethodHandleInfo").getMethod("getReferenceKind"),
                                    Class.forName("java.lang.invoke.MethodHandleInfo").getMethod("getMethodType"),
                                    JavaType.METHOD_TYPE.load().getMethod("returnType"),
                                    JavaType.METHOD_TYPE.load().getMethod("parameterArray"),
                                    JavaType.METHOD_HANDLES_LOOKUP.load().getMethod("lookupClass"),
                                    JavaType.METHOD_HANDLES_LOOKUP.load().getMethod("revealDirect", JavaType.METHOD_HANDLE.load()));
                        } catch (Exception ignored) {
                            return new Dispatcher.ForJava7CapableVm(Class.forName("java.lang.invoke.MethodHandles").getMethod("publicLookup"),
                                    Class.forName("java.lang.invoke.MethodHandleInfo").getMethod("getName"),
                                    Class.forName("java.lang.invoke.MethodHandleInfo").getMethod("getDeclaringClass"),
                                    Class.forName("java.lang.invoke.MethodHandleInfo").getMethod("getReferenceKind"),
                                    Class.forName("java.lang.invoke.MethodHandleInfo").getMethod("getMethodType"),
                                    JavaType.METHOD_TYPE.load().getMethod("returnType"),
                                    JavaType.METHOD_TYPE.load().getMethod("parameterArray"),
                                    JavaType.METHOD_HANDLES_LOOKUP.load().getMethod("lookupClass"),
                                    Class.forName("java.lang.invoke.MethodHandleInfo").getConstructor(JavaType.METHOD_HANDLE.load()));
                        }
                    } catch (Exception ignored) {
                        return Dispatcher.ForLegacyVm.INSTANCE;
                    }
                }
            }

            /**
             * An abstract base implementation of a dispatcher.
             */
            @HashCodeAndEqualsPlugin.Enhance
            abstract class AbstractBase implements Dispatcher, Initializable {

                /**
                 * An empty array that can be used to indicate no arguments to avoid an allocation on a reflective call.
                 */
                private static final Object[] NO_ARGUMENTS = new Object[0];

                /**
                 * A reference to {@code java.lang.invoke.MethodHandles#publicLookup}.
                 */
                protected final Method publicLookup;

                /**
                 * A reference to {@code java.lang.invoke.MethodHandleInfo#getName}.
                 */
                protected final Method getName;

                /**
                 * A reference to {@code java.lang.invoke.MethodHandleInfo#getDeclaringClass}.
                 */
                protected final Method getDeclaringClass;

                /**
                 * A reference to {@code java.lang.invoke.MethodHandleInfo#getReferenceKind}.
                 */
                protected final Method getReferenceKind;

                /**
                 * A reference to {@code java.lang.invoke.MethodHandleInfo#getMethodType}.
                 */
                protected final Method getMethodType;

                /**
                 * A reference to {@code java.lang.invoke.MethodType#returnType}.
                 */
                protected final Method returnType;

                /**
                 * A reference to {@code java.lang.invoke.MethodType#parameterArray}.
                 */
                protected final Method parameterArray;

                /**
                 * A reference to {@code java.lang.invoke.MethodHandles$Lookup#lookupClass} method.
                 */
                protected final Method lookupClass;

                /**
                 * Creates a legal dispatcher.
                 *
                 * @param publicLookup      A reference to {@code java.lang.invoke.MethodHandles#publicLookup}.
                 * @param getName           A reference to {@code java.lang.invoke.MethodHandleInfo#getName}.
                 * @param getDeclaringClass A reference to {@code java.lang.invoke.MethodHandleInfo#getDeclaringClass}.
                 * @param getReferenceKind  A reference to {@code java.lang.invoke.MethodHandleInfo#getReferenceKind}.
                 * @param getMethodType     A reference to {@code java.lang.invoke.MethodHandleInfo#getMethodType}.
                 * @param returnType        A reference to {@code java.lang.invoke.MethodType#returnType}.
                 * @param parameterArray    A reference to {@code java.lang.invoke.MethodType#parameterArray}.
                 * @param lookupClass       A reference to {@code java.lang.invoke.MethodHandles$Lookup#lookupClass} method.
                 */
                protected AbstractBase(Method publicLookup,
                                       Method getName,
                                       Method getDeclaringClass,
                                       Method getReferenceKind,
                                       Method getMethodType,
                                       Method returnType,
                                       Method parameterArray,
                                       Method lookupClass) {
                    this.publicLookup = publicLookup;
                    this.getName = getName;
                    this.getDeclaringClass = getDeclaringClass;
                    this.getReferenceKind = getReferenceKind;
                    this.getMethodType = getMethodType;
                    this.returnType = returnType;
                    this.parameterArray = parameterArray;
                    this.lookupClass = lookupClass;
                }

                /**
                 * {@inheritDoc}
                 */
                public Object publicLookup() {
                    try {
                        return publicLookup.invoke(null, NO_ARGUMENTS);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodHandles#publicLookup", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodHandles#publicLookup", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Object getMethodType(Object methodHandleInfo) {
                    try {
                        return getMethodType.invoke(methodHandleInfo, NO_ARGUMENTS);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodHandleInfo#getMethodType", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodHandleInfo#getMethodType", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public int getReferenceKind(Object methodHandleInfo) {
                    try {
                        return (Integer) getReferenceKind.invoke(methodHandleInfo, NO_ARGUMENTS);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodHandleInfo#getReferenceKind", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodHandleInfo#getReferenceKind", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> getDeclaringClass(Object methodHandleInfo) {
                    try {
                        return (Class<?>) getDeclaringClass.invoke(methodHandleInfo, NO_ARGUMENTS);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodHandleInfo#getDeclaringClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodHandleInfo#getDeclaringClass", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public String getName(Object methodHandleInfo) {
                    try {
                        return (String) getName.invoke(methodHandleInfo, NO_ARGUMENTS);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodHandleInfo#getName", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodHandleInfo#getName", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> returnType(Object methodType) {
                    try {
                        return (Class<?>) returnType.invoke(methodType, NO_ARGUMENTS);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodType#returnType", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflect.MethodType#returnType", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public List<? extends Class<?>> parameterArray(Object methodType) {
                    try {
                        return Arrays.asList((Class<?>[]) parameterArray.invoke(methodType, NO_ARGUMENTS));
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflect.MethodType#parameterArray", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflect.MethodType#parameterArray", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> lookupType(Object lookup) {
                    try {
                        return (Class<?>) lookupClass.invoke(lookup, NO_ARGUMENTS);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflect.MethodHandles.Lookup#lookupClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflect.MethodHandles.Lookup#lookupClass", exception.getCause());
                    }
                }
            }

            /**
             * A dispatcher for introspecting a {@code java.lang.invoke.MethodHandle} instance on a virtual machine that officially supports this
             * introspection, i.e. Java versions 8+.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForJava8CapableVm extends AbstractBase {

                /**
                 * A reference to the {@code java.lang.invoke.MethodHandles.Lookup#revealDirect} method.
                 */
                private final Method revealDirect;

                /**
                 * Creates a dispatcher for a modern VM.
                 *
                 * @param publicLookup      A reference to {@code java.lang.invoke.MethodHandles#publicLookup}.
                 * @param getName           A reference to {@code java.lang.invoke.MethodHandleInfo#getName}.
                 * @param getDeclaringClass A reference to {@code java.lang.invoke.MethodHandleInfo#getDeclaringClass}.
                 * @param getReferenceKind  A reference to {@code java.lang.invoke.MethodHandleInfo#getReferenceKind}.
                 * @param getMethodType     A reference to {@code java.lang.invoke.MethodHandleInfo#getMethodType}.
                 * @param returnType        A reference to {@code java.lang.invoke.MethodType#returnType}.
                 * @param parameterArray    A reference to {@code java.lang.invoke.MethodType#parameterArray}.
                 * @param lookupClass       A reference to {@code java.lang.invoke.MethodHandles$Lookup#lookupClass} method.
                 * @param revealDirect      A reference to the {@code java.lang.invoke.MethodHandles.Lookup#revealDirect} method.
                 */
                protected ForJava8CapableVm(Method publicLookup,
                                            Method getName,
                                            Method getDeclaringClass,
                                            Method getReferenceKind,
                                            Method getMethodType,
                                            Method returnType,
                                            Method parameterArray,
                                            Method lookupClass,
                                            Method revealDirect) {
                    super(publicLookup, getName, getDeclaringClass, getReferenceKind, getMethodType, returnType, parameterArray, lookupClass);
                    this.revealDirect = revealDirect;
                }

                /**
                 * {@inheritDoc}
                 */
                public Object reveal(Object lookup, Object methodHandle) {
                    try {
                        return revealDirect.invoke(lookup, methodHandle);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodHandles.Lookup#revealDirect", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodHandles.Lookup#revealDirect", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Dispatcher initialize() {
                    return this;
                }
            }

            /**
             * A dispatcher that extracts the information of a method handle by using private APIs that are available in Java 7+.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForJava7CapableVm extends AbstractBase implements PrivilegedAction<Dispatcher> {

                /**
                 * A reference to the {@code java.lang.invoke.MethodInfo} constructor.
                 */
                private final Constructor<?> methodInfo;

                /**
                 * Creates a dispatcher for an intermediate VM.
                 *
                 * @param publicLookup      A reference to {@code java.lang.invoke.MethodHandles#publicLookup}.
                 * @param getName           A reference to {@code java.lang.invoke.MethodHandleInfo#getName}.
                 * @param getDeclaringClass A reference to {@code java.lang.invoke.MethodHandleInfo#getDeclaringClass}.
                 * @param getReferenceKind  A reference to {@code java.lang.invoke.MethodHandleInfo#getReferenceKind}.
                 * @param getMethodType     A reference to {@code java.lang.invoke.MethodHandleInfo#getMethodType}.
                 * @param returnType        A reference to {@code java.lang.invoke.MethodType#returnType}.
                 * @param parameterArray    A reference to {@code java.lang.invoke.MethodType#parameterArray}.
                 * @param lookupClass       A reference to {@code java.lang.invoke.MethodHandles$Lookup#lookupClass} method.
                 * @param methodInfo        A reference to the {@code java.lang.invoke.MethodInfo} constructor.
                 */
                protected ForJava7CapableVm(Method publicLookup,
                                            Method getName,
                                            Method getDeclaringClass,
                                            Method getReferenceKind,
                                            Method getMethodType,
                                            Method returnType,
                                            Method parameterArray,
                                            Method lookupClass,
                                            Constructor<?> methodInfo) {
                    super(publicLookup, getName, getDeclaringClass, getReferenceKind, getMethodType, returnType, parameterArray, lookupClass);
                    this.methodInfo = methodInfo;
                }

                /**
                 * {@inheritDoc}
                 */
                public Dispatcher initialize() {
                    return AccessController.doPrivileged(this);
                }

                /**
                 * {@inheritDoc}
                 */
                public Dispatcher run() {
                    // This is safe even in a multi-threaded environment as all threads set the instances accessible before invoking any methods.
                    // By always setting accessibility, the security manager is always triggered if this operation was illegal.
                    methodInfo.setAccessible(true);
                    getName.setAccessible(true);
                    getDeclaringClass.setAccessible(true);
                    getReferenceKind.setAccessible(true);
                    getMethodType.setAccessible(true);
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public Object reveal(Object lookup, Object methodHandle) {
                    try {
                        return methodInfo.newInstance(methodHandle);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodInfo()", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodInfo()", exception.getCause());
                    } catch (InstantiationException exception) {
                        throw new IllegalStateException("Error constructing java.lang.invoke.MethodInfo", exception);
                    }
                }
            }

            /**
             * A dispatcher that does not support method handles at all.
             */
            enum ForLegacyVm implements Initializable {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Dispatcher initialize() {
                    throw new UnsupportedOperationException("Unsupported type on current JVM: java.lang.invoke.MethodHandle");
                }

                /**
                 * {@inheritDoc}
                 */
                public Object publicLookup() {
                    throw new UnsupportedOperationException("Unsupported type on current JVM: java.lang.invoke.MethodHandle");
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> lookupType(Object lookup) {
                    throw new UnsupportedOperationException("Unsupported type on current JVM: java.lang.invoke.MethodHandle");
                }
            }
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
    }

    /**
     * Represents a dynamically resolved constant pool entry of a class file. This feature is supported for class files in version 11 and newer.
     */
    class Dynamic implements JavaConstant {

        /**
         * The {@code java.lang.invoke.ConstantBootstraps} class's internal name..
         */
        private static final String CONSTANT_BOOTSTRAPS = "java/lang/invoke/ConstantBootstraps";

        /**
         * The represented bootstrap value.
         */
        private final ConstantDynamic value;

        /**
         * The represented value constant.
         */
        private final TypeDescription typeDescription;

        /**
         * Creates a new dynamic class pool entry.
         *
         * @param value           The represented bootstrap value.
         * @param typeDescription The represented value constant.
         */
        protected Dynamic(ConstantDynamic value, TypeDescription typeDescription) {
            this.value = value;
            this.typeDescription = typeDescription;
        }

        /**
         * Returns a constant {@code null} value of type {@link Object}.
         *
         * @return A dynamically resolved null constant.
         */
        public static Dynamic ofNullConstant() {
            return new Dynamic(new ConstantDynamic("nullConstant",
                    TypeDescription.OBJECT.getDescriptor(),
                    new Handle(Opcodes.H_INVOKESTATIC,
                            CONSTANT_BOOTSTRAPS,
                            "nullConstant",
                            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;",
                            false)), TypeDescription.OBJECT);
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
            return new Dynamic(new ConstantDynamic(typeDescription.getDescriptor(),
                    TypeDescription.CLASS.getDescriptor(),
                    new Handle(Opcodes.H_INVOKESTATIC,
                            CONSTANT_BOOTSTRAPS,
                            "primitiveClass",
                            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Class;",
                            false)), TypeDescription.CLASS);
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
            return new Dynamic(new ConstantDynamic(enumerationDescription.getValue(),
                    enumerationDescription.getEnumerationType().getDescriptor(),
                    new Handle(Opcodes.H_INVOKESTATIC,
                            CONSTANT_BOOTSTRAPS,
                            "enumConstant",
                            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Enum;",
                            false)), enumerationDescription.getEnumerationType());
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
            return new Dynamic(new ConstantDynamic(fieldDescription.getInternalName(),
                    fieldDescription.getDescriptor(),
                    new Handle(Opcodes.H_INVOKESTATIC,
                            CONSTANT_BOOTSTRAPS,
                            "getStaticFinal",
                            selfDeclared
                                    ? "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;"
                                    : "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/Object;",
                            false), selfDeclared
                    ? new Object[0]
                    : new Object[]{Type.getType(fieldDescription.getDeclaringType().getDescriptor())}), fieldDescription.getType().asErasure());
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
            Object[] argument = new Object[constants.size() + 1];
            argument[0] = new Handle(methodDescription.isConstructor() ? Opcodes.H_NEWINVOKESPECIAL : Opcodes.H_INVOKESTATIC,
                    methodDescription.getDeclaringType().getInternalName(),
                    methodDescription.getInternalName(),
                    methodDescription.getDescriptor(),
                    methodDescription.getDeclaringType().isInterface());
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
            int index = 0;
            for (Object constant : constants) {
                JavaConstant wrapped = Simple.wrap(constant);
                if (!wrapped.getTypeDescription().isAssignableTo(iterator.next())) {
                    throw new IllegalArgumentException("Cannot assign " + constants + " to " + methodDescription);
                }
                argument[++index] = wrapped.asConstantPoolValue();
            }
            return new Dynamic(new ConstantDynamic("invoke",
                    (methodDescription.isConstructor()
                            ? methodDescription.getDeclaringType()
                            : methodDescription.getReturnType().asErasure()).getDescriptor(),
                    new Handle(Opcodes.H_INVOKESTATIC,
                            CONSTANT_BOOTSTRAPS,
                            "invoke",
                            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/invoke/MethodHandle;[Ljava/lang/Object;)Ljava/lang/Object;",
                            false),
                    argument), methodDescription.isConstructor() ? methodDescription.getDeclaringType() : methodDescription.getReturnType().asErasure());
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
            return new Dynamic(new ConstantDynamic(fieldDescription.getInternalName(),
                    JavaType.VAR_HANDLE.getTypeStub().getDescriptor(),
                    new Handle(Opcodes.H_INVOKESTATIC,
                            CONSTANT_BOOTSTRAPS,
                            fieldDescription.isStatic()
                                    ? "staticFieldVarHandle"
                                    : "fieldVarHandle",
                            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle;",
                            false),
                    Type.getType(fieldDescription.getDeclaringType().getDescriptor()),
                    Type.getType(fieldDescription.getType().asErasure().getDescriptor())), JavaType.VAR_HANDLE.getTypeStub());
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
            return new Dynamic(new ConstantDynamic("arrayVarHandle",
                    JavaType.VAR_HANDLE.getTypeStub().getDescriptor(),
                    new Handle(Opcodes.H_INVOKESTATIC,
                            CONSTANT_BOOTSTRAPS,
                            "arrayVarHandle",
                            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle;",
                            false),
                    Type.getType(typeDescription.getDescriptor())), JavaType.VAR_HANDLE.getTypeStub());
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
         * @param constants The constant values passed to the bootstrap method. Values can be represented either
         *                  as {@link TypeDescription}, as {@link JavaConstant}, as {@link String} or a primitive
         *                  {@code int}, {@code long}, {@code float} or {@code double} represented as wrapper type.
         * @return A dynamic constant that represents the bootstrapped method's or constructor's result.
         */
        public static Dynamic bootstrap(String name, MethodDescription.InDefinedShape bootstrap, List<?> constants) {
            if (name.length() == 0 || name.contains(".")) {
                throw new IllegalArgumentException("Not a valid field name: " + name);
            }
            Object[] argument = new Object[constants.size()];
            List<TypeDescription> types = new ArrayList<TypeDescription>(constants.size());
            int index = 0;
            for (Object constant : constants) {
                JavaConstant wrapped = JavaConstant.Simple.wrap(constant);
                argument[index++] = wrapped.asConstantPoolValue();
                types.add(wrapped.getTypeDescription());
            }
            if (!bootstrap.isConstantBootstrap(types)) {
                throw new IllegalArgumentException("Not a valid bootstrap method " + bootstrap + " for " + Arrays.asList(argument));
            }
            return new Dynamic(new ConstantDynamic(name,
                    (bootstrap.isConstructor()
                            ? bootstrap.getDeclaringType()
                            : bootstrap.getReturnType().asErasure()).getDescriptor(),
                    new Handle(bootstrap.isConstructor() ? Opcodes.H_NEWINVOKESPECIAL : Opcodes.H_INVOKESTATIC,
                            bootstrap.getDeclaringType().getInternalName(),
                            bootstrap.getInternalName(),
                            bootstrap.getDescriptor(),
                            false),
                    argument),
                    bootstrap.isConstructor()
                            ? bootstrap.getDeclaringType()
                            : bootstrap.getReturnType().asErasure());
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
            } else if (value.getBootstrapMethod().getName().equals(MethodDescription.CONSTRUCTOR_INTERNAL_NAME)
                    ? !this.typeDescription.isAssignableTo(typeDescription)
                    : (!typeDescription.asBoxed().isInHierarchyWith(this.typeDescription.asBoxed()))) {
                throw new IllegalArgumentException(typeDescription + " is not compatible with bootstrapped type " + this.typeDescription);
            }
            Object[] bootstrapMethodArgument = new Object[value.getBootstrapMethodArgumentCount()];
            for (int index = 0; index < value.getBootstrapMethodArgumentCount(); index++) {
                bootstrapMethodArgument[index] = value.getBootstrapMethodArgument(index);
            }
            return new Dynamic(new ConstantDynamic(value.getName(),
                    typeDescription.getDescriptor(),
                    value.getBootstrapMethod(),
                    bootstrapMethodArgument), typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public Object asConstantPoolValue() {
            return value;
        }

        /**
         * {@inheritDoc}
         */
        public Object asConstantDescription() {
            return asConstantDescription(value);
        }

        /**
         * Resolves a {@link ConstantDynamic} value to a {@code java.lang.constant.ConstantDesc}.
         *
         * @param value The {@link ConstantDynamic} value to resolve.
         * @return A {@code java.lang.constant.ConstantDesc} representing the supplied {@link ConstantDynamic}.
         */
        private static ConstantDesc asConstantDescription(ConstantDynamic value) {
            ConstantDesc[] arguments = new ConstantDesc[value.getBootstrapMethodArgumentCount()];
            for (int index = 0; index < value.getBootstrapMethodArgumentCount(); index++) {
                if (value.getBootstrapMethodArgument(index) instanceof ConstantDynamic) {
                    arguments[index] = asConstantDescription((ConstantDynamic) value.getBootstrapMethodArgument(index));
                } else if (value.getBootstrapMethodArgument(index) instanceof Handle) {
                    arguments[index] = MethodHandleDesc.of(DirectMethodHandleDesc.Kind.valueOf(((Handle) value.getBootstrapMethodArgument(index)).getTag()),
                            ClassDesc.ofDescriptor(((Handle) value.getBootstrapMethodArgument(index)).getOwner()),
                            ((Handle) value.getBootstrapMethodArgument(index)).getName(),
                            ((Handle) value.getBootstrapMethodArgument(index)).getDesc());
                } else if (value.getBootstrapMethodArgument(index) instanceof Type) {
                    arguments[index] = ((Type) value.getBootstrapMethodArgument(index)).getSort() == Type.METHOD
                            ? MethodTypeDesc.ofDescriptor(((Type) value.getBootstrapMethodArgument(index)).getDescriptor())
                            : ClassDesc.ofDescriptor(((Type) value.getBootstrapMethodArgument(index)).getDescriptor());
                } else if (value.getBootstrapMethodArgument(index) instanceof Integer
                        || value.getBootstrapMethodArgument(index) instanceof Long
                        || value.getBootstrapMethodArgument(index) instanceof Float
                        || value.getBootstrapMethodArgument(index) instanceof Double
                        || value.getBootstrapMethodArgument(index) instanceof String) {
                    arguments[index] = (ConstantDesc) value.getBootstrapMethodArgument(index);
                } else {
                    throw new IllegalStateException("Could not resolve bootstrap argument to a constant description: " + value.getBootstrapMethodArgument(index));
                }
            }
            return DynamicConstantDesc.ofCanonical(MethodHandleDesc.of(DirectMethodHandleDesc.Kind.valueOf(value.getBootstrapMethod().getTag()),
                    ClassDesc.ofDescriptor(value.getBootstrapMethod().getOwner()),
                    value.getBootstrapMethod().getName(),
                    value.getBootstrapMethod().getDesc()), value.getName(), ClassDesc.ofDescriptor(value.getDescriptor()), arguments);
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getTypeDescription() {
            return typeDescription;
        }

        @Override
        public int hashCode() {
            int result = value.hashCode();
            result = 31 * result + typeDescription.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (other == null || getClass() != other.getClass()) {
                return false;
            }
            Dynamic dynamic = (Dynamic) other;
            return value.equals(dynamic.value) && typeDescription.equals(dynamic.typeDescription);
        }
    }
}
