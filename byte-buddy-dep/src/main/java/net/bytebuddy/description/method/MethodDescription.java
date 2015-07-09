package net.bytebuddy.description.method;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.description.type.generic.TypeVariableSource;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaInstance;
import net.bytebuddy.utility.JavaType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureWriter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Implementations of this interface describe a Java method, i.e. a method or a constructor. Implementations of this
 * interface must provide meaningful {@code equal(Object)} and {@code hashCode()} implementations.
 */
public interface MethodDescription extends TypeVariableSource, NamedElement.WithGenericName {

    /**
     * The internal name of a Java constructor.
     */
    String CONSTRUCTOR_INTERNAL_NAME = "<init>";

    /**
     * The internal name of a Java static initializer.
     */
    String TYPE_INITIALIZER_INTERNAL_NAME = "<clinit>";

    /**
     * The type initializer of any representation of a type initializer.
     */
    int TYPE_INITIALIZER_MODIFIER = Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;

    Object NO_DEFAULT_VALUE = null;

    GenericTypeDescription getReturnType();

    /**
     * Returns a list of this method's parameters.
     *
     * @return A list of this method's parameters.
     */
    ParameterList getParameters();

    GenericTypeList getExceptionTypes();

    String getUniqueSignature();

    /**
     * Returns this method modifier but adjusts its state of being abstract.
     *
     * @param nonAbstract {@code true} if the method should be treated as non-abstract.
     * @return The adjusted modifiers.
     */
    int getAdjustedModifiers(boolean nonAbstract);

    /**
     * Checks if this method description represents a constructor.
     *
     * @return {@code true} if this method description represents a constructor.
     */
    boolean isConstructor();

    /**
     * Checks if this method description represents a method, i.e. not a constructor or a type initializer.
     *
     * @return {@code true} if this method description represents a method.
     */
    boolean isMethod();

    /**
     * Checks if this method is a type initializer.
     *
     * @return {@code true} if this method description represents a type initializer.
     */
    boolean isTypeInitializer();

    /**
     * Verifies if a method description represents a given loaded method.
     *
     * @param method The method to be checked.
     * @return {@code true} if this method description represents the given loaded method.
     */
    boolean represents(Method method);

    /**
     * Verifies if a method description represents a given loaded constructor.
     *
     * @param constructor The constructor to be checked.
     * @return {@code true} if this method description represents the given loaded constructor.
     */
    boolean represents(Constructor<?> constructor);

    /**
     * Verifies if this method description represents an overridable method.
     *
     * @return {@code true} if this method description represents an overridable method.
     */
    boolean isOverridable();

    /**
     * Returns the size of the local variable array that is required for this method, i.e. the size of all parameters
     * if they were loaded on the stack including a reference to {@code this} if this method represented a non-static
     * method.
     *
     * @return The size of this method on the operand stack.
     */
    int getStackSize();

    /**
     * Checks if this method represents a Java 8+ default method.
     *
     * @return {@code true} if this method is a default method.
     */
    boolean isDefaultMethod();

    /**
     * Checks if this method can be called using the {@code INVOKESPECIAL} for a given type.
     *
     * @param typeDescription The type o
     * @return {@code true} if this method can be called using the {@code INVOKESPECIAL} instruction
     * using the given type.
     */
    boolean isSpecializableFor(TypeDescription typeDescription);

    /**
     * Returns the default value of this method or {@code null} if no such value exists. The returned values might be
     * of a different type than usual:
     * <ul>
     * <li>{@link java.lang.Class} values are represented as
     * {@link TypeDescription}s.</li>
     * <li>{@link java.lang.annotation.Annotation} values are represented as
     * {@link AnnotationDescription}s</li>
     * <li>{@link java.lang.Enum} values are represented as
     * {@link net.bytebuddy.description.enumeration.EnumerationDescription}s.</li>
     * <li>Arrays of the latter types are represented as arrays of the named wrapper types.</li>
     * </ul>
     *
     * @return The default value of this method or {@code null}.
     */
    Object getDefaultValue();

    /**
     * Returns the default value but casts it to the given type. If the type differs from the value, a
     * {@link java.lang.ClassCastException} is thrown.
     *
     * @param type The type to cast the default value to.
     * @param <T>  The type to cast the default value to.
     * @return The casted default value.
     */
    <T> T getDefaultValue(Class<T> type);

