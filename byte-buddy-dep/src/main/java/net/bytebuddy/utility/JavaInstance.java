package net.bytebuddy.utility;

import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.MethodHandleConstant;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.MethodTypeConstant;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

public interface JavaInstance {

    class MethodType implements JavaInstance {

        private static final JavaMethod RETURN_TYPE;

        private static final JavaMethod PARAMETER_ARRAY;

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

        public static MethodType of(Object methodType) {
            if (!JavaType.METHOD_TYPE.getTypeStub().isInstance(methodType)) {
                throw new IllegalArgumentException("Excpected method type object: " + methodType);
            }
            return of((Class<?>) RETURN_TYPE.invoke(methodType), (Class<?>[]) PARAMETER_ARRAY.invoke(methodType));
        }

        public static MethodType of(Class<?> returnType, Class<?>... parameterType) {
            return new MethodType(new TypeDescription.ForLoadedType(nonNull(returnType)), new TypeList.ForLoadedType(nonNull(parameterType)));
        }

        public static MethodType of(Method method) {
            return of(new MethodDescription.ForLoadedMethod(nonNull(method)));
        }

        public static MethodType of(Constructor<?> constructor) {
            return of(new MethodDescription.ForLoadedConstructor(nonNull(constructor)));
        }

        public static MethodType of(MethodDescription methodDescription) {
            return new MethodType(methodDescription.getReturnType(), methodDescription.getParameters().asTypeList());
        }

        public static MethodType ofSetter(Field field) {
            return ofSetter(new FieldDescription.ForLoadedField(nonNull(field)));
        }

        public static MethodType ofSetter(FieldDescription fieldDescription) {
            return new MethodType(TypeDescription.VOID, Collections.singletonList(fieldDescription.getFieldType()));
        }

        public static MethodType ofGetter(Field field) {
            return ofGetter(new FieldDescription.ForLoadedField(nonNull(field)));
        }

        public static MethodType ofGetter(FieldDescription fieldDescription) {
            return new MethodType(fieldDescription.getFieldType(), Collections.<TypeDescription>emptyList());
        }

        private final TypeDescription returnType;

        private final List<? extends TypeDescription> parameterTypes;

        public MethodType(TypeDescription returnType, List<? extends TypeDescription> parameterTypes) {
            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
        }

        public TypeDescription getReturnType() {
            return returnType;
        }

        public List<? extends TypeDescription> getParameterTypes() {
            return parameterTypes;
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
        public TypeDescription getTypeDescription() {
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
            return "TypeDescription.MethodTypeToken.Default{" +
                    "returnType=" + returnType +
                    ", parameterTypes=" + parameterTypes +
                    '}';
        }
    }

    class MethodHandle implements JavaInstance {

        public enum HandleType {

            INVOKE_VIRTUAL(Opcodes.H_INVOKEVIRTUAL),

            INVOKE_STATIC(Opcodes.H_INVOKESTATIC),

            INVOKE_SPECIAL(Opcodes.H_INVOKESPECIAL),

            INVOKE_INTERFACE(Opcodes.H_INVOKEINTERFACE),

            INVOKE_SPECIAL_CONSTRUCTOR(Opcodes.H_NEWINVOKESPECIAL),

            PUT_FIELD(Opcodes.H_PUTFIELD),

            GET_FIELD(Opcodes.H_GETFIELD),

            PUT_STATIC_FIELD(Opcodes.H_PUTSTATIC),

            GET_STATIC_FIELD(Opcodes.H_GETSTATIC);

            final int identifier;

            HandleType(int identifier) {
                this.identifier = identifier;
            }

            public int getIdentifier() {
                return identifier;
            }

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

            protected static HandleType of(int handle) {
                for (HandleType handleType : HandleType.values()) {
                    if (handleType.getIdentifier() == handle) {
                        return handleType;
                    }
                }
                throw new IllegalArgumentException("Unknown handle type: " + handle);
            }

