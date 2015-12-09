package net.bytebuddy.description.type;

import net.bytebuddy.description.ModifierReviewable;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.description.type.generic.TypeVariableSource;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.utility.JavaType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureWriter;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericSignatureFormatError;
import java.lang.reflect.Method;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.utility.ByteBuddyCommons.join;

/**
 * Implementations of this interface represent a Java type, i.e. a class or interface. Instances of this interface always
 * represent non-generic types of sort {@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#NON_GENERIC}.
 */
public interface TypeDescription extends GenericTypeDescription, TypeVariableSource {

    /**
     * A representation of the {@link java.lang.Object} type.
     */
    TypeDescription OBJECT = new ForLoadedType(Object.class);

    /**
     * A representation of the {@link java.lang.String} type.
     */
    TypeDescription STRING = new ForLoadedType(String.class);

    /**
     * A representation of the {@link java.lang.Class} type.
     */
    TypeDescription CLASS = new ForLoadedType(Class.class);

    /**
     * A representation of the {@code void} non-type.
     */
    TypeDescription VOID = new ForLoadedType(void.class);

    /**
     * A representation of the {@link java.lang.Enum} type.
     */
    TypeDescription ENUM = new ForLoadedType(Enum.class);

    /**
     * The modifiers of any array type.
     */
    int ARRAY_MODIFIERS = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_ABSTRACT;

    /**
     * A list of interfaces that are implicitly implemented by any array type.
     */
    GenericTypeList ARRAY_INTERFACES = new GenericTypeList.ForLoadedType(Cloneable.class, Serializable.class);

    /**
     * Represents any undefined property of a type description that is instead represented as {@code null} in order
     * to resemble the Java reflection API which returns {@code null} and is intuitive to many Java developers.
     */
    TypeDescription UNDEFINED = null;

    @Override
    FieldList<FieldDescription.InDefinedShape> getDeclaredFields();

    @Override
    MethodList<MethodDescription.InDefinedShape> getDeclaredMethods();

    /**
     * Checks if {@code value} is an instance of the type represented by this instance.
     *
     * @param value The object of interest.
     * @return {@code true} if the object is an instance of the type described by this instance.
     */
    boolean isInstance(Object value);

    /**
     * Checks if {@code value} is an instance of the type represented by this instance or a wrapper instance of the
     * corresponding primitive value.
     *
     * @param value The object of interest.
     * @return {@code true} if the object is an instance or wrapper of the type described by this instance.
     */
    boolean isInstanceOrWrapper(Object value);

    /**
     * Checks if this type is assignable from the type described by this instance, for example for
     * {@code class Foo} and {@code class Bar extends Foo}, this method would return {@code true} for
     * {@code Foo.class.isAssignableFrom(Bar.class)}.
     *
     * @param type The type of interest.
     * @return {@code true} if this type is assignable from {@code type}.
     */
    boolean isAssignableFrom(Class<?> type);

    /**
     * Checks if this type is assignable from the type described by this instance, for example for
     * {@code class Foo} and {@code class Bar extends Foo}, this method would return {@code true} for
     * {@code Foo.class.isAssignableFrom(Bar.class)}.
     * <p>&nbsp;</p>
     * Implementations of this methods are allowed to delegate to
     * {@link TypeDescription#isAssignableFrom(Class)}
     *
     * @param typeDescription The type of interest.
     * @return {@code true} if this type is assignable from {@code type}.
     */
    boolean isAssignableFrom(TypeDescription typeDescription);

    /**
     * Checks if this type is assignable from the type described by this instance, for example for
     * {@code class Foo} and {@code class Bar extends Foo}, this method would return {@code true} for
     * {@code Bar.class.isAssignableTo(Foo.class)}.
     *
     * @param type The type of interest.
     * @return {@code true} if this type is assignable to {@code type}.
     */
    boolean isAssignableTo(Class<?> type);

    /**
     * Checks if this type is assignable from the type described by this instance, for example for
     * {@code class Foo} and {@code class Bar extends Foo}, this method would return {@code true} for
     * {@code Bar.class.isAssignableFrom(Foo.class)}.
     * <p>&nbsp;</p>
     * Implementations of this methods are allowed to delegate to
     * {@link TypeDescription#isAssignableTo(Class)}
     *
     * @param typeDescription The type of interest.
     * @return {@code true} if this type is assignable to {@code type}.
     */
    boolean isAssignableTo(TypeDescription typeDescription);

    @Override
    TypeDescription getComponentType();

    @Override
    TypeDescription getOwnerType();

    @Override
    TypeDescription getDeclaringType();

    /**
     * Returns a list of types that are declared by this type excluding anonymous classes.
     *
     * @return A list of types that are declared within this type.
     */
    TypeList getDeclaredTypes();

    /**
     * Returns a description of the enclosing method of this type.
     *
     * @return A description of the enclosing method of this type or {@code null} if there is no such method.
     */
    MethodDescription getEnclosingMethod();