    /**
     * Asserts if this method is invokable on an instance of the given type, i.e. the method is an instance method or
     * a constructor and the method is visible to the type and can be invoked on the given instance.
     *
     * @param typeDescription The type to check.
     * @return {@code true} if this method is invokable on an instance of the given type.
     */
    boolean isInvokableOn(TypeDescription typeDescription);

    /**
     * Checks if the method is a bootstrap method.
     *
     * @return {@code true} if the method is a bootstrap method.
     */
    boolean isBootstrap();

    /**
     * Checks if the method is a bootstrap method that accepts the given arguments.
     *
     * @param arguments The arguments that the bootstrap method is expected to accept where primitive values
     *                  are to be represented as their wrapper types, loaded types by {@link TypeDescription},
     *                  method handles by {@link net.bytebuddy.utility.JavaInstance.MethodHandle} instances and
     *                  method types by {@link net.bytebuddy.utility.JavaInstance.MethodType} instances.
     * @return {@code true} if the method is a bootstrap method that accepts the given arguments.
     */
    boolean isBootstrap(List<?> arguments);

    /**
     * Checks if this method is capable of defining a default annotation value.
     *
     * @return {@code true} if it is possible to define a default annotation value for this method.
     */
    boolean isDefaultValue();

    /**
     * Checks if the given value can describe a default annotation value for this method.
     *
     * @param value The value that describes the default annotation value for this method.
     * @return {@code true} if the given value can describe a default annotation value for this method.
     */
    boolean isDefaultValue(Object value);

    Token asToken(ElementMatcher<? super TypeDescription> targetTypeMatcher);

    /**
     * An abstract base implementation of a method description.
     */
    abstract class AbstractMethodDescription extends AbstractModifierReviewable implements MethodDescription {

        /**
         * A merger of all method modifiers that are visible in the Java source code.
         */
        private static final int SOURCE_MODIFIERS = Modifier.PUBLIC
                | Modifier.PROTECTED
                | Modifier.PRIVATE
                | Modifier.ABSTRACT
                | Modifier.STATIC
                | Modifier.FINAL
                | Modifier.SYNCHRONIZED
                | Modifier.NATIVE;

        @Override
        public String getUniqueSignature() {
            return getInternalName() + getDescriptor();
        }

        @Override
        public int getStackSize() {
            return getParameters().asTypeList().getStackSize() + (isStatic() ? 0 : 1);
        }

        @Override
        public boolean isMethod() {
            return !isConstructor() && !isTypeInitializer();
        }

        @Override
        public boolean isConstructor() {
            return CONSTRUCTOR_INTERNAL_NAME.equals(getInternalName());
        }

        @Override
        public boolean isTypeInitializer() {
            return TYPE_INITIALIZER_INTERNAL_NAME.equals(getInternalName());
        }

        @Override
        public boolean represents(Method method) {
            return equals(new ForLoadedMethod(method));
        }

        @Override
        public boolean represents(Constructor<?> constructor) {
            return equals(new ForLoadedConstructor(constructor));
        }

        @Override
        public String getName() {
            return isMethod()
                    ? getInternalName()
                    : getDeclaringType().getName();
        }

        @Override
        public String getSourceCodeName() {
            return isMethod()
                    ? getName()
                    : EMPTY_NAME;
        }

        @Override
        public String getDescriptor() {
            StringBuilder descriptor = new StringBuilder("(");
            for (TypeDescription parameterType : getParameters().asTypeList().asRawTypes()) {
                descriptor.append(parameterType.getDescriptor());
            }
            return descriptor.append(")").append(getReturnType().asRawType().getDescriptor()).toString();
        }