            protected static HandleType ofSpecial(MethodDescription methodDescription) {
                if (methodDescription.isStatic() || methodDescription.isAbstract()) {
                    throw new IllegalArgumentException("Cannot invoke " + methodDescription + " via invokespecial");
                }
                return methodDescription.isConstructor()
                        ? INVOKE_SPECIAL_CONSTRUCTOR
                        : INVOKE_SPECIAL;
            }

            protected static HandleType ofGetter(FieldDescription fieldDescription) {
                return fieldDescription.isStatic()
                        ? GET_STATIC_FIELD
                        : GET_FIELD;
            }

            protected static HandleType ofSetter(FieldDescription fieldDescription) {
                return fieldDescription.isStatic()
                        ? PUT_STATIC_FIELD
                        : PUT_FIELD;
            }

            @Override
            public String toString() {
                return "TypeDescription.MethodHandleToken.Default.HandleType." + name();
            }
        }

        private static final JavaMethod REVEAL_DIRECT;

        private static final JavaMethod LOOKUP;

        private static final JavaMethod GET_NAME;

        private static final JavaMethod GET_DECLARING_TYPE;

        private static final JavaMethod GET_REFERENCE_TYPE;

        private static final JavaMethod GET_METHOD_TYPE;

        private static final JavaMethod RETURN_TYPE;

        private static final JavaMethod PARAMETER_ARRAY;

        static {
            JavaMethod revealDirect, lookup, getName, getDeclaringType, getReferenceType, getMethodType, returnType, parameterArray;
            try {
                Class<?> methodHandle = JavaType.METHOD_HANDLE.load();
                revealDirect = new JavaMethod.ForLoadedMethod(methodHandle.getDeclaredMethod("revealDirect", JavaType.METHOD_HANDLE.load()));
                Class<?> methodHandles = Class.forName("java.lang.invoke.MethodHandles");
                lookup = new JavaMethod.ForLoadedMethod(methodHandles.getDeclaredMethod("publicLookup"));
                Class<?> methodHandleInfo = Class.forName("java.lang.invoke.MethodHandleInfo");
                getName = new JavaMethod.ForLoadedMethod(methodHandleInfo.getDeclaredMethod("returnType"));
                getDeclaringType = new JavaMethod.ForLoadedMethod(methodHandleInfo.getDeclaredMethod("parameterArray"));
                getReferenceType = new JavaMethod.ForLoadedMethod(methodHandleInfo.getDeclaredMethod("parameterArray"));
                getMethodType = new JavaMethod.ForLoadedMethod(methodHandleInfo.getDeclaredMethod("parameterArray"));
                Class<?> methodType = JavaType.METHOD_TYPE.load();
                returnType = new JavaMethod.ForLoadedMethod(methodType.getDeclaredMethod("returnType"));
                parameterArray = new JavaMethod.ForLoadedMethod(methodType.getDeclaredMethod("parameterArray"));
            } catch (Exception ignored) {
                revealDirect = JavaMethod.ForUnavailableMethod.INSTANCE;
                lookup = JavaMethod.ForUnavailableMethod.INSTANCE;
                getName = JavaMethod.ForUnavailableMethod.INSTANCE;
                getDeclaringType = JavaMethod.ForUnavailableMethod.INSTANCE;
                getReferenceType = JavaMethod.ForUnavailableMethod.INSTANCE;
                getMethodType = JavaMethod.ForUnavailableMethod.INSTANCE;
                returnType = JavaMethod.ForUnavailableMethod.INSTANCE;
                parameterArray = JavaMethod.ForUnavailableMethod.INSTANCE;
            }
            REVEAL_DIRECT = revealDirect;
            LOOKUP = lookup;
            GET_NAME = getName;
            GET_DECLARING_TYPE = getDeclaringType;
            GET_REFERENCE_TYPE = getReferenceType;
            GET_METHOD_TYPE = getMethodType;
            RETURN_TYPE = returnType;
            PARAMETER_ARRAY = parameterArray;
        }

        public static MethodHandle of(Object methodHandle) {
            return of(methodHandle, LOOKUP.invokeStatic());
        }

