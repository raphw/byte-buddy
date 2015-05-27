package net.bytebuddy.description.type.generic;

import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaMethod;
import org.objectweb.asm.signature.SignatureVisitor;

import java.lang.reflect.*;
import java.util.Collections;
import java.util.List;

public interface GenericTypeDescription extends NamedElement {

    Sort getSort();

    TypeDescription asRawType();

    GenericTypeList getUpperBounds();

    GenericTypeList getLowerBounds();

    GenericTypeDescription getComponentType();

    GenericTypeList getParameters();

    TypeVariableSource getVariableSource();

    GenericTypeDescription getOwnerType();

    String getSymbol();

    String getTypeName();

    void accept(Visitor visitor);

    enum Sort {

        RAW,
        GENERIC_ARRAY,
        PARAMETERIZED,
        WILDCARD,
        VARIABLE;

        public static GenericTypeDescription describe(Type type) {
            if (type instanceof Class<?>) {
                return new TypeDescription.ForLoadedType((Class<?>) type);
            } else if (type instanceof GenericArrayType) {
                return new ForGenericArray.OfLoadedType((GenericArrayType) type);
            } else if (type instanceof ParameterizedType) {
                return new ForParameterizedType.OfLoadedType((ParameterizedType) type);
            } else if (type instanceof TypeVariable) {
                return new ForTypeVariable.OfLoadedType((TypeVariable<?>) type);
            } else if (type instanceof WildcardType) {
                return new ForWildcardType.OfLoadedType((WildcardType) type);
            } else {
                throw new IllegalStateException("Unknown type: " + type);
            }
        }

        public boolean isRawType() {
            return this == RAW;
        }

        public boolean isParameterized() {
            return this == PARAMETERIZED;
        }

        public boolean isGenericArray() {
            return this == GENERIC_ARRAY;
        }

        public boolean isWildcard() {
            return this == WILDCARD;
        }

        public boolean isTypeVariable() {
            return this == VARIABLE;
        }
    }

    interface Visitor {

        void onGenericArray(GenericTypeDescription genericTypeDescription);

        void onWildcardType(GenericTypeDescription genericTypeDescription);

        void onParameterizedType(GenericTypeDescription genericTypeDescription);

        void onTypeVariable(GenericTypeDescription genericTypeDescription);

        void onRawType(TypeDescription typeDescription);

        class ForSignatureVisitor implements Visitor {

            private final SignatureVisitor signatureVisitor;

            public ForSignatureVisitor(SignatureVisitor signatureVisitor) {
                this.signatureVisitor = signatureVisitor;
            }

            @Override
            public void onGenericArray(GenericTypeDescription genericTypeDescription) {
                genericTypeDescription.getComponentType().accept(new ForSignatureVisitor(signatureVisitor.visitArrayType()));
            }