    /**
     * Returns a description of the enclosing type of this type.
     *
     * @return A  description of the enclosing type of this type or {@code null} if there is no such type.
     */
    TypeDescription getEnclosingType();

    /**
     * <p>
     * Returns the type's actual modifiers as present in the class file. For example, a type cannot be {@code private}.
     * but it modifiers might reflect this property nevertheless if a class was defined as a private inner class.
     * </p>
     * <p>
     * Unfortunately, the modifier for marking a {@code static} class collides with the {@code SUPER} modifier such
     * that these flags are indistinguishable. Therefore, the flag must be specified manually.
     * </p>
     *
     * @param superFlag {@code true} if the super flag should be set.
     * @return The type's actual modifiers.
     */
    int getActualModifiers(boolean superFlag);

    /**
     * Returns the simple internalName of this type.
     *
     * @return The simple internalName of this type.
     */
    String getSimpleName();

    /**
     * Returns the canonical internalName of this type.
     *
     * @return The canonical internalName of this type.
     */
    String getCanonicalName();

    /**
     * Checks if this type description represents an anonymous type.
     *
     * @return {@code true} if this type description represents an anonymous type.
     */
    boolean isAnonymousClass();

    /**
     * Checks if this type description represents a local type.
     *
     * @return {@code true} if this type description represents a local type.
     */
    boolean isLocalClass();

    /**
     * Checks if this type description represents a member type.
     *
     * @return {@code true} if this type description represents a member type.
     */
    boolean isMemberClass();

    /**
     * Returns the package internalName of the type described by this instance.
     *
     * @return The package internalName of the type described by this instance.
     */
    PackageDescription getPackage();

    /**
     * Returns the annotations that this type declares or inherits from super types.
     *
     * @return A list of all inherited annotations.
     */
    AnnotationList getInheritedAnnotations();

    /**
     * Checks if two types are defined in the same package.
     *
     * @param typeDescription The type of interest.
     * @return {@code true} if this type and the given type are in the same package.
     */
    boolean isSamePackage(TypeDescription typeDescription);

    /**
     * Checks if instances of this type can be stored in the constant pool of a class. Note that any primitive
     * type that is smaller than an {@code int} cannot be stored in the constant pool as those types are represented
     * as {@code int} values internally.
     *
     * @return {@code true} if instances of this type can be stored in the constant pool of a class.
     */
    boolean isConstantPool();

    /**
     * Checks if this type represents a wrapper type for a primitive type. The {@link java.lang.Void} type is
     * not considered to be a wrapper type.
     *
     * @return {@code true} if this type represents a wrapper type.
     */
    boolean isPrimitiveWrapper();

    /**
     * Checks if instances of this type can be returned from an annotation method.
     *
     * @return {@code true} if instances of this type can be returned from an annotation method.
     */
    boolean isAnnotationReturnType();

    /**
     * Checks if instances of this type can be used for describing an annotation value.
     *
     * @return {@code true} if instances of this type can be used for describing an annotation value.
     */
    boolean isAnnotationValue();

    /**
     * Checks if instances of this type can be used for describing the given annotation value.
     *
     * @param value The value that is supposed to describe the annotation value for this instance.
     * @return {@code true} if instances of this type can be used for describing the given annotation value..
     */
    boolean isAnnotationValue(Object value);

    /**
     * An abstract base implementation of a type description.
     */
    abstract class AbstractBase extends ModifierReviewable.AbstractBase implements TypeDescription {

        @Override
        public GenericTypeDescription getSuperType() {
            GenericTypeDescription superType = getDeclaredSuperType();
            return superType == null
                    ? TypeDescription.UNDEFINED
                    : superType.accept(RawTypeWrapper.INSTANCE);
        }

        /**
         * Returns the declared super type in the form it is declared in the class file.
         *
         * @return The declared super type.
         */
        protected abstract GenericTypeDescription getDeclaredSuperType();

        @Override
        public GenericTypeList getInterfaces() {
            return new GenericTypeList.ForDetachedTypes(getDeclaredInterfaces(), RawTypeWrapper.INSTANCE);
        }

        /**
         * Returns the declared interface types in the form they are declared in the class file.
         *
         * @return The declared super type.
         */
        protected abstract GenericTypeList getDeclaredInterfaces();