        @Override
        public String getGenericSignature() {
            SignatureWriter signatureWriter = new SignatureWriter();
            boolean generic = false;
            for (GenericTypeDescription typeVariable : getTypeVariables()) {
                signatureWriter.visitFormalTypeParameter(typeVariable.getSymbol());
                boolean classBound = true;
                for (GenericTypeDescription upperBound : typeVariable.getUpperBounds()) {
                    upperBound.accept(new GenericTypeDescription.Visitor.ForSignatureVisitor(classBound
                            ? signatureWriter.visitClassBound()
                            : signatureWriter.visitInterfaceBound()));
                    classBound = false;
                }
                generic = true;
            }
            for (GenericTypeDescription parameterType : getParameters().asTypeList()) {
                parameterType.accept(new GenericTypeDescription.Visitor.ForSignatureVisitor(signatureWriter.visitParameterType()));
                generic = generic || !parameterType.getSort().isNonGeneric();
            }
            GenericTypeDescription returnType = getReturnType();
            returnType.accept(new GenericTypeDescription.Visitor.ForSignatureVisitor(signatureWriter.visitReturnType()));
            generic = generic || !returnType.getSort().isNonGeneric();
            for (GenericTypeDescription exceptionType : getExceptionTypes()) {
                exceptionType.accept(new GenericTypeDescription.Visitor.ForSignatureVisitor(signatureWriter.visitExceptionType()));
                generic = generic || !exceptionType.getSort().isNonGeneric();
            }
            return generic
                    ? signatureWriter.toString()
                    : null;
        }

        @Override
        public int getAdjustedModifiers(boolean nonAbstract) {
            return nonAbstract
                    ? getModifiers() & ~(Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)
                    : getModifiers() & ~Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT;
        }

        @Override
        public boolean isVisibleTo(TypeDescription typeDescription) {
            return getDeclaringType().isVisibleTo(typeDescription)
                    && (isPublic()
                    || typeDescription.equals(getDeclaringType())
                    || (isProtected() && getDeclaringType().isAssignableFrom(typeDescription))
                    || (!isPrivate() && typeDescription.isSamePackage(getDeclaringType())));
        }

        @Override
        public boolean isOverridable() {
            return !(isConstructor() || isFinal() || isPrivate() || isStatic());
        }

        @Override
        public boolean isDefaultMethod() {
            return !isAbstract() && !isBridge() && getDeclaringType().isInterface();
        }

        @Override
        public boolean isSpecializableFor(TypeDescription targetType) {
            if (isStatic()) { // Static private methods are never specializable, check static property first
                return false;
            } else if (isPrivate() || isConstructor()) {
                return getDeclaringType().equals(targetType);
            } else {
                return !isAbstract() && getDeclaringType().isAssignableFrom(targetType);
            }
        }

        @Override
        public <T> T getDefaultValue(Class<T> type) {
            return type.cast(getDefaultValue());
        }

        @Override
        public boolean isInvokableOn(TypeDescription typeDescription) {
            return !isStatic()
                    && !isTypeInitializer()
                    && isVisibleTo(typeDescription)
                    && getDeclaringType().isAssignableFrom(typeDescription);
        }

        @Override
        public boolean isBootstrap() {
            TypeDescription returnType = getReturnType().asRawType();
            if ((isMethod() && (!isStatic()
                    || !(JavaType.CALL_SITE.getTypeStub().isAssignableFrom(returnType) || JavaType.CALL_SITE.getTypeStub().isAssignableTo(returnType))))
                    || (isConstructor() && !JavaType.CALL_SITE.getTypeStub().isAssignableFrom(getDeclaringType()))) {
                return false;
            }
            TypeList parameterTypes = getParameters().asTypeList().asRawTypes();
            switch (parameterTypes.size()) {
                case 0:
                    return false;
                case 1:
                    return parameterTypes.getOnly().represents(Object[].class);
                case 2:
                    return JavaType.METHOD_HANDLES_LOOKUP.getTypeStub().isAssignableTo(parameterTypes.get(0))
                            && parameterTypes.get(1).represents(Object[].class);
                case 3:
                    return JavaType.METHOD_HANDLES_LOOKUP.getTypeStub().isAssignableTo(parameterTypes.get(0))
                            && (parameterTypes.get(1).represents(Object.class) || parameterTypes.get(1).represents(String.class))
                            && (parameterTypes.get(2).represents(Object[].class) || JavaType.METHOD_TYPE.getTypeStub().isAssignableTo(parameterTypes.get(2)));
                default:
                    if (!(JavaType.METHOD_HANDLES_LOOKUP.getTypeStub().isAssignableTo(parameterTypes.get(0))
                            && (parameterTypes.get(1).represents(Object.class) || parameterTypes.get(1).represents(String.class))
                            && (JavaType.METHOD_TYPE.getTypeStub().isAssignableTo(parameterTypes.get(2))))) {
                        return false;
                    }
                    int parameterIndex = 4;
                    for (TypeDescription parameterType : parameterTypes.subList(3, parameterTypes.size())) {
                        if (!parameterType.represents(Object.class) && !parameterType.isConstantPool()) {
                            return parameterType.represents(Object[].class) && parameterIndex == parameterTypes.size();
                        }
                        parameterIndex++;
                    }
                    return true;
            }
        }