            @Override
            public void onWildcardType(GenericTypeDescription genericTypeDescription) {
                GenericTypeList upperBounds = genericTypeDescription.getUpperBounds();
                if (upperBounds.isEmpty()) {
                    GenericTypeList lowerBounds = genericTypeDescription.getUpperBounds();
                    if (lowerBounds.isEmpty()) {
                        signatureVisitor.visitTypeArgument();
                    } else {
                        lowerBounds.getOnly().accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument('-')));
                        signatureVisitor.visitEnd();
                    }
                } else {
                    upperBounds.getOnly().accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument('+')));
                    signatureVisitor.visitEnd();
                }
            }

            @Override
            public void onParameterizedType(GenericTypeDescription genericTypeDescription) {
                GenericTypeDescription ownerType = genericTypeDescription.getOwnerType();
                if (ownerType != null) {
                    signatureVisitor.visitInnerClassType(ownerType.asRawType().getInternalName());
                } else {
                    signatureVisitor.visitClassType(genericTypeDescription.asRawType().getInternalName());
                }
                for (GenericTypeDescription upperBound : genericTypeDescription.getUpperBounds()) {
                    upperBound.accept(this);
                }
                signatureVisitor.visitEnd();
            }

            @Override
            public void onTypeVariable(GenericTypeDescription genericTypeDescription) {
                signatureVisitor.visitTypeVariable(genericTypeDescription.getSymbol());
            }

            @Override
            public void onRawType(TypeDescription typeDescription) {
                if (typeDescription.isPrimitive()) {
                    signatureVisitor.visitBaseType(typeDescription.getDescriptor().charAt(0));
                } else {
                    signatureVisitor.visitClassType(typeDescription.getInternalName());
                }
            }
        }
    }

    abstract class ForGenericArray implements GenericTypeDescription {

        @Override
        public Sort getSort() {
            return Sort.GENERIC_ARRAY;
        }

        @Override
        public TypeDescription asRawType() {
            return TypeDescription.ArrayProjection.of(getComponentType().asRawType(), 1);
        }

        @Override
        public GenericTypeList getUpperBounds() {
            return new GenericTypeList.Empty();
        }

        @Override
        public GenericTypeList getLowerBounds() {
            return new GenericTypeList.Empty();
        }

        @Override
        public TypeVariableSource getVariableSource() {
            return null;
        }

        @Override
        public GenericTypeList getParameters() {
            return new GenericTypeList.Empty();
        }

        @Override
        public GenericTypeDescription getOwnerType() {
            return null;
        }

        @Override
        public String getSymbol() {
            return null;
        }

        @Override
        public String getTypeName() {
            return toString();
        }

        @Override
        public String getSourceCodeName() {
            return toString();
        }

        @Override
        public void accept(Visitor visitor) {
            visitor.onGenericArray(this);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof GenericTypeDescription)) return false;
            GenericTypeDescription genericTypeDescription = (GenericTypeDescription) other;
            return genericTypeDescription.getSort().isGenericArray() && getComponentType().equals(genericTypeDescription.getComponentType());
        }

        @Override
        public int hashCode() {
            return getComponentType().hashCode();
        }

        @Override
        public String toString() {
            return getComponentType().getTypeName() + "[]";
        }

        public static class OfLoadedType extends ForGenericArray {

            private final GenericArrayType genericArrayType;

            public OfLoadedType(GenericArrayType genericArrayType) {
                this.genericArrayType = genericArrayType;
            }

            @Override
            public GenericTypeDescription getComponentType() {
                return Sort.describe(genericArrayType.getGenericComponentType());
            }
        }

        public static class Latent extends ForGenericArray {

            public static GenericTypeDescription of(GenericTypeDescription componentType, int arity) {
                if (arity < 0) {
                    throw new IllegalArgumentException("Arity cannot be negative");
                }
                while (componentType.getSort().isGenericArray()) {
                    arity++;
                    componentType = componentType.getComponentType();
                }
                return arity == 0
                        ? componentType
                        : new Latent(componentType, arity);
            }

            private final GenericTypeDescription componentType;

            private final int arity;

            protected Latent(GenericTypeDescription componentType, int arity) {
                this.componentType = componentType;
                this.arity = arity;
            }

            @Override
            public GenericTypeDescription getComponentType() {
                return arity == 0
                        ? componentType
                        : new Latent(componentType, arity - 1);
            }
        }
    }

    abstract class ForWildcardType implements GenericTypeDescription {

        public static final String SYMBOL = "?";

        @Override
        public Sort getSort() {
            return Sort.WILDCARD;
        }

        @Override
        public TypeDescription asRawType() {
            GenericTypeList upperBounds = getUpperBounds();
            return upperBounds.isEmpty()
                    ? TypeDescription.OBJECT
                    : upperBounds.get(0).asRawType();
        }

        @Override
        public GenericTypeDescription getComponentType() {
            return null;
        }

        @Override
        public TypeVariableSource getVariableSource() {
            return null;
        }

        @Override
        public GenericTypeList getParameters() {
            return new GenericTypeList.Empty();
        }

        @Override
        public GenericTypeDescription getOwnerType() {
            return null;
        }

        @Override
        public String getSymbol() {
            return null;
        }

        @Override
        public String getTypeName() {
            return toString();
        }

        @Override
        public String getSourceCodeName() {
            return toString();
        }

        @Override
        public void accept(Visitor visitor) {
            visitor.onWildcardType(this);
        }

        @Override
        public int hashCode() {
            int lowerHash = 1, upperHash = 1;
            for (GenericTypeDescription genericTypeDescription : getLowerBounds()) {
                lowerHash = 31 * lowerHash + genericTypeDescription.hashCode();
            }
            for (GenericTypeDescription genericTypeDescription : getUpperBounds()) {
                upperHash = 31 * upperHash + genericTypeDescription.hashCode();
            }
            return lowerHash ^ upperHash;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof GenericTypeDescription)) return false;
            GenericTypeDescription genericTypeDescription = (GenericTypeDescription) other;
            return genericTypeDescription.getSort().isWildcard()
                    && getUpperBounds().equals(genericTypeDescription.getUpperBounds())
                    && getLowerBounds().equals(genericTypeDescription.getLowerBounds());
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder(SYMBOL);
            GenericTypeList bounds = getUpperBounds();
            if (!bounds.isEmpty()) {
                if (bounds.size() == 1 && bounds.get(0).equals(TypeDescription.OBJECT)) {
                    return SYMBOL;
                }
                stringBuilder.append(" extends ");
            } else {
                bounds = getLowerBounds();
                if (bounds.isEmpty()) {
                    return SYMBOL;
                }
                stringBuilder.append(" super ");
            }
            boolean multiple = false;
            for (GenericTypeDescription genericTypeDescription : bounds) {
                if (multiple) {
                    stringBuilder.append(" & ");
                }
                stringBuilder.append(genericTypeDescription.getTypeName());
                multiple = true;
            }
            return stringBuilder.toString();
        }

        public static class OfLoadedType extends ForWildcardType {

            private final WildcardType wildcardType;

            public OfLoadedType(WildcardType wildcardType) {
                this.wildcardType = wildcardType;
            }

            @Override
            public GenericTypeList getLowerBounds() {
                return new GenericTypeList.ForLoadedType(wildcardType.getLowerBounds());
            }

            @Override
            public GenericTypeList getUpperBounds() {
                return new GenericTypeList.ForLoadedType(wildcardType.getUpperBounds());
            }
        }

        public static class Latent extends ForWildcardType {

            public static GenericTypeDescription unbounded() {
                return new Latent(Collections.<GenericTypeDescription>emptyList(), Collections.<GenericTypeDescription>emptyList());
            }

            public static GenericTypeDescription boundedAbove(GenericTypeDescription upperBound) {
                return new Latent(Collections.singletonList(upperBound), Collections.<GenericTypeDescription>emptyList());
            }

            public static GenericTypeDescription boundedBelow(GenericTypeDescription lowerBound) {
                return new Latent(Collections.<GenericTypeDescription>emptyList(), Collections.singletonList(lowerBound));
            }

            private final List<? extends GenericTypeDescription> upperBounds;

            private final List<? extends GenericTypeDescription> lowerBounds;

            protected Latent(List<? extends GenericTypeDescription> upperBounds, List<? extends GenericTypeDescription> lowerBounds) {
                this.upperBounds = upperBounds;
                this.lowerBounds = lowerBounds;
            }

            @Override
            public GenericTypeList getUpperBounds() {
                return new GenericTypeList.Explicit(upperBounds);
            }

            @Override
            public GenericTypeList getLowerBounds() {
                return new GenericTypeList.Explicit(lowerBounds);
            }
        }
    }

    abstract class ForParameterizedType implements GenericTypeDescription {

        @Override
        public Sort getSort() {
            return Sort.PARAMETERIZED;
        }

        @Override
        public GenericTypeList getUpperBounds() {
            return new GenericTypeList.Empty();
        }

        @Override
        public GenericTypeList getLowerBounds() {
            return new GenericTypeList.Empty();
        }

        @Override
        public GenericTypeDescription getComponentType() {
            return null;
        }

        @Override
        public TypeVariableSource getVariableSource() {
            return null;
        }

        @Override
        public String getTypeName() {
            return toString();
        }

        @Override
        public String getSymbol() {
            return null;
        }

        @Override
        public String getSourceCodeName() {
            return toString();
        }

        @Override
        public void accept(Visitor visitor) {
            visitor.onParameterizedType(this);
        }

        @Override
        public int hashCode() {
            int result = 1;
            for (GenericTypeDescription genericTypeDescription : getLowerBounds()) {
                result = 31 * result + genericTypeDescription.hashCode();
            }
            GenericTypeDescription ownerType = getOwnerType();
            return result ^ (ownerType == null
                    ? asRawType().hashCode()
                    : ownerType.hashCode());
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof GenericTypeDescription)) return false;
            GenericTypeDescription genericTypeDescription = (GenericTypeDescription) other;
            if (!genericTypeDescription.getSort().isParameterized()) return false;
            GenericTypeDescription ownerType = getOwnerType(), otherOwnerType = genericTypeDescription.getOwnerType();
            return asRawType().equals(genericTypeDescription.asRawType())
                    && !(ownerType == null && otherOwnerType != null) && !(ownerType != null && !ownerType.equals(otherOwnerType))
                    && getParameters().equals(genericTypeDescription.getParameters());

        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            GenericTypeDescription ownerType = getOwnerType();
            if (ownerType != null) {
                stringBuilder.append(ownerType.getTypeName());
                stringBuilder.append(".");
                stringBuilder.append(ownerType.getSort().isParameterized()
                        ? asRawType().getName().replace(ownerType.asRawType().getName() + "$", "")
                        : asRawType().getName());
            } else {
                stringBuilder.append(asRawType().getName());
            }
            GenericTypeList actualTypeArguments = getParameters();
            if (!actualTypeArguments.isEmpty()) {
                stringBuilder.append("<");
                boolean multiple = false;
                for (GenericTypeDescription genericTypeDescription : actualTypeArguments) {
                    if (multiple) {
                        stringBuilder.append(", ");
                    }
                    stringBuilder.append(genericTypeDescription.getTypeName());
                    multiple = true;
                }
                stringBuilder.append(">");
            }
            return stringBuilder.toString();
        }

        public static class OfLoadedType extends ForParameterizedType {

            private final ParameterizedType parameterizedType;

            public OfLoadedType(ParameterizedType parameterizedType) {
                this.parameterizedType = parameterizedType;
            }

            @Override
            public GenericTypeList getParameters() {
                return new GenericTypeList.ForLoadedType(parameterizedType.getActualTypeArguments());
            }

            @Override
            public GenericTypeDescription getOwnerType() {
                Type ownerType = parameterizedType.getOwnerType();
                return ownerType == null
                        ? null
                        : Sort.describe(ownerType);
            }

            @Override
            public TypeDescription asRawType() {
                return new TypeDescription.ForLoadedType((Class<?>) parameterizedType.getRawType());
            }
        }

        public static class Latent extends ForParameterizedType {

            private final TypeDescription rawType;

            private final List<? extends GenericTypeDescription> parameters;

            private final GenericTypeDescription ownerType;

            public Latent(TypeDescription rawType, List<? extends GenericTypeDescription> parameters, GenericTypeDescription ownerType) {
                this.rawType = rawType;
                this.parameters = parameters;
                this.ownerType = ownerType;
            }

            @Override
            public TypeDescription asRawType() {
                return rawType;
            }

            @Override
            public GenericTypeList getParameters() {
                return new GenericTypeList.Explicit(parameters);
            }

            @Override
            public GenericTypeDescription getOwnerType() {
                return ownerType;
            }
        }
    }

    abstract class ForTypeVariable implements GenericTypeDescription {

        @Override
        public Sort getSort() {
            return Sort.VARIABLE;
        }

        @Override
        public TypeDescription asRawType() {
            GenericTypeList upperBounds = getUpperBounds();
            return upperBounds.isEmpty()
                    ? TypeDescription.OBJECT
                    : upperBounds.get(0).asRawType();
        }

        @Override
        public GenericTypeDescription getComponentType() {
            return null;
        }

        @Override
        public GenericTypeList getParameters() {
            return new GenericTypeList.Empty();
        }

        @Override
        public GenericTypeList getLowerBounds() {
            return new GenericTypeList.Empty();
        }

        @Override
        public GenericTypeDescription getOwnerType() {
            return null;
        }

        @Override
        public String getTypeName() {
            return toString();
        }

        @Override
        public String getSourceCodeName() {
            return getSymbol();
        }

        @Override
        public void accept(Visitor visitor) {
            visitor.onTypeVariable(this);
        }

        @Override
        public int hashCode() {
            return getVariableSource().hashCode() ^ getSymbol().hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof GenericTypeDescription)) return false;
            GenericTypeDescription genericTypeDescription = (GenericTypeDescription) other;
            return genericTypeDescription.getSort().isTypeVariable()
                    && genericTypeDescription.getSymbol().equals(genericTypeDescription.getSymbol())
                    && genericTypeDescription.getVariableSource().equals(genericTypeDescription.getVariableSource());
        }

        @Override
        public String toString() {
            return getSymbol();
        }

        public static class OfLoadedType extends ForTypeVariable {

            private final TypeVariable<?> typeVariable;

            public OfLoadedType(TypeVariable<?> typeVariable) {
                this.typeVariable = typeVariable;
            }

            @Override
            public TypeVariableSource getVariableSource() {
                GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();
                if (genericDeclaration instanceof Class) {
                    return new TypeDescription.ForLoadedType((Class<?>) genericDeclaration);
                } else if (genericDeclaration instanceof Method) {
                    return new MethodDescription.ForLoadedMethod((Method) genericDeclaration);
                } else if (genericDeclaration instanceof Constructor) {
                    return new MethodDescription.ForLoadedConstructor((Constructor<?>) genericDeclaration);
                } else {
                    throw new IllegalStateException("Unknown declaration: " + genericDeclaration);
                }
            }

            @Override
            public GenericTypeList getUpperBounds() {
                return new GenericTypeList.ForLoadedType(typeVariable.getBounds());
            }

            @Override
            public String getSymbol() {
                return typeVariable.getName();
            }
        }

        public static class Latent extends ForTypeVariable {

            private final List<? extends GenericTypeDescription> upperBounds;

            private final TypeVariableSource typeVariableSource;

            private final String symbol;

            public Latent(List<? extends GenericTypeDescription> upperBounds, TypeVariableSource typeVariableSource, String symbol) {
                this.upperBounds = upperBounds;
                this.typeVariableSource = typeVariableSource;
                this.symbol = symbol;
            }

            @Override
            public GenericTypeList getUpperBounds() {
                return new GenericTypeList.Explicit(upperBounds);
            }

            @Override
            public TypeVariableSource getVariableSource() {
                return typeVariableSource;
            }

            @Override
            public String getSymbol() {
                return symbol;
            }
        }
    }

    abstract class LazyProjection implements GenericTypeDescription {

        protected abstract GenericTypeDescription resolve();

        @Override
        public Sort getSort() {
            return resolve().getSort();
        }

        @Override
        public GenericTypeList getUpperBounds() {
            return resolve().getUpperBounds();
        }

        @Override
        public GenericTypeList getLowerBounds() {
            return resolve().getLowerBounds();
        }

        @Override
        public GenericTypeDescription getComponentType() {
            return resolve().getComponentType();
        }

        @Override
        public GenericTypeList getParameters() {
            return resolve().getParameters();
        }

        @Override
        public TypeVariableSource getVariableSource() {
            return resolve().getVariableSource();
        }

        @Override
        public GenericTypeDescription getOwnerType() {
            return resolve().getOwnerType();
        }

        @Override
        public String getTypeName() {
            return resolve().getTypeName();
        }

        @Override
        public String getSymbol() {
            return resolve().getSymbol();
        }

        @Override
        public String getSourceCodeName() {
            return resolve().getSourceCodeName();
        }

        @Override
        public void accept(Visitor visitor) {
            resolve().accept(visitor);
        }

        @Override
        public int hashCode() {
            return resolve().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return resolve().equals(obj);
        }

        @Override
        public String toString() {
            return resolve().toString();
        }

        public static class OfLoadedSuperType extends LazyProjection {

            private final Class<?> type;

            public OfLoadedSuperType(Class<?> type) {
                this.type = type;
            }

            @Override
            protected GenericTypeDescription resolve() {
                return Sort.describe(type.getGenericSuperclass());
            }

            @Override
            public TypeDescription asRawType() {
                return new TypeDescription.ForLoadedType(type.getSuperclass());
            }
        }

        public static class OfLoadedReturnType extends LazyProjection {

            private final Method method;

            public OfLoadedReturnType(Method method) {
                this.method = method;
            }

            @Override
            protected GenericTypeDescription resolve() {
                return Sort.describe(method.getGenericReturnType());
            }

            @Override
            public TypeDescription asRawType() {
                return new TypeDescription.ForLoadedType(method.getReturnType());
            }
        }

        public static class OfLoadedFieldType extends LazyProjection {

            private final Field field;

            public OfLoadedFieldType(Field field) {
                this.field = field;
            }

            @Override
            protected GenericTypeDescription resolve() {
                return Sort.describe(field.getGenericType());
            }

            @Override
            public TypeDescription asRawType() {
                return new TypeDescription.ForLoadedType(field.getType());
            }
        }

        public static class OfLoadedParameter extends LazyProjection {

            protected static final JavaMethod GET_TYPE;

            protected static final JavaMethod GET_GENERIC_TYPE;

            static {
                JavaMethod getType, getGenericType;
                try {
                    Class<?> parameterType = Class.forName("java.lang.reflect.Parameter");
                    getType = new JavaMethod.ForLoadedMethod(parameterType.getDeclaredMethod("getType"));
                    getGenericType = new JavaMethod.ForLoadedMethod(parameterType.getDeclaredMethod("getParameterizedType"));
                } catch (Exception ignored) {
                    getType = JavaMethod.ForUnavailableMethod.INSTANCE;
                    getGenericType = JavaMethod.ForUnavailableMethod.INSTANCE;
                }
                GET_TYPE = getType;
                GET_GENERIC_TYPE = getGenericType;
            }

            private final Object parameter;

            public OfLoadedParameter(Object parameter) {
                this.parameter = parameter;
            }

            @Override
            protected GenericTypeDescription resolve() {
                return Sort.describe((Type) GET_GENERIC_TYPE.invoke(parameter));
            }

            @Override
            public TypeDescription asRawType() {
                return new TypeDescription.ForLoadedType((Class<?>) GET_TYPE.invoke(parameter));
            }
        }

        public static class OfLegacyVmConstructorParameter extends LazyProjection {

            private final Constructor<?> constructor;

            private final int index;

            private final Class<?> rawType;

            public OfLegacyVmConstructorParameter(Constructor<?> constructor, int index, Class<?> rawType) {
                this.constructor = constructor;
                this.index = index;
                this.rawType = rawType;
            }

            @Override
            protected GenericTypeDescription resolve() {
                return Sort.describe(constructor.getGenericParameterTypes()[index]);
            }

            @Override
            public TypeDescription asRawType() {
                return new TypeDescription.ForLoadedType(rawType);
            }
        }

        public static class OfLegacyVmMethodParameter extends LazyProjection {

            private final Method method;

            private final int index;

            private final Class<?> rawType;

            public OfLegacyVmMethodParameter(Method method, int index, Class<?> rawType) {
                this.method = method;
                this.index = index;
                this.rawType = rawType;
            }

            @Override
            protected GenericTypeDescription resolve() {
                return Sort.describe(method.getGenericParameterTypes()[index]);
            }

            @Override
            public TypeDescription asRawType() {
                return new TypeDescription.ForLoadedType(rawType);
            }
        }
    }
}