        /**
         * Checks if a specific type is assignable to another type where the source type must be a super
         * type of the target type.
         *
         * @param sourceType The source type to which another type is to be assigned to.
         * @param targetType The target type that is to be assigned to the source type.
         * @return {@code true} if the target type is assignable to the source type.
         */
        private static boolean isAssignable(TypeDescription sourceType, TypeDescription targetType) {
            // Means that '[sourceType] var = ([targetType]) val;' is a valid assignment. This is true, if:
            // (1) Both types are equal (implies primitive types.)
            if (sourceType.equals(targetType)) {
                return true;
            }
            // (3) For arrays, there are special assignment rules.
            if (targetType.isArray()) {
                return sourceType.isArray()
                        ? isAssignable(sourceType.getComponentType(), targetType.getComponentType())
                        : sourceType.represents(Object.class) || TypeDescription.ARRAY_INTERFACES.contains(sourceType);
            }
            // (2) Interfaces do not extend the Object type but are assignable to the Object type.
            if (sourceType.represents(Object.class)) {
                return !targetType.isPrimitive();
            }
            // (4) The sub type has a super type and this super type is assignable to the super type.
            GenericTypeDescription superType = targetType.getSuperType();
            if (superType != null && sourceType.isAssignableFrom(superType.asErasure())) {
                return true;
            }
            // (5) If the target type is an interface, any of this type's interfaces might be assignable to it.
            if (sourceType.isInterface()) {
                for (TypeDescription interfaceType : targetType.getInterfaces().asErasures()) {
                    if (sourceType.isAssignableFrom(interfaceType)) {
                        return true;
                    }
                }
            }
            // (6) None of these criteria are true, i.e. the types are not assignable.
            return false;
        }

        @Override
        public boolean isAssignableFrom(Class<?> type) {
            return isAssignableFrom(new ForLoadedType(type));
        }

        @Override
        public boolean isAssignableFrom(TypeDescription typeDescription) {
            return isAssignable(this, typeDescription);
        }

        @Override
        public boolean isAssignableTo(Class<?> type) {
            return isAssignableTo(new ForLoadedType(type));
        }

        @Override
        public boolean isAssignableTo(TypeDescription typeDescription) {
            return isAssignable(typeDescription, this);
        }

        @Override
        public Sort getSort() {
            return Sort.NON_GENERIC;
        }

        @Override
        public TypeDescription asErasure() {
            return this;
        }

        @Override
        public GenericTypeList getUpperBounds() {
            throw new IllegalStateException("A non-generic type does not imply upper type bounds: " + this);
        }

        @Override
        public GenericTypeList getLowerBounds() {
            throw new IllegalStateException("A non-generic type does not imply lower type bounds: " + this);
        }

        @Override
        public GenericTypeList getParameters() {
            return new GenericTypeList.Empty();
        }

        @Override
        public String getSymbol() {
            throw new IllegalStateException("A non-generic type does not imply a symbol: " + this);
        }

        @Override
        public TypeDescription getOwnerType() {
            MethodDescription enclosingMethod = getEnclosingMethod();
            return enclosingMethod == null
                    ? getEnclosingType()
                    : enclosingMethod.getDeclaringType().asErasure();
        }

        @Override
        public boolean isInstance(Object value) {
            return isAssignableFrom(value.getClass());
        }

        @Override
        public boolean isInstanceOrWrapper(Object value) {
            return isInstance(value)
                    || (represents(boolean.class) && value instanceof Boolean)
                    || (represents(byte.class) && value instanceof Byte)
                    || (represents(short.class) && value instanceof Short)
                    || (represents(char.class) && value instanceof Character)
                    || (represents(int.class) && value instanceof Integer)
                    || (represents(long.class) && value instanceof Long)
                    || (represents(float.class) && value instanceof Float)
                    || (represents(double.class) && value instanceof Double);
        }

