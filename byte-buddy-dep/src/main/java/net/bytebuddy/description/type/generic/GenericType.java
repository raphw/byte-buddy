package net.bytebuddy.description.type.generic;

import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaMethod;

import java.lang.reflect.*;
import java.util.Collections;
import java.util.List;

public interface GenericType extends NamedElement {

    Sort getSort();

    TypeDescription asRawType();

    GenericTypeList getUpperBounds();

    GenericTypeList getLowerBounds();

    GenericType getComponentType();

    GenericTypeList getParameters();

    TypeVariableSource getVariableSource();

    GenericType getOwnerType();

    String getSymbol();

    String getTypeName();

    enum Sort {

        RAW,
        GENERIC_ARRAY,
        PARAMETERIZED,
        WILDCARD,
        VARIABLE;

        public static GenericType describe(Type type) {
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

    abstract class ForGenericArray implements GenericType {

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
        public GenericType getOwnerType() {
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
        public boolean equals(Object other) {
            if (!(other instanceof GenericType)) return false;
            GenericType genericType = (GenericType) other;
            return genericType.getSort().isGenericArray() && getComponentType().equals(genericType.getComponentType());
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
            public GenericType getComponentType() {
                return Sort.describe(genericArrayType.getGenericComponentType());
            }
        }

        public static class Latent extends ForGenericArray {

            public static GenericType of(GenericType componentType, int arity) {
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

            private final GenericType componentType;

            private final int arity;

            protected Latent(GenericType componentType, int arity) {
                this.componentType = componentType;
                this.arity = arity;
            }

            @Override
            public GenericType getComponentType() {
                return arity == 0
                        ? componentType
                        : new Latent(componentType, arity - 1);
            }
        }
    }

    abstract class ForWildcardType implements GenericType {

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
        public GenericType getComponentType() {
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
        public GenericType getOwnerType() {
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
        public int hashCode() {
            int lowerHash = 1, upperHash = 1;
            for (GenericType genericType : getLowerBounds()) {
                lowerHash = 31 * lowerHash + genericType.hashCode();
            }
            for (GenericType genericType : getUpperBounds()) {
                upperHash = 31 * upperHash + genericType.hashCode();
            }
            return lowerHash ^ upperHash;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof GenericType)) return false;
            GenericType genericType = (GenericType) other;
            return genericType.getSort().isWildcard()
                    && getUpperBounds().equals(genericType.getUpperBounds())
                    && getLowerBounds().equals(genericType.getLowerBounds());
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
            for (GenericType genericType : bounds) {
                if (multiple) {
                    stringBuilder.append(" & ");
                }
                stringBuilder.append(genericType.getTypeName());
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

            public static GenericType unbounded() {
                return new Latent(Collections.<GenericType>emptyList(), Collections.<GenericType>emptyList());
            }

            public static GenericType boundedAbove(GenericType upperBound) {
                return new Latent(Collections.singletonList(upperBound), Collections.<GenericType>emptyList());
            }

            public static GenericType boundedBelow(GenericType lowerBound) {
                return new Latent(Collections.<GenericType>emptyList(), Collections.singletonList(lowerBound));
            }

            private final List<? extends GenericType> upperBounds;

            private final List<? extends GenericType> lowerBounds;

            protected Latent(List<? extends GenericType> upperBounds, List<? extends GenericType> lowerBounds) {
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

    abstract class ForParameterizedType implements GenericType {

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
        public GenericType getComponentType() {
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
        public int hashCode() {
            int result = 1;
            for (GenericType genericType : getLowerBounds()) {
                result = 31 * result + genericType.hashCode();
            }
            GenericType ownerType = getOwnerType();
            return result ^ (ownerType == null
                    ? asRawType().hashCode()
                    : ownerType.hashCode());
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof GenericType)) return false;
            GenericType genericType = (GenericType) other;
            if (!genericType.getSort().isParameterized()) return false;
            GenericType ownerType = getOwnerType(), otherOwnerType = genericType.getOwnerType();
            return asRawType().equals(genericType.asRawType())
                    && !(ownerType == null && otherOwnerType != null) && !(ownerType != null && !ownerType.equals(otherOwnerType))
                    && getParameters().equals(genericType.getParameters());

        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            GenericType ownerType = getOwnerType();
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
                for (GenericType genericType : actualTypeArguments) {
                    if (multiple) {
                        stringBuilder.append(", ");
                    }
                    stringBuilder.append(genericType.getTypeName());
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
            public GenericType getOwnerType() {
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

            private final List<? extends GenericType> parameters;

            private final GenericType ownerType;

            public Latent(TypeDescription rawType, List<? extends GenericType> parameters, GenericType ownerType) {
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
            public GenericType getOwnerType() {
                return ownerType;
            }
        }
    }

    abstract class ForTypeVariable implements GenericType {

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
        public GenericType getComponentType() {
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
        public GenericType getOwnerType() {
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
        public int hashCode() {
            return getVariableSource().hashCode() ^ getSymbol().hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof GenericType)) return false;
            GenericType genericType = (GenericType) other;
            return genericType.getSort().isTypeVariable()
                    && genericType.getSymbol().equals(genericType.getSymbol())
                    && genericType.getVariableSource().equals(genericType.getVariableSource());
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

            private final List<? extends GenericType> upperBounds;

            private final TypeVariableSource typeVariableSource;

            private final String symbol;

            public Latent(List<? extends GenericType> upperBounds, TypeVariableSource typeVariableSource, String symbol) {
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

    abstract class LazyProjection implements GenericType {

        protected abstract GenericType resolve();

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
        public GenericType getComponentType() {
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
        public GenericType getOwnerType() {
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
            protected GenericType resolve() {
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
            protected GenericType resolve() {
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
            protected GenericType resolve() {
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
            protected GenericType resolve() {
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
            protected GenericType resolve() {
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
            protected GenericType resolve() {
                return Sort.describe(method.getGenericParameterTypes()[index]);
            }

            @Override
            public TypeDescription asRawType() {
                return new TypeDescription.ForLoadedType(rawType);
            }
        }
    }
}