        @Override
        public boolean isBootstrap(List<?> arguments) {
            if (!isBootstrap()) {
                return false;
            }
            for (Object argument : arguments) {
                Class<?> argumentType = argument.getClass();
                if (!(argumentType == String.class
                        || argumentType == Integer.class
                        || argumentType == Long.class
                        || argumentType == Float.class
                        || argumentType == Double.class
                        || TypeDescription.class.isAssignableFrom(argumentType)
                        || JavaInstance.MethodHandle.class.isAssignableFrom(argumentType)
                        || JavaInstance.MethodType.class.isAssignableFrom(argumentType))) {
                    throw new IllegalArgumentException("Not a bootstrap argument: " + argument);
                }
            }
            TypeList parameterTypes = getParameters().asTypeList().asRawTypes();
            // The following assumes that the bootstrap method is a valid one, as checked above.
            if (parameterTypes.size() < 4) {
                return arguments.isEmpty() || parameterTypes.get(parameterTypes.size() - 1).represents(Object[].class);
            } else {
                int index = 4;
                Iterator<?> argumentIterator = arguments.iterator();
                for (TypeDescription parameterType : parameterTypes.subList(3, parameterTypes.size())) {
                    boolean finalParameterCheck = !argumentIterator.hasNext();
                    if (!finalParameterCheck) {
                        Class<?> argumentType = argumentIterator.next().getClass();
                        finalParameterCheck = !(parameterType.represents(String.class) && argumentType == String.class)
                                && !(parameterType.represents(int.class) && argumentType == Integer.class)
                                && !(parameterType.represents(long.class) && argumentType == Long.class)
                                && !(parameterType.represents(float.class) && argumentType == Float.class)
                                && !(parameterType.represents(double.class) && argumentType == Double.class)
                                && !(parameterType.represents(Class.class) && TypeDescription.class.isAssignableFrom(argumentType))
                                && !(parameterType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub()) && JavaInstance.MethodHandle.class.isAssignableFrom(argumentType))
                                && !(parameterType.equals(JavaType.METHOD_TYPE.getTypeStub()) && JavaInstance.MethodType.class.isAssignableFrom(argumentType));
                    }
                    if (finalParameterCheck) {
                        return index == parameterTypes.size() && parameterType.represents(Object[].class);
                    }
                    index++;
                }
                return true;
            }
        }

        @Override
        public boolean isDefaultValue() {
            return !isConstructor()
                    && !isStatic()
                    && getReturnType().asRawType().isAnnotationReturnType()
                    && getParameters().isEmpty();
        }

        @Override
        public boolean isDefaultValue(Object value) {
            if (!isDefaultValue()) {
                return false;
            }
            TypeDescription returnType = getReturnType().asRawType();
            return (returnType.represents(boolean.class) && value instanceof Boolean)
                    || (returnType.represents(boolean.class) && value instanceof Boolean)
                    || (returnType.represents(byte.class) && value instanceof Byte)
                    || (returnType.represents(char.class) && value instanceof Character)
                    || (returnType.represents(short.class) && value instanceof Short)
                    || (returnType.represents(int.class) && value instanceof Integer)
                    || (returnType.represents(long.class) && value instanceof Long)
                    || (returnType.represents(float.class) && value instanceof Float)
                    || (returnType.represents(long.class) && value instanceof Long)
                    || (returnType.represents(String.class) && value instanceof String)
                    || (returnType.isAssignableTo(Enum.class) && value instanceof EnumerationDescription)
                    || (returnType.isAssignableTo(Annotation.class) && value instanceof AnnotationDescription)
                    || (returnType.represents(Class.class) && value instanceof TypeDescription);
        }

        @Override
        public TypeVariableSource getEnclosingSource() {
            return getDeclaringType();
        }

        @Override
        public GenericTypeDescription findVariable(String symbol) {
            GenericTypeList typeVariables = getTypeVariables().filter(named(symbol));
            return typeVariables.isEmpty()
                    ? getEnclosingSource().findVariable(symbol)
                    : typeVariables.getOnly();
        }