        @Override
        public boolean isAnnotationValue(Object value) {
            if ((represents(Class.class) && value instanceof TypeDescription)
                    || (value instanceof AnnotationDescription && ((AnnotationDescription) value).getAnnotationType().equals(this))
                    || (value instanceof EnumerationDescription && ((EnumerationDescription) value).getEnumerationType().equals(this))
                    || (represents(String.class) && value instanceof String)
                    || (represents(boolean.class) && value instanceof Boolean)
                    || (represents(byte.class) && value instanceof Byte)
                    || (represents(short.class) && value instanceof Short)
                    || (represents(char.class) && value instanceof Character)
                    || (represents(int.class) && value instanceof Integer)
                    || (represents(long.class) && value instanceof Long)
                    || (represents(float.class) && value instanceof Float)
                    || (represents(double.class) && value instanceof Double)
                    || (represents(String[].class) && value instanceof String[])
                    || (represents(boolean[].class) && value instanceof boolean[])
                    || (represents(byte[].class) && value instanceof byte[])
                    || (represents(short[].class) && value instanceof short[])
                    || (represents(char[].class) && value instanceof char[])
                    || (represents(int[].class) && value instanceof int[])
                    || (represents(long[].class) && value instanceof long[])
                    || (represents(float[].class) && value instanceof float[])
                    || (represents(double[].class) && value instanceof double[])
                    || (represents(Class[].class) && value instanceof TypeDescription[])) {
                return true;
            } else if (isAssignableTo(Annotation[].class) && value instanceof AnnotationDescription[]) {
                for (AnnotationDescription annotationDescription : (AnnotationDescription[]) value) {
                    if (!annotationDescription.getAnnotationType().equals(getComponentType())) {
                        return false;
                    }
                }
                return true;
            } else if (isAssignableTo(Enum[].class) && value instanceof EnumerationDescription[]) {
                for (EnumerationDescription enumerationDescription : (EnumerationDescription[]) value) {
                    if (!enumerationDescription.getEnumerationType().equals(getComponentType())) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String getInternalName() {
            return getName().replace('.', '/');
        }

        @Override
        public int getActualModifiers(boolean superFlag) {
            int actualModifiers;
            if (isPrivate()) {
                actualModifiers = getModifiers() & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);
            } else if (isProtected()) {
                actualModifiers = getModifiers() & ~(Opcodes.ACC_PROTECTED | Opcodes.ACC_STATIC) | Opcodes.ACC_PUBLIC;
            } else {
                actualModifiers = getModifiers() & ~Opcodes.ACC_STATIC;
            }
            return superFlag ? (actualModifiers | Opcodes.ACC_SUPER) : actualModifiers;
        }

        @Override
        public String getGenericSignature() {
            try {
                SignatureWriter signatureWriter = new SignatureWriter();
                boolean generic = false;
                for (GenericTypeDescription typeVariable : getTypeVariables()) {
                    signatureWriter.visitFormalTypeParameter(typeVariable.getSymbol());
                    for (GenericTypeDescription upperBound : typeVariable.getUpperBounds()) {
                        upperBound.accept(new GenericTypeDescription.Visitor.ForSignatureVisitor(upperBound.asErasure().isInterface()
                                ? signatureWriter.visitInterfaceBound()
                                : signatureWriter.visitClassBound()));
                    }
                    generic = true;
                }
                GenericTypeDescription superType = getSuperType();
                // The object type itself is non generic and implicitly returns a non-generic signature
                if (superType == null) {
                    superType = TypeDescription.OBJECT;
                }
                superType.accept(new GenericTypeDescription.Visitor.ForSignatureVisitor(signatureWriter.visitSuperclass()));
                generic = generic || !superType.getSort().isNonGeneric();
                for (GenericTypeDescription interfaceType : getInterfaces()) {
                    interfaceType.accept(new GenericTypeDescription.Visitor.ForSignatureVisitor(signatureWriter.visitInterface()));
                    generic = generic || !interfaceType.getSort().isNonGeneric();
                }
                return generic
                        ? signatureWriter.toString()
                        : NON_GENERIC_SIGNATURE;
            } catch (GenericSignatureFormatError ignored) {
                return NON_GENERIC_SIGNATURE;
            }
        }

        @Override
        public TypeVariableSource getVariableSource() {
            return UNDEFINED;
        }

        @Override
        public boolean isSamePackage(TypeDescription typeDescription) {
            PackageDescription thisPackage = getPackage(), otherPackage = typeDescription.getPackage();
            return thisPackage == null || otherPackage == null
                    ? thisPackage == otherPackage
                    : thisPackage.equals(otherPackage);
        }

        @Override
        public boolean isVisibleTo(TypeDescription typeDescription) {
            return isPrimitive() || (isArray()
                    ? getComponentType().isVisibleTo(typeDescription)
                    : isPublic() || isProtected() || isSamePackage(typeDescription)/* || equals(typeDescription) */);
        }

        @Override
        public AnnotationList getInheritedAnnotations() {
            AnnotationList declaredAnnotations = getDeclaredAnnotations();
            if (getSuperType() == null) {
                return declaredAnnotations;
            } else {
                Set<TypeDescription> annotationTypes = new HashSet<TypeDescription>();
                for (AnnotationDescription annotationDescription : declaredAnnotations) {
                    annotationTypes.add(annotationDescription.getAnnotationType());
                }
                return new AnnotationList.Explicit(join(declaredAnnotations, getSuperType().asErasure().getInheritedAnnotations().inherited(annotationTypes)));
            }
        }

        @Override
        public String getSourceCodeName() {
            if (isArray()) {
                TypeDescription typeDescription = this;
                int dimensions = 0;
                do {
                    dimensions++;
                    typeDescription = typeDescription.getComponentType();
                } while (typeDescription.isArray());
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(typeDescription.getSourceCodeName());
                for (int i = 0; i < dimensions; i++) {
                    stringBuilder.append("[]");
                }
                return stringBuilder.toString();
            } else {
                return getName();
            }
        }

        @Override
        public boolean isConstantPool() {
            return represents(int.class)
                    || represents(long.class)
                    || represents(float.class)
                    || represents(double.class)
                    || represents(String.class)
                    || represents(Class.class)
                    || JavaType.METHOD_HANDLE.getTypeStub().equals(this)
                    || JavaType.METHOD_TYPE.getTypeStub().equals(this);
        }

        @Override
        public boolean isPrimitiveWrapper() {
            return represents(Boolean.class)
                    || represents(Byte.class)
                    || represents(Short.class)
                    || represents(Character.class)
                    || represents(Integer.class)
                    || represents(Long.class)
                    || represents(Float.class)
                    || represents(Double.class);
        }

        @Override
        public boolean isAnnotationReturnType() {
            return isPrimitive()
                    || represents(String.class)
                    || (isAssignableTo(Enum.class) && !represents(Enum.class))
                    || (isAssignableTo(Annotation.class) && !represents(Annotation.class))
                    || represents(Class.class)
                    || (isArray() && !getComponentType().isArray() && getComponentType().isAnnotationReturnType());
        }

        @Override
        public boolean isAnnotationValue() {
            return isPrimitive()
                    || represents(String.class)
                    || isAssignableTo(TypeDescription.class)
                    || isAssignableTo(AnnotationDescription.class)
                    || isAssignableTo(EnumerationDescription.class)
                    || (isArray() && !getComponentType().isArray() && getComponentType().isAnnotationValue());
        }

        @Override
        public boolean represents(java.lang.reflect.Type type) {
            return equals(Sort.describe(type));
        }

        @Override
        public String getTypeName() {
            return getName();
        }

        @Override
        public TypeVariableSource getEnclosingSource() {
            MethodDescription enclosingMethod = getEnclosingMethod();
            return enclosingMethod == null
                    ? getEnclosingType()
                    : enclosingMethod;
        }

        @Override
        public GenericTypeDescription findVariable(String symbol) {
            GenericTypeList typeVariables = getTypeVariables().filter(named(symbol));
            if (typeVariables.isEmpty()) {
                TypeVariableSource enclosingSource = getEnclosingSource();
                return enclosingSource == null
                        ? UNDEFINED
                        : enclosingSource.findVariable(symbol);
            } else {
                return typeVariables.getOnly();
            }
        }

        @Override
        public <T> T accept(TypeVariableSource.Visitor<T> visitor) {
            return visitor.onType(this);
        }

        @Override
        public <T> T accept(GenericTypeDescription.Visitor<T> visitor) {
            return visitor.onNonGenericType(this);
        }

        @Override
        public Iterator<GenericTypeDescription> iterator() {
            return new SuperTypeIterator(this);
        }

        @Override
        public boolean equals(Object other) {
            return other == this || other instanceof GenericTypeDescription
                    && ((GenericTypeDescription) other).getSort().isNonGeneric()
                    && getInternalName().equals(((GenericTypeDescription) other).asErasure().getInternalName());
        }

        @Override
        public int hashCode() {
            return getInternalName().hashCode();
        }

        @Override
        public String toString() {
            return (isPrimitive() ? "" : (isInterface() ? "interface" : "class") + " ") + getName();
        }

        /**
         * A visitor that represents all {@link TypeDescription} instances as raw generic types.
         */
        protected enum RawTypeWrapper implements GenericTypeDescription.Visitor<GenericTypeDescription> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public GenericTypeDescription onGenericArray(GenericTypeDescription genericArray) {
                return ForGenericArray.Latent.of(genericArray.getComponentType().accept(this), 1);
            }

            @Override
            public GenericTypeDescription onWildcard(GenericTypeDescription wildcard) {
                // Wildcards which are used within parameterized types are taken care of by the calling method.
                GenericTypeList lowerBounds = wildcard.getLowerBounds();
                return lowerBounds.isEmpty()
                        ? GenericTypeDescription.ForWildcardType.Latent.boundedAbove(wildcard.getUpperBounds().getOnly().accept(this))
                        : GenericTypeDescription.ForWildcardType.Latent.boundedBelow(lowerBounds.getOnly().accept(this));
            }

            @Override
            public GenericTypeDescription onParameterizedType(GenericTypeDescription parameterizedType) {
                List<GenericTypeDescription> parameters = new ArrayList<GenericTypeDescription>(parameterizedType.getParameters().size());
                for (GenericTypeDescription parameter : parameterizedType.getParameters()) {
                    parameters.add(parameter.accept(this));
                }
                GenericTypeDescription ownerType = parameterizedType.getOwnerType();
                return new GenericTypeDescription.ForParameterizedType.Latent(parameterizedType.asErasure(),
                        parameters,
                        ownerType == null
                                ? TypeDescription.UNDEFINED
                                : ownerType.accept(this));
            }

            @Override
            public GenericTypeDescription onTypeVariable(GenericTypeDescription typeVariable) {
                return new RawTypeVariable(typeVariable);
            }

            @Override
            public GenericTypeDescription onNonGenericType(GenericTypeDescription typeDescription) {
                return new ForNonGenericType.Latent(typeDescription.asErasure());
            }

            @Override
            public String toString() {
                return "TypeDescription.AbstractBase.RawTypeWrapper." + name();
            }

            /**
             * An representation of a type variable with raw type bounds.
             */
            protected static class RawTypeVariable extends ForTypeVariable {

                /**
                 * The type variable in its declared form.
                 */
                private final GenericTypeDescription typeVariable;

                /**
                 * Creates a new raw type representation of a type variable.
                 *
                 * @param typeVariable The type variable in its declared form.
                 */
                protected RawTypeVariable(GenericTypeDescription typeVariable) {
                    this.typeVariable = typeVariable;
                }

                @Override
                public GenericTypeList getUpperBounds() {
                    return new GenericTypeList.ForDetachedTypes(typeVariable.getUpperBounds(), RawTypeWrapper.INSTANCE);
                }

                @Override
                public TypeVariableSource getVariableSource() {
                    return typeVariable.getVariableSource();
                }

                @Override
                public String getSymbol() {
                    return typeVariable.getSymbol();
                }
            }
        }

        /**
         * An adapter implementation of a {@link TypeDescription} that
         * describes any type that is not an array or a primitive type.
         */
        public abstract static class OfSimpleType extends TypeDescription.AbstractBase {

            @Override
            public boolean isPrimitive() {
                return false;
            }

            @Override
            public boolean isArray() {
                return false;
            }

            @Override
            public TypeDescription getComponentType() {
                return UNDEFINED;
            }

            @Override
            public String getDescriptor() {
                return "L" + getInternalName() + ";";
            }

            @Override
            public String getCanonicalName() {
                return getName().replace('$', '.');
            }

            @Override
            public String getSimpleName() {
                int simpleNameIndex = getInternalName().lastIndexOf('$');
                simpleNameIndex = simpleNameIndex == -1
                        ? getInternalName().lastIndexOf('/')
                        : simpleNameIndex;
                return simpleNameIndex == -1 ? getInternalName() : getInternalName().substring(simpleNameIndex + 1);
            }

            @Override
            public StackSize getStackSize() {
                return StackSize.SINGLE;
            }
        }
    }

