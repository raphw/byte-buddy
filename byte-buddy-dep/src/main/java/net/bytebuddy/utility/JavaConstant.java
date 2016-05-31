package net.bytebuddy.utility;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.constant.JavaConstantValue;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
     * Returns the instance as loadable onto the operand stack.
     *
     * @return A stack manipulation that loads the represented value onto the operand stack.
     */
    StackManipulation asStackManipulation();

    /**
     * Returns a description of the type of the represented instance or at least a stub.
     *
     * @return A description of the type of the represented instance or at least a stub.
     */
    TypeDescription getInstanceType();

    /**
     * Represents a {@code java.lang.invoke.MethodType} object.
     */
    class MethodType implements JavaConstant {

        /**
         * A dispatcher for extracting information from a {@code java.lang.invoke.MethodType} instance.
         */
        private static final Dispatcher DISPATCHER;

        /*
         * Locates a dispatcher depending on the feature set of the currently running JVM.
         */
        static {
            Dispatcher dispatcher;
            try {
                Class<?> methodType = JavaType.METHOD_TYPE.load();
                dispatcher = new Dispatcher.ForJava7CapableVm(methodType.getDeclaredMethod("returnType"), methodType.getDeclaredMethod("parameterArray"));
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception ignored) {
                dispatcher = Dispatcher.ForLegacyVm.INSTANCE;
            }
            DISPATCHER = dispatcher;
        }

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
            if (!JavaType.METHOD_TYPE.getTypeStub().isInstance(methodType)) {
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
            return of(new TypeDescription.ForLoadedType(returnType), new TypeList.ForLoadedTypes(parameterType));
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
            return ofConstant(new TypeDescription.ForLoadedType(type));
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

        @Override
        public Object asConstantPoolValue() {
            StringBuilder stringBuilder = new StringBuilder("(");
            for (TypeDescription parameterType : getParameterTypes()) {
                stringBuilder.append(parameterType.getDescriptor());
            }
            return Type.getMethodType(stringBuilder.append(")").append(getReturnType().getDescriptor()).toString());
        }

        @Override
        public StackManipulation asStackManipulation() {
            return new JavaConstantValue(this);
        }

        @Override
        public TypeDescription getInstanceType() {
            return JavaType.METHOD_TYPE.getTypeStub();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            MethodType that = (MethodType) other;
            return parameterTypes.equals(that.parameterTypes) && returnType.equals(that.returnType);

        }

        @Override
        public int hashCode() {
            int result = returnType.hashCode();
            result = 31 * result + parameterTypes.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "JavaConstant.MethodType{" +
                    "returnType=" + returnType +
                    ", parameterTypes=" + parameterTypes +
                    '}';
        }

        /**
         * A dispatcher for extracting information from a {@code java.lang.invoke.MethodType} instance.
         */
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

            /**
             * A dispatcher for virtual machines that are aware of the {@code java.lang.invoke.MethodType} type that was added in Java version 7.
             */
            class ForJava7CapableVm implements Dispatcher {

                /**
                 * A reference to {@code java.lang.invoke.MethodType#returnType}.
                 */
                private final Method returnType;

                /**
                 * A reference to {@code java.lang.invoke.MethodType#returnType}.
                 */
                private final Method parameterArray;

                /**
                 * Creates a new dispatcher for a modern JVM.
                 *
                 * @param returnType     A reference to {@code java.lang.invoke.MethodType#returnType}.
                 * @param parameterArray A reference to {@code java.lang.invoke.MethodType#returnType}.
                 */
                protected ForJava7CapableVm(Method returnType, Method parameterArray) {
                    this.returnType = returnType;
                    this.parameterArray = parameterArray;
                }

                @Override
                public Class<?> returnType(Object methodType) {
                    try {
                        return (Class<?>) returnType.invoke(methodType);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodType#returnType", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodType#returnType", exception.getCause());
                    }
                }

                @Override
                public Class<?>[] parameterArray(Object methodType) {
                    try {
                        return (Class<?>[]) parameterArray.invoke(methodType);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodType#parameterArray", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodType#parameterArray", exception.getCause());
                    }
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    ForJava7CapableVm that = (ForJava7CapableVm) other;
                    return returnType.equals(that.returnType) && parameterArray.equals(that.parameterArray);
                }

                @Override
                public int hashCode() {
                    int result = returnType.hashCode();
                    result = 31 * result + parameterArray.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "JavaConstant.MethodType.Dispatcher.ForJava7CapableVm{" +
                            "returnType=" + returnType +
                            ", parameterArray=" + parameterArray +
                            '}';
                }
            }

            /**
             * A dispatcher for virtual machines that are <b>not</b> aware of the {@code java.lang.invoke.MethodType} type that was added in Java version 7.
             */
            enum ForLegacyVm implements Dispatcher {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public Class<?> returnType(Object methodType) {
                    throw new IllegalStateException("Unsupported type for the current JVM: java.lang.invoke.MethodType");
                }

                @Override
                public Class<?>[] parameterArray(Object methodType) {
                    throw new IllegalStateException("Unsupported type for the current JVM: java.lang.invoke.MethodType");
                }

                @Override
                public String toString() {
                    return "JavaConstant.MethodType.Dispatcher.ForLegacyVm." + name();
                }
            }
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
        private static final Dispatcher.Initializable DISPATCHER;

        /*
         * Locates a dispatcher depending on the feature set of the currently running JVM.
         */
        static {
            Dispatcher.Initializable dispatcher;
            try {
                try {
                    dispatcher = new Dispatcher.ForJava8CapableVm(Class.forName("java.lang.invoke.MethodHandles").getDeclaredMethod("publicLookup"),
                            Class.forName("java.lang.invoke.MethodHandleInfo").getDeclaredMethod("getName"),
                            Class.forName("java.lang.invoke.MethodHandleInfo").getDeclaredMethod("getDeclaringClass"),
                            Class.forName("java.lang.invoke.MethodHandleInfo").getDeclaredMethod("getReferenceKind"),
                            Class.forName("java.lang.invoke.MethodHandleInfo").getDeclaredMethod("getMethodType"),
                            JavaType.METHOD_TYPE.load().getDeclaredMethod("returnType"),
                            JavaType.METHOD_TYPE.load().getDeclaredMethod("parameterArray"),
                            JavaType.METHOD_HANDLES_LOOKUP.load().getDeclaredMethod("lookupClass"),
                            JavaType.METHOD_HANDLES_LOOKUP.load().getDeclaredMethod("revealDirect", JavaType.METHOD_HANDLE.load()));
                } catch (RuntimeException exception) {
                    throw exception;
                } catch (Exception ignored) {
                    dispatcher = new Dispatcher.ForJava7CapableVm(Class.forName("java.lang.invoke.MethodHandles").getDeclaredMethod("publicLookup"),
                            Class.forName("java.lang.invoke.MethodHandleInfo").getDeclaredMethod("getName"),
                            Class.forName("java.lang.invoke.MethodHandleInfo").getDeclaredMethod("getDeclaringClass"),
                            Class.forName("java.lang.invoke.MethodHandleInfo").getDeclaredMethod("getReferenceKind"),
                            Class.forName("java.lang.invoke.MethodHandleInfo").getDeclaredMethod("getMethodType"),
                            JavaType.METHOD_TYPE.load().getDeclaredMethod("returnType"),
                            JavaType.METHOD_TYPE.load().getDeclaredMethod("parameterArray"),
                            JavaType.METHOD_HANDLES_LOOKUP.load().getDeclaredMethod("lookupClass"),
                            Class.forName("java.lang.invoke.MethodHandleInfo").getDeclaredConstructor(JavaType.METHOD_HANDLE.load()));
                }
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception ignored) {
                dispatcher = Dispatcher.ForLegacyVm.INSTANCE;
            }
            DISPATCHER = dispatcher;
        }

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
            return ofLoaded(methodHandle, DISPATCHER.publicLookup(), AccessController.getContext());
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
            return ofLoaded(methodHandle, lookup, AccessController.getContext());
        }

        /**
         * Creates a method handles representation of a loaded method handle which is analyzed using a public {@code MethodHandles.Lookup} object.
         * A method handle can only be analyzed on virtual machines that support the corresponding API (Java 7+). For virtual machines before Java 8+,
         * a method handle instance can only be analyzed by taking advantage of private APIs what might require a access context.
         *
         * @param methodHandle         The loaded method handle to represent.
         * @param accessControlContext The access control context to be used for making private methods accessible when using Java 7.
         * @return A representation of the loaded method handle
         */
        public static MethodHandle ofLoaded(Object methodHandle, AccessControlContext accessControlContext) {
            return ofLoaded(methodHandle, DISPATCHER.publicLookup(), accessControlContext);
        }

        /**
         * Creates a method handles representation of a loaded method handle which is analyzed using the given lookup context.
         * A method handle can only be analyzed on virtual machines that support the corresponding API (Java 7+). For virtual machines before Java 8+,
         * a method handle instance can only be analyzed by taking advantage of private APIs what might require a access context.
         *
         * @param methodHandle         The loaded method handle to represent.
         * @param lookup               The lookup object to use for analyzing the method handle.
         * @param accessControlContext The access control context to be used for making private methods accessible when using Java 7.
         * @return A representation of the loaded method handle
         */
        public static MethodHandle ofLoaded(Object methodHandle, Object lookup, AccessControlContext accessControlContext) {
            if (!JavaType.METHOD_HANDLE.getTypeStub().isInstance(methodHandle)) {
                throw new IllegalArgumentException("Expected method handle object: " + methodHandle);
            } else if (!JavaType.METHOD_HANDLES_LOOKUP.getTypeStub().isInstance(lookup)) {
                throw new IllegalArgumentException("Expected method handle lookup object: " + lookup);
            }
            Dispatcher dispatcher = DISPATCHER.initialize(accessControlContext);
            Object methodHandleInfo = dispatcher.reveal(lookup, methodHandle);
            Object methodType = dispatcher.getMethodType(methodHandleInfo);
            return new MethodHandle(HandleType.of(dispatcher.getReferenceKind(methodHandleInfo)),
                    new TypeDescription.ForLoadedType(dispatcher.getDeclaringClass(methodHandleInfo)),
                    dispatcher.getName(methodHandleInfo),
                    new TypeDescription.ForLoadedType(dispatcher.returnType(methodType)),
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
            return ofSpecial(new MethodDescription.ForLoadedMethod(method), new TypeDescription.ForLoadedType(type));
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

        @Override
        public Object asConstantPoolValue() {
            StringBuilder stringBuilder = new StringBuilder("(");
            for (TypeDescription parameterType : getParameterTypes()) {
                stringBuilder.append(parameterType.getDescriptor());
            }
            String descriptor = stringBuilder.append(")").append(getReturnType().getDescriptor()).toString();
            return new Handle(getHandleType().getIdentifier(), getOwnerType().getInternalName(), getName(), descriptor, getOwnerType().isInterface());
        }

        @Override
        public StackManipulation asStackManipulation() {
            return new JavaConstantValue(this);
        }

        @Override
        public TypeDescription getInstanceType() {
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
            StringBuilder stringBuilder = new StringBuilder("(");
            for (TypeDescription parameterType : parameterTypes) {
                stringBuilder.append(parameterType.getDescriptor());
            }
            return stringBuilder.append(')').append(returnType.getDescriptor()).toString();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            MethodHandle aDefault = (MethodHandle) other;
            return handleType == aDefault.handleType
                    && name.equals(aDefault.name)
                    && ownerType.equals(aDefault.ownerType)
                    && parameterTypes.equals(aDefault.parameterTypes)
                    && returnType.equals(aDefault.returnType);
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
        public String toString() {
            return "JavaConstant.MethodHandle{" +
                    "handleType=" + handleType +
                    ", ownerType=" + ownerType +
                    ", name='" + name + '\'' +
                    ", returnType=" + returnType +
                    ", parameterTypes=" + parameterTypes +
                    '}';
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
             * @return The {@link java.lang.invoke.MethodType} instance representing the method handle's type.
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
                 * @param accessControlContext The access control context to be used for introspecting private APIs, if required.
                 * @return The initialized dispatcher.
                 */
                Dispatcher initialize(AccessControlContext accessControlContext);

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
             * An abstract base impleementation of a dispatcher.
             */
            abstract class AbstractBase implements Dispatcher, Initializable {

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

                @Override
                public Object publicLookup() {
                    try {
                        return publicLookup.invoke(null);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodHandles#publicLookup", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodHandles#publicLookup", exception.getCause());
                    }
                }

                @Override
                public Object getMethodType(Object methodHandleInfo) {
                    try {
                        return getMethodType.invoke(methodHandleInfo);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodHandleInfo#getMethodType", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodHandleInfo#getMethodType", exception.getCause());
                    }
                }

                @Override
                public int getReferenceKind(Object methodHandleInfo) {
                    try {
                        return (Integer) getReferenceKind.invoke(methodHandleInfo);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodHandleInfo#getReferenceKind", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodHandleInfo#getReferenceKind", exception.getCause());
                    }
                }

                @Override
                public Class<?> getDeclaringClass(Object methodHandleInfo) {
                    try {
                        return (Class<?>) getDeclaringClass.invoke(methodHandleInfo);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodHandleInfo#getDeclaringClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodHandleInfo#getDeclaringClass", exception.getCause());
                    }
                }

                @Override
                public String getName(Object methodHandleInfo) {
                    try {
                        return (String) getName.invoke(methodHandleInfo);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodHandleInfo#getName", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodHandleInfo#getName", exception.getCause());
                    }
                }

                @Override
                public Class<?> returnType(Object methodType) {
                    try {
                        return (Class<?>) returnType.invoke(methodType);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodType#returnType", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflect.MethodType#returnType", exception.getCause());
                    }
                }

                @Override
                public List<? extends Class<?>> parameterArray(Object methodType) {
                    try {
                        return Arrays.asList((Class<?>[]) parameterArray.invoke(methodType));
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflect.MethodType#parameterArray", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflect.MethodType#parameterArray", exception.getCause());
                    }
                }

                @Override
                public Class<?> lookupType(Object lookup) {
                    try {
                        return (Class<?>) lookupClass.invoke(lookup);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflect.MethodHandles.Lookup#lookupClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflect.MethodHandles.Lookup#lookupClass", exception.getCause());
                    }
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    AbstractBase that = (AbstractBase) other;
                    return publicLookup.equals(that.publicLookup)
                            && getName.equals(that.getName)
                            && getDeclaringClass.equals(that.getDeclaringClass)
                            && getReferenceKind.equals(that.getReferenceKind)
                            && getMethodType.equals(that.getMethodType)
                            && returnType.equals(that.returnType)
                            && parameterArray.equals(that.parameterArray)
                            && lookupClass.equals(that.lookupClass);
                }

                @Override
                public int hashCode() {
                    int result = publicLookup.hashCode();
                    result = 31 * result + getName.hashCode();
                    result = 31 * result + getDeclaringClass.hashCode();
                    result = 31 * result + getReferenceKind.hashCode();
                    result = 31 * result + getMethodType.hashCode();
                    result = 31 * result + returnType.hashCode();
                    result = 31 * result + parameterArray.hashCode();
                    result = 31 * result + lookupClass.hashCode();
                    return result;
                }
            }

            /**
             * A dispatcher for introspecting a {@code java.lang.invoke.MethodHandle} instance on a virtual machine that officially supports this
             * introspection, i.e. Java versions 8+.
             */
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

                @Override
                public Object reveal(Object lookup, Object methodHandle) {
                    try {
                        return revealDirect.invoke(lookup, methodHandle);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodHandles.Lookup#revealDirect", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodHandles.Lookup#revealDirect", exception.getCause());
                    }
                }

                @Override
                public Dispatcher initialize(AccessControlContext accessControlContext) {
                    return this;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    if (!super.equals(other)) return false;
                    ForJava8CapableVm that = (ForJava8CapableVm) other;
                    return revealDirect.equals(that.revealDirect);
                }

                @Override
                public int hashCode() {
                    int result = super.hashCode();
                    result = 31 * result + revealDirect.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "JavaConstant.MethodHandle.Dispatcher.ForJava8CapableVm{" +
                            "publicLookup=" + publicLookup +
                            ", getName=" + getName +
                            ", getDeclaringClass=" + getDeclaringClass +
                            ", getReferenceKind=" + getReferenceKind +
                            ", getMethodType=" + getMethodType +
                            ", returnType=" + returnType +
                            ", parameterArray=" + parameterArray +
                            ", lookupClass=" + lookupClass +
                            ", revealDirect=" + revealDirect +
                            '}';
                }
            }

            /**
             * A dispatcher that extracts the information of a method handle by using private APIs that are available in Java 7+.
             */
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

                @Override
                public Dispatcher initialize(AccessControlContext accessControlContext) {
                    return AccessController.doPrivileged(this, accessControlContext);
                }

                @Override
                public Dispatcher run() {
                    // This is safe even in a multi-threaded environment as all threads set the instances accessible before invoking any methods.
                    // By always setting accessability, the security manager is always triggered if this operation was illegal.
                    methodInfo.setAccessible(true);
                    getName.setAccessible(true);
                    getDeclaringClass.setAccessible(true);
                    getReferenceKind.setAccessible(true);
                    getMethodType.setAccessible(true);
                    return this;
                }

                @Override
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

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    if (!super.equals(other)) return false;
                    ForJava7CapableVm that = (ForJava7CapableVm) other;
                    return methodInfo.equals(that.methodInfo);
                }

                @Override
                public int hashCode() {
                    int result = super.hashCode();
                    result = 31 * result + methodInfo.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "JavaConstant.MethodHandle.Dispatcher.ForJava7CapableVm{" +
                            "publicLookup=" + publicLookup +
                            ", getName=" + getName +
                            ", getDeclaringClass=" + getDeclaringClass +
                            ", getReferenceKind=" + getReferenceKind +
                            ", getMethodType=" + getMethodType +
                            ", returnType=" + returnType +
                            ", parameterArray=" + parameterArray +
                            ", lookupClass=" + lookupClass +
                            ", methodInfo=" + methodInfo +
                            '}';
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

                @Override
                public Dispatcher initialize(AccessControlContext accessControlContext) {
                    throw new IllegalStateException("Unsupported type on current JVM: java.lang.invoke.MethodHandle");
                }

                @Override
                public Object publicLookup() {
                    throw new IllegalStateException("Unsupported type on current JVM: java.lang.invoke.MethodHandle");
                }

                @Override
                public Class<?> lookupType(Object lookup) {
                    throw new IllegalStateException("Unsupported type on current JVM: java.lang.invoke.MethodHandle");
                }

                @Override
                public String toString() {
                    return "JavaConstant.MethodHandle.Dispatcher.ForLegacyVm." + name();
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
            INVOKE_VIRTUAL(Opcodes.H_INVOKEVIRTUAL),

            /**
             * A handle representing an invokestatic invocation.
             */
            INVOKE_STATIC(Opcodes.H_INVOKESTATIC),

            /**
             * A handle representing an invokespecial invocation for a non-constructor.
             */
            INVOKE_SPECIAL(Opcodes.H_INVOKESPECIAL),

            /**
             * A handle representing an invokeinterface invocation.
             */
            INVOKE_INTERFACE(Opcodes.H_INVOKEINTERFACE),

            /**
             * A handle representing an invokespecial invocation for a constructor.
             */
            INVOKE_SPECIAL_CONSTRUCTOR(Opcodes.H_NEWINVOKESPECIAL),

            /**
             * A handle representing a write of a non-static field invocation.
             */
            PUT_FIELD(Opcodes.H_PUTFIELD),

            /**
             * A handle representing a read of a non-static field invocation.
             */
            GET_FIELD(Opcodes.H_GETFIELD),

            /**
             * A handle representing a write of a static field invocation.
             */
            PUT_STATIC_FIELD(Opcodes.H_PUTSTATIC),

            /**
             * A handle representing a read of a static field invocation.
             */
            GET_STATIC_FIELD(Opcodes.H_GETSTATIC);

            /**
             * The represented identifier.
             */
            private final int identifier;

            /**
             * Creates a new handle type.
             *
             * @param identifier The represented identifier.
             */
            HandleType(int identifier) {
                this.identifier = identifier;
            }

            /**
             * Extracts a handle type for invoking the given method.
             *
             * @param methodDescription The method for which a handle type should be found.
             * @return The handle type for the given method.
             */
            protected static HandleType of(MethodDescription.InDefinedShape methodDescription) {
                if (methodDescription.isStatic()) {
                    return INVOKE_STATIC;
                } else if (methodDescription.isPrivate()) {
                    return INVOKE_SPECIAL;
                } else if (methodDescription.isConstructor()) {
                    return INVOKE_SPECIAL_CONSTRUCTOR;
                } else if (methodDescription.getDeclaringType().asErasure().isInterface()) {
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

            @Override
            public String toString() {
                return "JavaConstant.MethodHandle.HandleType." + name();
            }
        }
    }
}