        @Override
        public <T> T accept(TypeVariableSource.Visitor<T> visitor) {
            return visitor.onMethod(this);
        }

        @Override
        public Token asToken(ElementMatcher<? super TypeDescription> targetTypeMatcher) {
            GenericTypeDescription.Visitor<GenericTypeDescription> visitor = new GenericTypeDescription.Visitor.Substitutor.ForDetachment(targetTypeMatcher);
            return new Token(getInternalName(),
                    getModifiers(),
                    getTypeVariables().accept(visitor),
                    getReturnType().accept(visitor),
                    getParameters().asTokens(targetTypeMatcher),
                    getExceptionTypes().accept(visitor),
                    getDeclaredAnnotations(),
                    getDefaultValue());
        }

        @Override
        public boolean equals(Object other) {
            return other == this || other instanceof MethodDescription
                    && getInternalName().equals(((MethodDescription) other).getInternalName())
                    && getDeclaringType().equals(((MethodDescription) other).getDeclaringType())
                    && getReturnType().asRawType().equals(((MethodDescription) other).getReturnType().asRawType())
                    && getParameters().asTypeList().asRawTypes().equals(((MethodDescription) other).getParameters().asTypeList().asRawTypes());
        }

        @Override
        public int hashCode() {
            int hashCode = getDeclaringType().hashCode();
            hashCode = 31 * hashCode + getInternalName().hashCode();
            hashCode = 31 * hashCode + getReturnType().asRawType().hashCode();
            return 31 * hashCode + getParameters().asTypeList().asRawTypes().hashCode();
        }