    /**
     * A type description implementation that represents a loaded type.
     */
    class ForLoadedType extends AbstractBase {

        /**
         * The loaded type this instance represents.
         */
        private final Class<?> type;

        /**
         * Creates a new immutable type description for a loaded type.
         *
         * @param type The type to be represented by this type description.
         */
        public ForLoadedType(Class<?> type) {
            this.type = type;
        }

        @Override
        public boolean isAssignableFrom(Class<?> type) {
            // The JVM conducts more efficient assignability lookups of loaded types what is attempted first.
            return this.type.isAssignableFrom(type) || (this.type.getClassLoader() != type.getClassLoader() && super.isAssignableFrom(type));
        }

        @Override
        public boolean isAssignableTo(Class<?> type) {
            // The JVM conducts more efficient assignability lookups of loaded types what is attempted first.
            return type.isAssignableFrom(this.type) || (this.type.getClassLoader() != type.getClassLoader() && super.isAssignableTo(type));
        }

        @Override
        public boolean represents(java.lang.reflect.Type type) {
            // The JVM conducts more efficient assignability lookups of loaded types what is attempted first.
            return type == this.type || super.represents(type);
        }

        @Override
        public TypeDescription getComponentType() {
            Class<?> componentType = type.getComponentType();
            return componentType == null
                    ? TypeDescription.UNDEFINED
                    : new TypeDescription.ForLoadedType(componentType);
        }

