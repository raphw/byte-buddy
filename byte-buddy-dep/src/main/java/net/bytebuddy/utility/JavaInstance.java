package net.bytebuddy.utility;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.constant.MethodHandleConstant;
import net.bytebuddy.implementation.bytecode.constant.MethodTypeConstant;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.utility.ByteBuddyCommons.isActualType;
import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

/**
 * Returns a Java instance of an object that has a special meaning to the Java virtual machine and that is not
 * available to Java in versions 6.
 */
public interface JavaInstance {

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
    class MethodType implements JavaInstance {

        /**
         * The Java method to receive the return type of a {@code MethodType} representation.
         */
        private static final JavaMethod RETURN_TYPE;

        /**
         * The Java method to receive the parameter types of a {@code MethodType} representation.
         */
        private static final JavaMethod PARAMETER_ARRAY;

        /*
         * Locates the Java methods for calling methods on {@code MethodType} instances, if those are available.
         */
        static {
            JavaMethod returnType, parameterArray;
            try {
                Class<?> methodType = JavaType.METHOD_TYPE.load();
                returnType = new JavaMethod.ForLoadedMethod(methodType.getDeclaredMethod("returnType"));
                parameterArray = new JavaMethod.ForLoadedMethod(methodType.getDeclaredMethod("parameterArray"));
            } catch (Exception ignored) {
                returnType = JavaMethod.ForUnavailableMethod.INSTANCE;
                parameterArray = JavaMethod.ForUnavailableMethod.INSTANCE;
            }
            RETURN_TYPE = returnType;
            PARAMETER_ARRAY = parameterArray;
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
         * @param methodType A method type object to represent as a {@link JavaInstance}.
         * @return The method type represented as a {@code JavaInstance}.
         */
        public static MethodType of(Object methodType) {
            if (!JavaType.METHOD_TYPE.getTypeStub().isInstance(methodType)) {
                throw new IllegalArgumentException("Excpected method type object: " + methodType);
            }
            return of((Class<?>) RETURN_TYPE.invoke(methodType), (Class<?>[]) PARAMETER_ARRAY.invoke(methodType));
        }

        /**
         * Returns a method type description of the given return type and parameter types.
         *
         * @param returnType    The return type to represent.
         * @param parameterType The parameter types to represent.
         * @return A method type of the given return type and parameter types.
         */
        public static MethodType of(Class<?> returnType, Class<?>... parameterType) {
            return of(new TypeDescription.ForLoadedType(nonNull(returnType)), new TypeList.ForLoadedType(nonNull(parameterType)));
        }

        /**
         * Returns a method type description of the given return type and parameter types.
         *
         * @param returnType     The return type to represent.
         * @param parameterTypes The parameter types to represent.
         * @return A method type of the given return type and parameter types.
         */
        public static MethodType of(TypeDescription returnType, List<? extends TypeDescription> parameterTypes) {
            return new MethodType(nonNull(returnType), isActualType(parameterTypes));
        }

        /**
         * Returns a method type description of the given method.
         *
         * @param method The method to extract the method type from.
         * @return The method type of the given method.
         */
        public static MethodType of(Method method) {
            return of(new MethodDescription.ForLoadedMethod(nonNull(method)));
        }

        /**
         * Returns a method type description of the given constructor.
         *
         * @param constructor The constructor to extract the method type from.
         * @return The method type of the given constructor.
         */
        public static MethodType of(Constructor<?> constructor) {
            return of(new MethodDescription.ForLoadedConstructor(nonNull(constructor)));
        }

        /**
         * Returns a method type description of the given method.
         *
         * @param methodDescription The method to extract the method type from.
         * @return The method type of the given method.
         */
        public static MethodType of(MethodDescription methodDescription) {
            return new MethodType(methodDescription.getReturnType().asRawType(), methodDescription.getParameters().asTypeList());
        }

        /**
         * Returns a method type for a setter of the given field.
         *
         * @param field The field to extract a setter type for.
         * @return The type of a setter for the given field.
         */
        public static MethodType ofSetter(Field field) {
            return ofSetter(new FieldDescription.ForLoadedField(nonNull(field)));
        }

        /**
         * Returns a method type for a setter of the given field.
         *
         * @param fieldDescription The field to extract a setter type for.
         * @return The type of a setter for the given field.
         */
        public static MethodType ofSetter(FieldDescription fieldDescription) {
            return new MethodType(TypeDescription.VOID, Collections.singletonList(fieldDescription.getType().asRawType()));
        }

        /**
         * Returns a method type for a getter of the given field.
         *
         * @param field The field to extract a getter type for.
         * @return The type of a getter for the given field.
         */
        public static MethodType ofGetter(Field field) {
            return ofGetter(new FieldDescription.ForLoadedField(nonNull(field)));
        }

        /**
         * Returns a method type for a getter of the given field.
         *
         * @param fieldDescription The field to extract a getter type for.
         * @return The type of a getter for the given field.
         */
        public static MethodType ofGetter(FieldDescription fieldDescription) {
            return new MethodType(fieldDescription.getType().asRawType(), Collections.<TypeDescription>emptyList());
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
            return ofConstant(new TypeDescription.ForLoadedType(nonNull(type)));
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
        public List<TypeDescription> getParameterTypes() {
            return new ArrayList<TypeDescription>(parameterTypes);
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
            return MethodTypeConstant.of(this);
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
            return "JavaInstance.MethodType{" +
                    "returnType=" + returnType +
                    ", parameterTypes=" + parameterTypes +
                    '}';
        }
    }

    /**
     * Represents a {@code java.lang.invoke.MethodHandle} object. Note that constant {@code MethodHandle}s cannot
     * be represented within the constant pool of a Java class and can therefore not be represented as an instance of
     * this representation order.
     */
    class MethodHandle implements JavaInstance {

        /**
         * The Java method to reveal the execution context of a {@code MethodHandle} as {@code MethodHandleInfo}.
         */
        private static final JavaMethod REVEAL_DIRECT;

        /**
         * The Java method to construct a {@code MethodHandleInfo} instance for Java 7 where revealing is not yet supported.
         */
        private static final JavaMethod NEW_METHOD_HANDLE_INFO;

        /**
         * The Java method to receive a {@code MethodHandle} instance.
         */
        private static final JavaMethod LOOKUP;

        /**
         * The Java method to receive the name represented by a {@code MethodHandleInfo}.
         */
        private static final JavaMethod GET_NAME;

        /**
         * The Java method to receive the declaring type represented by a {@code MethodHandleInfo}.
         */
        private static final JavaMethod GET_DECLARING_CLASS;

        /**
         * The Java method to receive the name represented by a {@code MethodHandleInfo}.
         */
        private static final JavaMethod GET_REFERENCE_KIND;

        /**
         * The Java method to receive the method type of a {@code MethodHandleInfo}.
         */
        private static final JavaMethod GET_METHOD_TYPE;

        /**
         * The Java method to receive the return type of a {@code MethodType} representation.
         */
        private static final JavaMethod RETURN_TYPE;

        /**
         * The Java method to receive the parameter types of a {@code MethodType} representation.
         */
        private static final JavaMethod PARAMETER_ARRAY;

        /*
         * Locates the Java methods for calling methods on {@code MethodHandle} instances, if those are available.
         */
        static {
            JavaMethod revealDirect, newMethodHandleInfo, lookup, getName, getDeclaringClass, getReferenceKind, getMethodType, returnType, parameterArray;
            try {
                Class<?> methodHandlesLookup = JavaType.METHOD_HANDLES_LOOKUP.load();
                try {
                    revealDirect = new JavaMethod.ForLoadedMethod(methodHandlesLookup.getDeclaredMethod("revealDirect", JavaType.METHOD_HANDLE.load()));
                } catch (Exception ignored) {
                    revealDirect = JavaMethod.ForUnavailableMethod.INSTANCE;
                }
                Class<?> methodHandles = Class.forName("java.lang.invoke.MethodHandles");
                lookup = new JavaMethod.ForLoadedMethod(methodHandles.getDeclaredMethod("publicLookup"));
                Class<?> methodHandleInfo = Class.forName("java.lang.invoke.MethodHandleInfo");
                if (revealDirect.isInvokable()) {
                    newMethodHandleInfo = JavaMethod.ForUnavailableMethod.INSTANCE;
                } else {
                    Constructor<?> newMethodHandleInfoConstructor = methodHandleInfo.getDeclaredConstructor(JavaType.METHOD_HANDLE.load());
                    newMethodHandleInfoConstructor.setAccessible(true);
                    newMethodHandleInfo = new JavaMethod.ForLoadedConstructor(newMethodHandleInfoConstructor);
                }
                Method getNameMethod = methodHandleInfo.getDeclaredMethod("getName");
                Method getDeclaringClassMethod = methodHandleInfo.getDeclaredMethod("getDeclaringClass");
                Method getReferenceKindMethod = methodHandleInfo.getDeclaredMethod("getReferenceKind");
                Method getMethodTypeMethod = methodHandleInfo.getDeclaredMethod("getMethodType");
                getName = new JavaMethod.ForLoadedMethod(getNameMethod);
                getDeclaringClass = new JavaMethod.ForLoadedMethod(getDeclaringClassMethod);
                getReferenceKind = new JavaMethod.ForLoadedMethod(getReferenceKindMethod);
                getMethodType = new JavaMethod.ForLoadedMethod(getMethodTypeMethod);
                if (!revealDirect.isInvokable()) {
                    getNameMethod.setAccessible(true);
                    getDeclaringClassMethod.setAccessible(true);
                    getReferenceKindMethod.setAccessible(true);
                    getMethodTypeMethod.setAccessible(true);
                }
                Class<?> methodType = JavaType.METHOD_TYPE.load();
                returnType = new JavaMethod.ForLoadedMethod(methodType.getDeclaredMethod("returnType"));
                parameterArray = new JavaMethod.ForLoadedMethod(methodType.getDeclaredMethod("parameterArray"));
            } catch (Exception ignored) {
                revealDirect = JavaMethod.ForUnavailableMethod.INSTANCE;
                newMethodHandleInfo = JavaMethod.ForUnavailableMethod.INSTANCE;
                lookup = JavaMethod.ForUnavailableMethod.INSTANCE;
                getName = JavaMethod.ForUnavailableMethod.INSTANCE;
                getDeclaringClass = JavaMethod.ForUnavailableMethod.INSTANCE;
                getReferenceKind = JavaMethod.ForUnavailableMethod.INSTANCE;
                getMethodType = JavaMethod.ForUnavailableMethod.INSTANCE;
                returnType = JavaMethod.ForUnavailableMethod.INSTANCE;
                parameterArray = JavaMethod.ForUnavailableMethod.INSTANCE;
            }
            REVEAL_DIRECT = revealDirect;
            NEW_METHOD_HANDLE_INFO = newMethodHandleInfo;
            LOOKUP = lookup;
            GET_NAME = getName;
            GET_DECLARING_CLASS = getDeclaringClass;
            GET_REFERENCE_KIND = getReferenceKind;
            GET_METHOD_TYPE = getMethodType;
            RETURN_TYPE = returnType;
            PARAMETER_ARRAY = parameterArray;
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
         * A method handle can only
         *
         * @param methodHandle The loaded method handle to represent.
         * @return A  representation of the loaded method handle
         */
        public static MethodHandle of(Object methodHandle) {
            return of(methodHandle, LOOKUP.invokeStatic());
        }

        /**
         * Creates a method handles representation of a loaded method handle .
         *
         * @param methodHandle The loaded method handle to represent.
         * @param lookup       The lookup object to use for analyzing the method handle.
         * @return A  representation of the loaded method handle
         */
        public static MethodHandle of(Object methodHandle, Object lookup) {
            if (!JavaType.METHOD_HANDLE.getTypeStub().isInstance(methodHandle)) {
                throw new IllegalArgumentException("Expected method handle object: " + methodHandle);
            } else if (!JavaType.METHOD_HANDLES_LOOKUP.getTypeStub().isInstance(lookup)) {
                throw new IllegalArgumentException("Expected method handle lookup object: " + lookup);
            }
            Object methodHandleInfo = REVEAL_DIRECT.isInvokable()
                    ? REVEAL_DIRECT.invoke(lookup, methodHandle)
                    : NEW_METHOD_HANDLE_INFO.invokeStatic(methodHandle);
            Object methodType = GET_METHOD_TYPE.invoke(methodHandleInfo);
            return new MethodHandle(HandleType.of((Integer) GET_REFERENCE_KIND.invoke(methodHandleInfo)),
                    new TypeDescription.ForLoadedType((Class<?>) GET_DECLARING_CLASS.invoke(methodHandleInfo)),
                    (String) GET_NAME.invoke(methodHandleInfo),
                    new TypeDescription.ForLoadedType((Class<?>) RETURN_TYPE.invoke(methodType)),
                    new TypeList.ForLoadedType((Class<?>[]) PARAMETER_ARRAY.invoke(methodType)));
        }

        /**
         * Creates a method handle representation of the given method.
         *
         * @param method The method ro represent.
         * @return A method handle representing the given method.
         */
        public static MethodHandle of(Method method) {
            return of(new MethodDescription.ForLoadedMethod(nonNull(method)));
        }

        /**
         * Creates a method handle representation of the given constructor.
         *
         * @param constructor The constructor ro represent.
         * @return A method handle representing the given constructor.
         */
        public static MethodHandle of(Constructor<?> constructor) {
            return of(new MethodDescription.ForLoadedConstructor(nonNull(constructor)));
        }

        /**
         * Creates a method handle representation of the given method.
         *
         * @param methodDescription The method ro represent.
         * @return A method handle representing the given method.
         */
        public static MethodHandle of(MethodDescription methodDescription) {
            return new MethodHandle(HandleType.of(methodDescription),
                    methodDescription.getDeclaringType(),
                    methodDescription.getInternalName(),
                    methodDescription.getReturnType().asRawType(),
                    methodDescription.getParameters().asTypeList());

        }

        /**
         * Creates a method handle representation of the given method for an explicit special method invocation of an otherwise virtual method.
         *
         * @param method The method ro represent.
         * @param type   The type on which the method is to be invoked on as a special method invocation.
         * @return A method handle representing the given method as special method invocation.
         */
        public static MethodHandle ofSpecial(Method method, Class<?> type) {
            return ofSpecial(new MethodDescription.ForLoadedMethod(nonNull(method)), new TypeDescription.ForLoadedType(nonNull(type)));
        }

        /**
         * Creates a method handle representation of the given method for an explicit special method invocation of an otherwise virtual method.
         *
         * @param methodDescription The method ro represent.
         * @param typeDescription   The type on which the method is to be invoked on as a special method invocation.
         * @return A method handle representing the given method as special method invocation.
         */
        public static MethodHandle ofSpecial(MethodDescription methodDescription, TypeDescription typeDescription) {
            if (!methodDescription.isSpecializableFor(typeDescription)) {
                throw new IllegalArgumentException("Cannot specialize " + methodDescription + " for " + typeDescription);
            }
            return new MethodHandle(HandleType.ofSpecial(methodDescription),
                    typeDescription,
                    methodDescription.getInternalName(),
                    methodDescription.getReturnType().asRawType(),
                    methodDescription.getParameters().asTypeList());
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
        public static MethodHandle ofGetter(FieldDescription fieldDescription) {
            return new MethodHandle(HandleType.ofGetter(fieldDescription),
                    fieldDescription.getDeclaringType(),
                    fieldDescription.getInternalName(),
                    fieldDescription.getType().asRawType(),
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
        public static MethodHandle ofSetter(FieldDescription fieldDescription) {
            return new MethodHandle(HandleType.ofSetter(fieldDescription),
                    fieldDescription.getDeclaringType(),
                    fieldDescription.getInternalName(),
                    TypeDescription.VOID,
                    Collections.singletonList(fieldDescription.getType().asRawType()));
        }

        @Override
        public Object asConstantPoolValue() {
            StringBuilder stringBuilder = new StringBuilder("(");
            for (TypeDescription parameterType : getParameterTypes()) {
                stringBuilder.append(parameterType.getDescriptor());
            }
            String descriptor = stringBuilder.append(")").append(getReturnType().getDescriptor()).toString();
            return new Handle(getHandleType().getIdentifier(), getOwnerType().getInternalName(), getName(), descriptor);
        }

        @Override
        public StackManipulation asStackManipulation() {
            return MethodHandleConstant.of(this);
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
        public List<TypeDescription> getParameterTypes() {
            return new ArrayList<TypeDescription>(parameterTypes);
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
            return "JavaInstance.MethodHandle{" +
                    "handleType=" + handleType +
                    ", ownerType=" + ownerType +
                    ", name='" + name + '\'' +
                    ", returnType=" + returnType +
                    ", parameterTypes=" + parameterTypes +
                    '}';
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
            protected static HandleType of(MethodDescription methodDescription) {
                if (methodDescription.isStatic()) {
                    return INVOKE_STATIC;
                } else if (methodDescription.isPrivate()) {
                    return INVOKE_SPECIAL;
                } else if (methodDescription.isConstructor()) {
                    return INVOKE_SPECIAL_CONSTRUCTOR;
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
            protected static HandleType ofSpecial(MethodDescription methodDescription) {
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
            protected static HandleType ofGetter(FieldDescription fieldDescription) {
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
            protected static HandleType ofSetter(FieldDescription fieldDescription) {
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
                return "JavaInstance.MethodHandle.HandleType." + name();
            }
        }
    }
}