        @Override
        public String toGenericString() {
            StringBuilder stringBuilder = new StringBuilder();
            int modifiers = getModifiers() & SOURCE_MODIFIERS;
            if (modifiers != EMPTY_MASK) {
                stringBuilder.append(Modifier.toString(modifiers)).append(" ");
            }
            if (isMethod()) {
                stringBuilder.append(getReturnType().getSourceCodeName()).append(" ");
                stringBuilder.append(getDeclaringType().getSourceCodeName()).append(".");
            }
            stringBuilder.append(getName()).append("(");
            boolean first = true;
            for (GenericTypeDescription typeDescription : getParameters().asTypeList()) {
                if (!first) {
                    stringBuilder.append(",");
                } else {
                    first = false;
                }
                stringBuilder.append(typeDescription.getSourceCodeName());
            }
            stringBuilder.append(")");
            GenericTypeList exceptionTypes = getExceptionTypes();
            if (exceptionTypes.size() > 0) {
                stringBuilder.append(" throws ");
                first = true;
                for (GenericTypeDescription typeDescription : exceptionTypes) {
                    if (!first) {
                        stringBuilder.append(",");
                    } else {
                        first = false;
                    }
                    stringBuilder.append(typeDescription.getSourceCodeName());
                }
            }
            return stringBuilder.toString();
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            int modifiers = getModifiers() & SOURCE_MODIFIERS;
            if (modifiers != EMPTY_MASK) {
                stringBuilder.append(Modifier.toString(modifiers)).append(" ");
            }
            if (isMethod()) {
                stringBuilder.append(getReturnType().asRawType().getSourceCodeName()).append(" ");
                stringBuilder.append(getDeclaringType().getSourceCodeName()).append(".");
            }
            stringBuilder.append(getName()).append("(");
            boolean first = true;
            for (TypeDescription typeDescription : getParameters().asTypeList().asRawTypes()) {
                if (!first) {
                    stringBuilder.append(",");
                } else {
                    first = false;
                }
                stringBuilder.append(typeDescription.getSourceCodeName());
            }
            stringBuilder.append(")");
            TypeList exceptionTypes = getExceptionTypes().asRawTypes();
            if (exceptionTypes.size() > 0) {
                stringBuilder.append(" throws ");
                first = true;
                for (TypeDescription typeDescription : exceptionTypes) {
                    if (!first) {
                        stringBuilder.append(",");
                    } else {
                        first = false;
                    }
                    stringBuilder.append(typeDescription.getSourceCodeName());
                }
            }
            return stringBuilder.toString();
        }
    }

    /**
     * An implementation of a method description for a loaded constructor.
     */
    class ForLoadedConstructor extends AbstractMethodDescription {

        /**
         * The loaded constructor that is represented by this instance.
         */
        private final Constructor<?> constructor;

        /**
         * Creates a new immutable method description for a loaded constructor.
         *
         * @param constructor The loaded constructor to be represented by this method description.
         */
        public ForLoadedConstructor(Constructor<?> constructor) {
            this.constructor = constructor;
        }

        @Override
        public TypeDescription getDeclaringType() {
            return new TypeDescription.ForLoadedType(constructor.getDeclaringClass());
        }

        @Override
        public GenericTypeDescription getReturnType() {
            return TypeDescription.VOID;
        }

        @Override
        public ParameterList getParameters() {
            return ParameterList.ForLoadedExecutable.of(constructor);
        }

        @Override
        public GenericTypeList getExceptionTypes() {
            return new GenericTypeList.LazyProjection.OfConstructorExceptionTypes(constructor);
        }

        @Override
        public boolean isConstructor() {
            return true;
        }

        @Override
        public boolean isTypeInitializer() {
            return false;
        }

        @Override
        public boolean represents(Method method) {
            return false;
        }

        @Override
        public boolean represents(Constructor<?> constructor) {
            return this.constructor.equals(constructor) || equals(new ForLoadedConstructor(constructor));
        }

        @Override
        public String getName() {
            return constructor.getName();
        }

        @Override
        public int getModifiers() {
            return constructor.getModifiers();
        }

        @Override
        public boolean isSynthetic() {
            return constructor.isSynthetic();
        }

        @Override
        public String getInternalName() {
            return CONSTRUCTOR_INTERNAL_NAME;
        }

        @Override
        public String getDescriptor() {
            return Type.getConstructorDescriptor(constructor);
        }

        @Override
        public Object getDefaultValue() {
            return null;
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.ForLoadedAnnotation(constructor.getDeclaredAnnotations());
        }

        @Override
        public GenericTypeList getTypeVariables() {
            return new GenericTypeList.ForLoadedType(constructor.getTypeParameters());
        }
    }

    /**
     * An implementation of a method description for a loaded method.
     */
    class ForLoadedMethod extends AbstractMethodDescription {

        /**
         * The loaded method that is represented by this instance.
         */
        private final Method method;

        /**
         * Creates a new immutable method description for a loaded method.
         *
         * @param method The loaded method to be represented by this method description.
         */
        public ForLoadedMethod(Method method) {
            this.method = method;
        }

        @Override
        public TypeDescription getDeclaringType() {
            return new TypeDescription.ForLoadedType(method.getDeclaringClass());
        }

        @Override
        public GenericTypeDescription getReturnType() {
            return new GenericTypeDescription.LazyProjection.OfLoadedReturnType(method);
        }

        @Override
        public ParameterList getParameters() {
            return ParameterList.ForLoadedExecutable.of(method);
        }

        @Override
        public GenericTypeList getExceptionTypes() {
            return new GenericTypeList.LazyProjection.OfMethodExceptionTypes(method);
        }

        @Override
        public boolean isConstructor() {
            return false;
        }

        @Override
        public boolean isTypeInitializer() {
            return false;
        }

        @Override
        public boolean isBridge() {
            return method.isBridge();
        }

        @Override
        public boolean represents(Method method) {
            return this.method.equals(method) || equals(new ForLoadedMethod(method));
        }

        @Override
        public boolean represents(Constructor<?> constructor) {
            return false;
        }

        @Override
        public String getName() {
            return method.getName();
        }

        @Override
        public int getModifiers() {
            return method.getModifiers();
        }

        @Override
        public boolean isSynthetic() {
            return method.isSynthetic();
        }

        @Override
        public String getInternalName() {
            return method.getName();
        }

        @Override
        public String getDescriptor() {
            return Type.getMethodDescriptor(method);
        }

        /**
         * Returns the loaded method that is represented by this method description.
         *
         * @return The loaded method that is represented by this method description.
         */
        public Method getLoadedMethod() {
            return method;
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.ForLoadedAnnotation(method.getDeclaredAnnotations());
        }

        @Override
        public Object getDefaultValue() {
            Object value = method.getDefaultValue();
            return value == null
                    ? null
                    : AnnotationDescription.ForLoadedAnnotation.describe(value, new TypeDescription.ForLoadedType(method.getReturnType()));
        }

        @Override
        public GenericTypeList getTypeVariables() {
            return new GenericTypeList.ForLoadedType(method.getTypeParameters());
        }
    }

    /**
     * A latent method description describes a method that is not attached to a declaring
     * {@link TypeDescription}.
     */
    class Latent extends AbstractMethodDescription {

        /**
         * The type that is declaring this method.
         */
        private final TypeDescription declaringType;

        /**
         * the internal name of this method.
         */
        private final String internalName;

        /**
         * The modifiers of this method.
         */
        private final int modifiers;

        private final List<? extends GenericTypeDescription> typeVariables;

        /**
         * The return type of this method.
         */
        private final GenericTypeDescription returnType;

        private final List<? extends ParameterDescription.Token> parameterTokens;

        /**
         * This method's exception types.
         */
        private final List<? extends GenericTypeDescription> exceptionTypes;

        private final List<? extends AnnotationDescription> declaredAnnotations;

        private final Object defaultValue;

        public Latent(TypeDescription declaringType, Token token) {
            this(declaringType,
                    token.getInternalName(),
                    token.getModifiers(),
                    token.getTypeVariables(),
                    token.getReturnType(),
                    token.getParameterTokens(),
                    token.getExceptionTypes(),
                    token.getAnnotations(),
                    token.getDefaultValue());
        }

        public Latent(TypeDescription declaringType,
                      String internalName,
                      int modifiers,
                      List<? extends GenericTypeDescription> typeVariables,
                      GenericTypeDescription returnType,
                      List<? extends ParameterDescription.Token> parameterTokens,
                      List<? extends GenericTypeDescription> exceptionTypes,
                      List<? extends AnnotationDescription> declaredAnnotations,
                      Object defaultValue) {
            this.declaringType = declaringType;
            this.internalName = internalName;
            this.modifiers = modifiers;
            this.typeVariables = typeVariables;
            this.returnType = returnType;
            this.parameterTokens = parameterTokens;
            this.exceptionTypes = exceptionTypes;
            this.declaredAnnotations = declaredAnnotations;
            this.defaultValue = defaultValue;
        }

        @Override
        public GenericTypeList getTypeVariables() {
            return GenericTypeList.ForDetachedTypes.OfTypeVariable.attach(this, typeVariables);
        }

        @Override
        public GenericTypeDescription getReturnType() {
            return returnType.accept(GenericTypeDescription.Visitor.Substitutor.ForAttachment.of(this));
        }

        @Override
        public ParameterList getParameters() {
            return new ParameterList.ForTokens(this, parameterTokens);
        }

        @Override
        public GenericTypeList getExceptionTypes() {
            return GenericTypeList.ForDetachedTypes.attach(this, exceptionTypes);
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Explicit(declaredAnnotations);
        }

        @Override
        public String getInternalName() {
            return internalName;
        }

        @Override
        public TypeDescription getDeclaringType() {
            return declaringType;
        }

        @Override
        public int getModifiers() {
            return modifiers;
        }

        @Override
        public Object getDefaultValue() {
            return defaultValue;
        }

        public static class TypeInitializer extends MethodDescription.AbstractMethodDescription {

            private final TypeDescription typeDescription;

            public TypeInitializer(TypeDescription typeDescription) {
                this.typeDescription = typeDescription;
            }

            @Override
            public GenericTypeDescription getReturnType() {
                return TypeDescription.VOID;
            }

            @Override
            public ParameterList getParameters() {
                return new ParameterList.Empty();
            }

            @Override
            public GenericTypeList getExceptionTypes() {
                return new GenericTypeList.Empty();
            }

            @Override
            public Object getDefaultValue() {
                return NO_DEFAULT_VALUE;
            }

            @Override
            public GenericTypeList getTypeVariables() {
                return new GenericTypeList.Empty();
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return new AnnotationList.Empty();
            }

            @Override
            public TypeDescription getDeclaringType() {
                return typeDescription;
            }

            @Override
            public int getModifiers() {
                return TYPE_INITIALIZER_MODIFIER;
            }

            @Override
            public String getInternalName() {
                return MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME;
            }
        }
    }

    class TypeSubstituting extends AbstractMethodDescription {

        private final MethodDescription methodDescription;

        private final GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor;

        public TypeSubstituting(MethodDescription methodDescription, GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor) {
            this.methodDescription = methodDescription;
            this.visitor = visitor;
        }

        @Override
        public GenericTypeList getTypeVariables() {
            return methodDescription.getTypeVariables().accept(visitor);
        }

        @Override
        public GenericTypeDescription getReturnType() {
            return methodDescription.getReturnType().accept(visitor);
        }

        @Override
        public ParameterList getParameters() {
            return new ParameterList.Substituted(methodDescription.getParameters(), visitor);
        }

        @Override
        public GenericTypeList getExceptionTypes() {
            return methodDescription.getExceptionTypes().accept(visitor);
        }

        @Override
        public Object getDefaultValue() {
            return methodDescription.getDefaultValue();
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return methodDescription.getDeclaredAnnotations();
        }

        @Override
        public TypeDescription getDeclaringType() {
            return methodDescription.getDeclaringType();
        }

        @Override
        public int getModifiers() {
            return methodDescription.getModifiers();
        }

        @Override
        public String getInternalName() {
            return methodDescription.getInternalName();
        }
    }

    class Token implements ByteCodeElement.Token<Token> {

        private final String internalName;

        private final int modifiers;

        private final List<GenericTypeDescription> typeVariables;

        private final GenericTypeDescription returnType;

        private final List<? extends ParameterDescription.Token> parameterTokens;

        private final List<? extends GenericTypeDescription> exceptionTypes;

        private final List<? extends AnnotationDescription> annotations;

        private final Object defaultValue;

        public Token(String internalName,
                     int modifiers,
                     List<GenericTypeDescription> typeVariables,
                     GenericTypeDescription returnType,
                     List<? extends ParameterDescription.Token> parameterTokens,
                     List<? extends GenericTypeDescription> exceptionTypes,
                     List<? extends AnnotationDescription> annotations,
                     Object defaultValue) {
            this.internalName = internalName;
            this.modifiers = modifiers;
            this.typeVariables = typeVariables;
            this.returnType = returnType;
            this.parameterTokens = parameterTokens;
            this.exceptionTypes = exceptionTypes;
            this.annotations = annotations;
            this.defaultValue = defaultValue;
        }

        public String getInternalName() {
            return internalName;
        }

        public int getModifiers() {
            return modifiers;
        }

        public GenericTypeList getTypeVariables() {
            return new GenericTypeList.Explicit(typeVariables);
        }

        public GenericTypeDescription getReturnType() {
            return returnType;
        }

        public TokenList<ParameterDescription.Token> getParameterTokens() {
            return new TokenList<ParameterDescription.Token>(parameterTokens);
        }

        public GenericTypeList getExceptionTypes() {
            return new GenericTypeList.Explicit(exceptionTypes);
        }

        public AnnotationList getAnnotations() {
            return new AnnotationList.Explicit(annotations);
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        @Override
        public Token accept(GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor) {
            return new Token(getInternalName(),
                    getModifiers(),
                    getTypeVariables().accept(visitor),
                    getReturnType().accept(visitor),
                    getParameterTokens().accept(visitor),
                    getExceptionTypes().accept(visitor),
                    getAnnotations(),
                    getDefaultValue());
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Token)) return false;
            Token token = (Token) other;
            if (!internalName.equals(token.internalName)) return false;
            if (!returnType.asRawType().equals(token.returnType.asRawType())) return false;
            if (parameterTokens.size() != token.parameterTokens.size()) return false;
            for (int index = 0; index < parameterTokens.size(); index++) {
                if (!parameterTokens.get(index).getType().asRawType().equals(token.parameterTokens.get(index).getType().asRawType())) return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = internalName.hashCode();
            result = 31 * result + returnType.asRawType().hashCode();
            for (ParameterDescription.Token parameterToken : parameterTokens) {
                result = 31 * result + parameterToken.getType().asRawType().hashCode();
            }
            return result;
        }

        @Override
        public String toString() {
            return "MethodDescription.Token{" +
                    "internalName='" + internalName + '\'' +
                    ", returnType=" + returnType +
                    ", modifiers=" + modifiers +
                    ", parameterTokens=" + parameterTokens +
                    ", exceptionTypes=" + exceptionTypes +
                    ", annotations=" + annotations +
                    '}';
        }
    }
}