        @Override
        public boolean isArray() {
            return type.isArray();
        }

        @Override
        public boolean isPrimitive() {
            return type.isPrimitive();
        }

        @Override
        public boolean isAnnotation() {
            return type.isAnnotation();
        }

        @Override
        public GenericTypeDescription getDeclaredSuperType() {
            return type.getSuperclass() == null
                    ? TypeDescription.UNDEFINED
                    : new LazyProjection.OfLoadedSuperType(type);
        }

        @Override
        public GenericTypeList getDeclaredInterfaces() {
            return isArray()
                    ? ARRAY_INTERFACES
                    : new GenericTypeList.OfLoadedInterfaceTypes(type);
        }

        @Override
        public TypeDescription getDeclaringType() {
            Class<?> declaringType = type.getDeclaringClass();
            return declaringType == null
                    ? TypeDescription.UNDEFINED
                    : new TypeDescription.ForLoadedType(declaringType);
        }

        @Override
        public MethodDescription getEnclosingMethod() {
            Method enclosingMethod = type.getEnclosingMethod();
            Constructor<?> enclosingConstructor = type.getEnclosingConstructor();
            if (enclosingMethod != null) {
                return new MethodDescription.ForLoadedMethod(enclosingMethod);
            } else if (enclosingConstructor != null) {
                return new MethodDescription.ForLoadedConstructor(enclosingConstructor);
            } else {
                return MethodDescription.UNDEFINED;
            }
        }

        @Override
        public TypeDescription getEnclosingType() {
            Class<?> enclosingType = type.getEnclosingClass();
            return enclosingType == null
                    ? TypeDescription.UNDEFINED
                    : new TypeDescription.ForLoadedType(enclosingType);
        }

        @Override
        public TypeList getDeclaredTypes() {
            return new TypeList.ForLoadedType(type.getDeclaredClasses());
        }

        @Override
        public String getSimpleName() {
            return type.getSimpleName();
        }

        @Override
        public boolean isAnonymousClass() {
            return type.isAnonymousClass();
        }

        @Override
        public boolean isLocalClass() {
            return type.isLocalClass();
        }