        public static MethodHandle of(Object methodHandle, Object lookup) {
            if (!JavaType.METHOD_HANDLE.getTypeStub().isInstance(methodHandle)) {
                throw new IllegalArgumentException("Expected method handle object: " + methodHandle);
            } else if (!JavaType.METHOD_TYPES_LOOKUP.getTypeStub().isInstance(lookup)) {
                throw new IllegalArgumentException("Expected method handle lookup object: " + lookup);
            }
            Object methodHandleInfo = REVEAL_DIRECT.invoke(lookup, methodHandle);
            Object methodType = GET_METHOD_TYPE.invoke(methodHandleInfo);
            return new MethodHandle(HandleType.of((Integer) GET_REFERENCE_TYPE.invoke(methodHandleInfo)),
                    new TypeDescription.ForLoadedType((Class<?>) GET_DECLARING_TYPE.invoke(methodHandleInfo)),
                    (String) GET_NAME.invoke(methodHandleInfo),
                    new TypeDescription.ForLoadedType((Class<?>) RETURN_TYPE.invoke(methodType)),
                    new TypeList.ForLoadedType((Class<?>[]) PARAMETER_ARRAY.invoke(methodType)));
        }

        public static MethodHandle of(Method method) {
            return of(new MethodDescription.ForLoadedMethod(nonNull(method)));
        }

        public static MethodHandle of(Constructor<?> constructor) {
            return of(new MethodDescription.ForLoadedConstructor(nonNull(constructor)));
        }

        public static MethodHandle of(MethodDescription methodDescription) {
            return new MethodHandle(HandleType.of(methodDescription),
                    methodDescription.getDeclaringType(),
                    methodDescription.getInternalName(),
                    methodDescription.getReturnType(),
                    methodDescription.getParameters().asTypeList());
        }

        public static MethodHandle ofSpecial(Method method) {
            return ofSpecial(new MethodDescription.ForLoadedMethod(nonNull(method)));
        }

        public static MethodHandle ofSpecial(MethodDescription methodDescription) {
            return new MethodHandle(HandleType.ofSpecial(methodDescription),
                    methodDescription.getDeclaringType(),
                    methodDescription.getInternalName(),
                    methodDescription.getReturnType(),
                    methodDescription.getParameters().asTypeList());
        }

        public static MethodHandle ofGetter(Field field) {
            return ofGetter(new FieldDescription.ForLoadedField(field));
        }

        public static MethodHandle ofGetter(FieldDescription fieldDescription) {
            return new MethodHandle(HandleType.ofGetter(fieldDescription),
                    fieldDescription.getDeclaringType(),
                    fieldDescription.getInternalName(),
                    fieldDescription.getFieldType(),
                    Collections.<TypeDescription>emptyList());
        }

        public static MethodHandle ofSetter(Field field) {
            return ofSetter(new FieldDescription.ForLoadedField(field));
        }

        public static MethodHandle ofSetter(FieldDescription fieldDescription) {
            return new MethodHandle(HandleType.ofSetter(fieldDescription),
                    fieldDescription.getDeclaringType(),
                    fieldDescription.getInternalName(),
                    TypeDescription.VOID,
                    Collections.singletonList(fieldDescription.getFieldType()));
        }

        private final HandleType handleType;

        private final TypeDescription ownerType;

        private final String name;

        private final TypeDescription returnType;

        private final List<? extends TypeDescription> parameterTypes;

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
        public TypeDescription getTypeDescription() {
            return JavaType.METHOD_HANDLE.getTypeStub();
        }

        public HandleType getHandleType() {
            return handleType;
        }

        public TypeDescription getOwnerType() {
            return ownerType;
        }

        public String getName() {
            return name;
        }

        public TypeDescription getReturnType() {
            return returnType;
        }

        public List<? extends TypeDescription> getParameterTypes() {
            return parameterTypes;
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
            return "TypeDescription.MethodHandleToken.Default{" +
                    "handleType=" + handleType +
                    ", ownerType=" + ownerType +
                    ", name='" + name + '\'' +
                    ", returnType=" + returnType +
                    ", parameterTypes=" + parameterTypes +
                    '}';
        }
    }

    Object asConstantPoolValue();

    StackManipulation asStackManipulation();

    TypeDescription getTypeDescription();
}