        @Override
        public boolean isMemberClass() {
            return type.isMemberClass();
        }

        @Override
        public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
            return new FieldList.ForLoadedField(type.getDeclaredFields());
        }

        @Override
        public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
            return new MethodList.ForLoadedType(type);
        }

        @Override
        public PackageDescription getPackage() {
            Package aPackage = type.getPackage();
            return aPackage == null
                    ? PackageDescription.UNDEFINED
                    : new PackageDescription.ForLoadedPackage(aPackage);
        }

        @Override
        public StackSize getStackSize() {
            return StackSize.of(type);
        }

        @Override
        public String getName() {
            String name = type.getName();
            int anonymousLoaderIndex = name.indexOf('/');
            return anonymousLoaderIndex == -1
                    ? name
                    : name.substring(0, anonymousLoaderIndex);
        }

        @Override
        public String getCanonicalName() {
            return type.getCanonicalName();
        }

        @Override
        public String getDescriptor() {
            return Type.getDescriptor(type);
        }

        @Override
        public int getModifiers() {
            return type.getModifiers();
        }

        @Override
        public GenericTypeList getTypeVariables() {
            return new GenericTypeList.ForLoadedType(type.getTypeParameters());
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.ForLoadedAnnotation(type.getDeclaredAnnotations());
        }
    }

    /**
     * A projection for an array type based on an existing {@link TypeDescription}.
     */
    class ArrayProjection extends AbstractBase {

        /**
         * The base component type which is itself not an array.
         */
        private final TypeDescription componentType;

        /**
         * The arity of this array.
         */
        private final int arity;

        /**
         * Crrates a new array projection.
         *
         * @param componentType The base component type of the array which is itself not an array.
         * @param arity         The arity of this array.
         */
        protected ArrayProjection(TypeDescription componentType, int arity) {
            this.componentType = componentType;
            this.arity = arity;
        }

        /**
         * Creates an array projection.
         *
         * @param componentType The component type of the array.
         * @param arity         The arity of this array.
         * @return A projection of the component type as an arity of the given value.
         */
        public static TypeDescription of(TypeDescription componentType, int arity) {
            if (arity < 0) {
                throw new IllegalArgumentException("Arrays cannot have a negative arity");
            }
            while (componentType.isArray()) {
                componentType = componentType.getComponentType();
                arity++;
            }
            return arity == 0
                    ? componentType
                    : new ArrayProjection(componentType, arity);
        }

        @Override
        public boolean isArray() {
            return true;
        }

        @Override
        public TypeDescription getComponentType() {
            return arity == 1
                    ? componentType
                    : new ArrayProjection(componentType, arity - 1);
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        protected GenericTypeDescription getDeclaredSuperType() {
            return TypeDescription.OBJECT;
        }

        @Override
        protected GenericTypeList getDeclaredInterfaces() {
            return ARRAY_INTERFACES;
        }

        @Override
        public MethodDescription getEnclosingMethod() {
            return MethodDescription.UNDEFINED;
        }

        @Override
        public TypeDescription getEnclosingType() {
            return UNDEFINED;
        }

        @Override
        public TypeList getDeclaredTypes() {
            return new TypeList.Empty();
        }

        @Override
        public String getSimpleName() {
            StringBuilder stringBuilder = new StringBuilder(componentType.getSimpleName());
            for (int i = 0; i < arity; i++) {
                stringBuilder.append("[]");
            }
            return stringBuilder.toString();
        }

        @Override
        public String getCanonicalName() {
            StringBuilder stringBuilder = new StringBuilder(componentType.getCanonicalName());
            for (int i = 0; i < arity; i++) {
                stringBuilder.append("[]");
            }
            return stringBuilder.toString();
        }

        @Override
        public boolean isAnonymousClass() {
            return false;
        }

        @Override
        public boolean isLocalClass() {
            return false;
        }

        @Override
        public boolean isMemberClass() {
            return false;
        }

        @Override
        public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
            return new FieldList.Empty();
        }

        @Override
        public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
            return new MethodList.Empty();
        }

        @Override
        public StackSize getStackSize() {
            return StackSize.SINGLE;
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Empty();
        }

        @Override
        public AnnotationList getInheritedAnnotations() {
            return new AnnotationList.Empty();
        }

        @Override
        public PackageDescription getPackage() {
            return PackageDescription.UNDEFINED;
        }

        @Override
        public String getName() {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < arity; i++) {
                stringBuilder.append('[');
            }
            return stringBuilder.append(componentType.getDescriptor().replace('/', '.')).toString();
        }

        @Override
        public String getDescriptor() {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < arity; i++) {
                stringBuilder.append('[');
            }
            return stringBuilder.append(componentType.getDescriptor()).toString();
        }

        @Override
        public TypeDescription getDeclaringType() {
            return UNDEFINED;
        }

        @Override
        public int getModifiers() {
            return ARRAY_MODIFIERS;
        }

        @Override
        public GenericTypeList getTypeVariables() {
            return new GenericTypeList.Empty();
        }
    }

    /**
     * A latent type description for a type without methods or fields.
     */
    class Latent extends AbstractBase.OfSimpleType {

        /**
         * The name of the type.
         */
        private final String name;

        /**
         * The modifiers of the type.
         */
        private final int modifiers;

        /**
         * The super type or {@code null} if no such type exists.
         */
        private final GenericTypeDescription superType;

        /**
         * The interfaces that this type implements.
         */
        private final List<? extends GenericTypeDescription> interfaces;

        /**
         * Creates a new latent type.
         *
         * @param name       The name of the type.
         * @param modifiers  The modifiers of the type.
         * @param superType  The super type or {@code null} if no such type exists.
         * @param interfaces The interfaces that this type implements.
         */
        public Latent(String name, int modifiers, GenericTypeDescription superType, List<? extends GenericTypeDescription> interfaces) {
            this.name = name;
            this.modifiers = modifiers;
            this.superType = superType;
            this.interfaces = interfaces;
        }

        @Override
        protected GenericTypeDescription getDeclaredSuperType() {
            return superType;
        }

        @Override
        protected GenericTypeList getDeclaredInterfaces() {
            return new GenericTypeList.Explicit(interfaces);
        }

        @Override
        public MethodDescription getEnclosingMethod() {
            throw new IllegalStateException("Cannot resolve enclosing method of a latent type description: " + this);
        }

        @Override
        public TypeDescription getEnclosingType() {
            throw new IllegalStateException("Cannot resolve enclosing type of a latent type description: " + this);
        }

        @Override
        public TypeList getDeclaredTypes() {
            throw new IllegalStateException("Cannot resolve inner types of a latent type description: " + this);
        }

        @Override
        public boolean isAnonymousClass() {
            throw new IllegalStateException("Cannot resolve anonymous type property of a latent type description: " + this);
        }

        @Override
        public boolean isLocalClass() {
            throw new IllegalStateException("Cannot resolve local class property of a latent type description: " + this);
        }

        @Override
        public boolean isMemberClass() {
            throw new IllegalStateException("Cannot resolve member class property of a latent type description: " + this);
        }

        @Override
        public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
            throw new IllegalStateException("Cannot resolve declared fields of a latent type description: " + this);
        }

        @Override
        public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
            throw new IllegalStateException("Cannot resolve declared methods of a latent type description: " + this);
        }

        @Override
        public PackageDescription getPackage() {
            String name = getName();
            int index = name.lastIndexOf('.');
            return index == -1
                    ? PackageDescription.UNDEFINED
                    : new PackageDescription.Simple(name.substring(0, index));
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            throw new IllegalStateException("Cannot resolve declared annotations of a latent type description: " + this);
        }

        @Override
        public TypeDescription getDeclaringType() {
            throw new IllegalStateException("Cannot resolve declared type of a latent type description: " + this);
        }

        @Override
        public int getModifiers() {
            return modifiers;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public GenericTypeList getTypeVariables() {
            throw new IllegalStateException("Cannot resolve type variables of a latent type description: " + this);
        }
    }

    /**
     * A type representation of a package description.
     */
    class ForPackageDescription extends AbstractBase.OfSimpleType {

        /**
         * The package to be described as a type.
         */
        private final PackageDescription packageDescription;

        /**
         * Creates a new type description of a package description.
         *
         * @param packageDescription The package to be described as a type.
         */
        public ForPackageDescription(PackageDescription packageDescription) {
            this.packageDescription = packageDescription;
        }

        @Override
        protected GenericTypeDescription getDeclaredSuperType() {
            return TypeDescription.OBJECT;
        }

        @Override
        protected GenericTypeList getDeclaredInterfaces() {
            return new GenericTypeList.Empty();
        }

        @Override
        public MethodDescription getEnclosingMethod() {
            return MethodDescription.UNDEFINED;
        }

        @Override
        public TypeDescription getEnclosingType() {
            return UNDEFINED;
        }

        @Override
        public boolean isAnonymousClass() {
            return false;
        }

        @Override
        public boolean isLocalClass() {
            return false;
        }

        @Override
        public boolean isMemberClass() {
            return false;
        }

        @Override
        public TypeList getDeclaredTypes() {
            return new TypeList.Empty();
        }

        @Override
        public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
            return new FieldList.Empty();
        }

        @Override
        public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
            return new MethodList.Empty();
        }

        @Override
        public PackageDescription getPackage() {
            return packageDescription;
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return packageDescription.getDeclaredAnnotations();
        }

        @Override
        public TypeDescription getDeclaringType() {
            return UNDEFINED;
        }

        @Override
        public GenericTypeList getTypeVariables() {
            return new GenericTypeList.Empty();
        }

        @Override
        public int getModifiers() {
            return PackageDescription.PACKAGE_MODIFIERS;
        }

        @Override
        public String getName() {
            return packageDescription.getName() + "." + PackageDescription.PACKAGE_CLASS_NAME;
        }
    }
}
