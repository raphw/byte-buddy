package net.bytebuddy.description.type.generic;

import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaMethod;
import org.objectweb.asm.signature.SignatureVisitor;

import java.lang.reflect.*;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

public interface GenericTypeDescription extends NamedElement {

    Sort getSort();

    String getTypeName();

    TypeDescription asRawType();

    GenericTypeDescription getSuperType();

    GenericTypeList getInterfaces();

    FieldList getDeclaredFields();

    MethodList getDeclaredMethods();

    GenericTypeList getUpperBounds();

    GenericTypeList getLowerBounds();

    GenericTypeDescription getComponentType();

    GenericTypeList getParameters();

    TypeVariableSource getVariableSource();

    GenericTypeDescription getOwnerType();

    String getSymbol();

    /**
     * Returns the size of the type described by this instance.
     *
     * @return The size of the type described by this instance.
     */
    StackSize getStackSize();

    <T> T accept(Visitor<T> visitor);

    enum Sort {

        RAW,
        GENERIC_ARRAY,
        PARAMETERIZED,
        WILDCARD,
        VARIABLE,
        DETACHED_VARIABLE;

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

        public boolean isDetachedTypeVariable() {
            return this == DETACHED_VARIABLE;
        }
    }

    interface Visitor<T> {

        T onGenericArray(GenericTypeDescription genericTypeDescription);

        T onWildcardType(GenericTypeDescription genericTypeDescription);

        T onParameterizedType(GenericTypeDescription genericTypeDescription);

        T onTypeVariable(GenericTypeDescription genericTypeDescription);

        T onRawType(TypeDescription typeDescription);

        class ForSignatureVisitor implements Visitor<SignatureVisitor> {

            private static final int ONLY_CHARACTER = 0;

            protected final SignatureVisitor signatureVisitor;

            public ForSignatureVisitor(SignatureVisitor signatureVisitor) {
                this.signatureVisitor = signatureVisitor;
            }

            @Override
            public SignatureVisitor onGenericArray(GenericTypeDescription genericTypeDescription) {
                genericTypeDescription.getComponentType().accept(new ForSignatureVisitor(signatureVisitor.visitArrayType()));
                return signatureVisitor;
            }

            @Override
            public SignatureVisitor onWildcardType(GenericTypeDescription genericTypeDescription) {
                throw new IllegalStateException("Unexpected wildcard: " + genericTypeDescription);
            }

            @Override
            public SignatureVisitor onParameterizedType(GenericTypeDescription genericTypeDescription) {
                onOwnableType(genericTypeDescription);
                signatureVisitor.visitEnd();
                return signatureVisitor;
            }

            private void onOwnableType(GenericTypeDescription genericTypeDescription) {
                GenericTypeDescription ownerType = genericTypeDescription.getOwnerType();
                if (ownerType != null) {
                    onOwnableType(ownerType);
                    signatureVisitor.visitInnerClassType(genericTypeDescription.asRawType().getSimpleName());
                } else {
                    signatureVisitor.visitClassType(genericTypeDescription.asRawType().getInternalName());
                }
                for (GenericTypeDescription upperBound : genericTypeDescription.getParameters()) {
                    upperBound.accept(new OfParameter(signatureVisitor));
                }
            }

            @Override
            public SignatureVisitor onTypeVariable(GenericTypeDescription genericTypeDescription) {
                signatureVisitor.visitTypeVariable(genericTypeDescription.getSymbol());
                return signatureVisitor;
            }

            @Override
            public SignatureVisitor onRawType(TypeDescription typeDescription) {
                if (typeDescription.isPrimitive()) {
                    signatureVisitor.visitBaseType(typeDescription.getDescriptor().charAt(ONLY_CHARACTER));
                } else {
                    signatureVisitor.visitClassType(typeDescription.getInternalName());
                    signatureVisitor.visitEnd();
                }
                return signatureVisitor;
            }

            protected static class OfParameter extends ForSignatureVisitor {

                protected OfParameter(SignatureVisitor signatureVisitor) {
                    super(signatureVisitor);
                }

                @Override
                public SignatureVisitor onWildcardType(GenericTypeDescription genericTypeDescription) {
                    GenericTypeList upperBounds = genericTypeDescription.getUpperBounds();
                    GenericTypeList lowerBounds = genericTypeDescription.getLowerBounds();
                    if (lowerBounds.isEmpty() && upperBounds.getOnly().asRawType().represents(Object.class)) {
                        signatureVisitor.visitTypeArgument();
                    } else if (!lowerBounds.isEmpty() /* && upperBounds.isEmpty() */) {
                        lowerBounds.getOnly().accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.EXTENDS)));
                    } else /* if (!upperBounds.isEmpty() && lowerBounds.isEmpty()) */ {
                        upperBounds.getOnly().accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.SUPER)));
                    }
                    return signatureVisitor;
                }

                @Override
                public SignatureVisitor onGenericArray(GenericTypeDescription genericTypeDescription) {
                    genericTypeDescription.accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.INSTANCEOF)));
                    return signatureVisitor;
                }

                @Override
                public SignatureVisitor onParameterizedType(GenericTypeDescription genericTypeDescription) {
                    genericTypeDescription.accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.INSTANCEOF)));
                    return signatureVisitor;
                }

                @Override
                public SignatureVisitor onTypeVariable(GenericTypeDescription genericTypeDescription) {
                    genericTypeDescription.accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.INSTANCEOF)));
                    return signatureVisitor;
                }

                @Override
                public SignatureVisitor onRawType(TypeDescription typeDescription) {
                    typeDescription.accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.INSTANCEOF)));
                    return signatureVisitor;
                }
            }
        }

        enum ForErasure implements Visitor<TypeDescription> {

            INSTANCE;

            @Override
            public TypeDescription onGenericArray(GenericTypeDescription genericTypeDescription) {
                return genericTypeDescription.asRawType();
            }

            @Override
            public TypeDescription onWildcardType(GenericTypeDescription genericTypeDescription) {
                throw new IllegalArgumentException("Cannot erase wildcard: " + genericTypeDescription);
            }

            @Override
            public TypeDescription onParameterizedType(GenericTypeDescription genericTypeDescription) {
                return genericTypeDescription.asRawType();
            }

            @Override
            public TypeDescription onTypeVariable(GenericTypeDescription genericTypeDescription) {
                return genericTypeDescription.asRawType();
            }

            @Override
            public TypeDescription onRawType(TypeDescription typeDescription) {
                return typeDescription;
            }
        }

        abstract class Substitutor implements Visitor<GenericTypeDescription> {

            @Override
            public GenericTypeDescription onParameterizedType(GenericTypeDescription genericTypeDescription) {
                GenericTypeDescription ownerType = genericTypeDescription.getOwnerType();
                List<GenericTypeDescription> parameters = new ArrayList<GenericTypeDescription>(genericTypeDescription.getParameters().size());
                for (GenericTypeDescription parameter : genericTypeDescription.getParameters()) {
                    parameters.add(parameter.accept(this));
                }
                return new GenericTypeDescription.ForParameterizedType.Latent(genericTypeDescription.asRawType(),
                        parameters,
                        ownerType == null
                                ? null
                                : ownerType.accept(this));
            }

            @Override
            public GenericTypeDescription onGenericArray(GenericTypeDescription genericTypeDescription) {
                return GenericTypeDescription.ForGenericArray.Latent.of(genericTypeDescription.getComponentType().accept(this), 1);
            }

            @Override
            public GenericTypeDescription onWildcardType(GenericTypeDescription genericTypeDescription) {
                GenericTypeList lowerBounds = genericTypeDescription.getLowerBounds(), upperBounds = genericTypeDescription.getUpperBounds();
                return lowerBounds.isEmpty()
                        ? GenericTypeDescription.ForWildcardType.Latent.boundedAbove(upperBounds.getOnly().accept(this))
                        : GenericTypeDescription.ForWildcardType.Latent.boundedBelow(lowerBounds.getOnly().accept(this));
            }

            public static class ForAttachment extends Substitutor {

                public static ForAttachment of(FieldDescription fieldDescription) {
                    return new ForAttachment(fieldDescription.getDeclaringType(), fieldDescription.getDeclaringType());
                }

                public static ForAttachment of(MethodDescription methodDescription) {
                    return new ForAttachment(methodDescription.getDeclaringType(), methodDescription);
                }

                public static ForAttachment of(TypeDescription typeDescription) {
                    return new ForAttachment(typeDescription, typeDescription);
                }

                private final TypeDescription declaringType;

                private final TypeVariableSource typeVariableSource;

                private final Map<String, GenericTypeDescription> formalVariables;

                protected ForAttachment(TypeDescription declaringType, TypeVariableSource typeVariableSource) {
                    this.declaringType = declaringType;
                    this.typeVariableSource = typeVariableSource;
                    formalVariables = new HashMap<String, GenericTypeDescription>();
                }

                @Override
                public GenericTypeDescription onTypeVariable(GenericTypeDescription genericTypeDescription) {
                    if (genericTypeDescription.getSort().isTypeVariable()) {
                        throw new IllegalStateException("Type variable is not detached: " + genericTypeDescription);
                    }
                    GenericTypeDescription typeVariable = formalVariables.get(genericTypeDescription.getSymbol());
                    if (typeVariable == null) {
                        typeVariable = typeVariableSource.findVariable(genericTypeDescription.getSymbol());
                    }
                    if (typeVariable == null) {
                        throw new IllegalStateException("Cannot attach undefined type variable: " + genericTypeDescription);
                    }
                    return typeVariable;
                }

                @Override
                public GenericTypeDescription onRawType(TypeDescription typeDescription) {
                    return typeDescription.equals(TargetType.DESCRIPTION)
                            ? declaringType
                            : typeDescription;
                }

                protected void register(String symbol, GenericTypeDescription typeVariable) {
                    if (formalVariables.put(symbol, typeVariable) != null) {
                        throw new IllegalArgumentException("Nominal type variable is already defined: " + symbol);
                    }
                }

                public class OfFormalVariable implements GenericTypeDescription.Visitor<GenericTypeDescription> {

                    private final List<? super GenericTypeDescription> typeVariables;

                    public OfFormalVariable(List<? super GenericTypeDescription> typeVariables) {
                        this.typeVariables = typeVariables;
                    }

                    @Override
                    public GenericTypeDescription onTypeVariable(GenericTypeDescription genericTypeDescription) {
                        if (genericTypeDescription.getSort().isTypeVariable()) {
                            throw new IllegalStateException("Type variable is not detached: " + genericTypeDescription);
                        }
                        return new AttachedTypeVariable(genericTypeDescription.getSymbol(),
                                typeVariableSource,
                                genericTypeDescription.getUpperBounds(),
                                typeVariables,
                                ForAttachment.this);
                    }

                    @Override
                    public GenericTypeDescription onRawType(TypeDescription typeDescription) {
                        throw new IllegalArgumentException("Expected type variable: " + typeDescription);
                    }

                    @Override
                    public GenericTypeDescription onGenericArray(GenericTypeDescription genericTypeDescription) {
                        throw new IllegalArgumentException("Expected type variable: " + genericTypeDescription);
                    }

                    @Override
                    public GenericTypeDescription onWildcardType(GenericTypeDescription genericTypeDescription) {
                        throw new IllegalArgumentException("Expected type variable: " + genericTypeDescription);
                    }

                    @Override
                    public GenericTypeDescription onParameterizedType(GenericTypeDescription genericTypeDescription) {
                        throw new IllegalArgumentException("Expected type variable: " + genericTypeDescription);
                    }
                }

                protected static class AttachedTypeVariable extends GenericTypeDescription.ForTypeVariable {

                    private final String symbol;

                    private final TypeVariableSource typeVariableSource;

                    private final List<GenericTypeDescription> bounds;

                    public AttachedTypeVariable(String symbol,
                                                TypeVariableSource typeVariableSource,
                                                List<GenericTypeDescription> bounds,
                                                List<? super GenericTypeDescription> typeVariables,
                                                GenericTypeDescription.Visitor<GenericTypeDescription> visitor) {
                        this.symbol = symbol;
                        this.typeVariableSource = typeVariableSource;
                        typeVariables.add(this);
                        this.bounds = new ArrayList<GenericTypeDescription>(bounds.size());
                        for (GenericTypeDescription bound : bounds) {
                            this.bounds.add(bound.accept(visitor));
                        }
                    }

                    @Override
                    public GenericTypeList getUpperBounds() {
                        return new GenericTypeList.Explicit(bounds);
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

            public static class ForDetachment extends Substitutor {

                private final TypeDescription declaringType;

                private final Map<String, GenericTypeDescription> detachedVariables;

                public ForDetachment(TypeDescription declaringType) {
                    this.declaringType = declaringType;
                    detachedVariables = new HashMap<String, GenericTypeDescription>();
                }

                @Override
                public GenericTypeDescription onTypeVariable(GenericTypeDescription genericTypeDescription) {
                    if (genericTypeDescription.getSort().isDetachedTypeVariable()) {
                        throw new IllegalStateException("Type variable already detached: " + genericTypeDescription);
                    }
                    GenericTypeDescription typeVariable = detachedVariables.get(genericTypeDescription.getSymbol());
                    if (typeVariable == null) {
                        typeVariable = new DetachedTypeVariable(genericTypeDescription.getSymbol(),
                                genericTypeDescription.getUpperBounds(),
                                this);
                    }
                    return typeVariable;
                }

                @Override
                public GenericTypeDescription onRawType(TypeDescription typeDescription) {
                    return declaringType.equals(typeDescription)
                            ? TargetType.DESCRIPTION
                            : typeDescription;
                }

                protected void register(String symbol, GenericTypeDescription typeVariable) {
                    detachedVariables.put(symbol, typeVariable);
                }

                protected static class DetachedTypeVariable extends GenericTypeDescription.ForTypeVariable.InDetachedForm {

                    private final String symbol;

                    private final List<GenericTypeDescription> bounds;

                    public DetachedTypeVariable(String symbol, List<GenericTypeDescription> bounds, ForDetachment visitor) {
                        this.symbol = symbol;
                        visitor.register(symbol, this);
                        this.bounds = new ArrayList<GenericTypeDescription>(bounds.size());
                        for (GenericTypeDescription bound : bounds) {
                            this.bounds.add(bound.accept(visitor));
                        }
                    }

                    @Override
                    public GenericTypeList getUpperBounds() {
                        return new GenericTypeList.Explicit(bounds);
                    }

                    @Override
                    public String getSymbol() {
                        return symbol;
                    }
                }
            }

            public static class ForTypeVariable extends Substitutor {

                public static Visitor<GenericTypeDescription> bind(GenericTypeDescription typeDescription) {
                    Map<GenericTypeDescription, GenericTypeDescription> bindings = new HashMap<GenericTypeDescription, GenericTypeDescription>();
                    do {
                        GenericTypeList parameters = typeDescription.getParameters();
                        int index = 0;
                        for (GenericTypeDescription typeVariable : typeDescription.asRawType().getTypeVariables()) {
                            bindings.put(typeVariable, parameters.get(index++));
                        }
                        typeDescription = typeDescription.getOwnerType();
                    } while (typeDescription != null);
                    return new ForTypeVariable(bindings);
                }

                private final Map<GenericTypeDescription, GenericTypeDescription> bindings;

                protected ForTypeVariable(Map<GenericTypeDescription, GenericTypeDescription> bindings) {
                    this.bindings = bindings;
                }

                @Override
                public GenericTypeDescription onTypeVariable(GenericTypeDescription genericTypeDescription) {
                    GenericTypeDescription substitution = bindings.get(genericTypeDescription);
                    if (substitution == null) {
                        throw new IllegalArgumentException("Cannot substitute " + genericTypeDescription + " with " + bindings);
                    }
                    return substitution;
                }

                @Override
                public GenericTypeDescription onRawType(TypeDescription typeDescription) {
                    return typeDescription;
                }
            }

            public static class ForRawType extends Substitutor implements TypeVariableSource.Visitor<ForRawType.TypeVariableProxy> {

                public static Visitor<GenericTypeDescription> replace(TypeDescription substitute,
                                                                      ElementMatcher<? super TypeDescription> substitutionMatcher) {
                    return new ForRawType(substitute, substitutionMatcher);
                }

                private final TypeDescription substitute;

                private final ElementMatcher<? super TypeDescription> substitutionMatcher;

                protected ForRawType(TypeDescription substitute, ElementMatcher<? super TypeDescription> substitutionMatcher) {
                    this.substitute = substitute;
                    this.substitutionMatcher = substitutionMatcher;
                }

                @Override
                public GenericTypeDescription onTypeVariable(GenericTypeDescription genericTypeDescription) {
                    return genericTypeDescription.getVariableSource().accept(this).resolve(genericTypeDescription);
                }

                @Override
                public GenericTypeDescription onRawType(TypeDescription typeDescription) {
                    int arity = 0;
                    TypeDescription componentType = typeDescription;
                    while (componentType.isArray()) {
                        componentType = componentType.getComponentType();
                        arity++;
                    }
                    return substitutionMatcher.matches(componentType)
                            ? TypeDescription.ArrayProjection.of(substitute, arity)
                            : typeDescription;
                }

                @Override
                public TypeVariableProxy onType(TypeDescription typeDescription) {
                    return substitutionMatcher.matches(typeDescription)
                            ? new TypeVariableProxy.ForType(substitute)
                            : TypeVariableProxy.Retaining.INSTANCE;
                }

                @Override
                @SuppressWarnings("unchecked")
                public TypeVariableProxy onMethod(MethodDescription methodDescription) {
                    return substitutionMatcher.matches(methodDescription.getDeclaringType())
                            ? TypeVariableProxy.ForMethod.of(substitute, (Visitor<TypeDescription>) (Object) this, methodDescription)
                            : TypeVariableProxy.Retaining.INSTANCE;
                }

                protected static abstract class LazyTypeVariable extends GenericTypeDescription.ForTypeVariable {

                    protected final String symbol;

                    protected LazyTypeVariable(String symbol) {
                        this.symbol = symbol;
                    }

                    @Override
                    public GenericTypeList getUpperBounds() {
                        return resolve().getUpperBounds();
                    }

                    @Override
                    public TypeVariableSource getVariableSource() {
                        return resolve().getVariableSource();
                    }

                    @Override
                    public String getSymbol() {
                        return symbol;
                    }

                    protected abstract GenericTypeDescription resolve();

                    protected static class OfType extends LazyTypeVariable {

                        private final TypeDescription typeDescription;

                        protected OfType(String symbol, TypeDescription typeDescription) {
                            super(symbol);
                            this.typeDescription = typeDescription;
                        }

                        @Override
                        protected GenericTypeDescription resolve() {
                            GenericTypeDescription genericTypeDescription = typeDescription.findVariable(symbol);
                            if (genericTypeDescription == null) {
                                throw new IllegalStateException("Cannot resolve type variable '" + symbol + "' for " + typeDescription);
                            }
                            return genericTypeDescription;
                        }
                    }

                    protected static class OfMethod extends LazyTypeVariable {

                        private final TypeDescription typeDescription;

                        private final ElementMatcher<? super MethodDescription> typeVariableMethodMatcher;

                        protected OfMethod(String symbol,
                                           TypeDescription typeDescription,
                                           ElementMatcher<? super MethodDescription> typeVariableMethodMatcher) {
                            super(symbol);
                            this.typeDescription = typeDescription;
                            this.typeVariableMethodMatcher = typeVariableMethodMatcher;
                        }

                        @Override
                        protected GenericTypeDescription resolve() {
                            MethodList methodDescriptions = typeDescription.getDeclaredMethods().filter(typeVariableMethodMatcher);
                            if (methodDescriptions.isEmpty()) {
                                throw new IllegalStateException("Cannot resolve method " + typeVariableMethodMatcher + " declared by " + typeDescription);
                            }
                            GenericTypeDescription genericTypeDescription = methodDescriptions.getOnly().findVariable(symbol);
                            if (genericTypeDescription == null) {
                                throw new IllegalStateException("Cannot resolve type variable '" + symbol + "' for " + methodDescriptions.getOnly());
                            }
                            return genericTypeDescription;
                        }
                    }
                }

                protected interface TypeVariableProxy {

                    GenericTypeDescription resolve(GenericTypeDescription original);

                    enum Retaining implements TypeVariableProxy {

                        INSTANCE;

                        @Override
                        public GenericTypeDescription resolve(GenericTypeDescription original) {
                            return original;
                        }
                    }

                    class ForType implements TypeVariableProxy {

                        private final TypeDescription substitute;

                        protected ForType(TypeDescription substitute) {
                            this.substitute = substitute;
                        }

                        @Override
                        public GenericTypeDescription resolve(GenericTypeDescription original) {
                            return new LazyTypeVariable.OfType(original.getSymbol(), substitute);
                        }
                    }

                    class ForMethod implements TypeVariableProxy {

                        protected static TypeVariableProxy of(TypeDescription substitute,
                                                              Visitor<TypeDescription> substitutor,
                                                              MethodDescription methodDescription) {
                            return new ForMethod(substitute,
                                    returns(methodDescription.getReturnType().asRawType().accept(substitutor))
                                            .and(takesArguments(methodDescription.getParameters().asTypeList().asRawTypes().accept(substitutor)))
                                            .and(methodDescription.isConstructor()
                                                    ? isConstructor()
                                                    : ElementMatchers.<MethodDescription>named(methodDescription.getName())));
                        }

                        private final TypeDescription substitute;

                        private final ElementMatcher<? super MethodDescription> typeVariableMethodMatcher;

                        protected ForMethod(TypeDescription substitute, ElementMatcher<? super MethodDescription> typeVariableMethodMatcher) {
                            this.substitute = substitute;
                            this.typeVariableMethodMatcher = typeVariableMethodMatcher;
                        }

                        @Override
                        public GenericTypeDescription resolve(GenericTypeDescription original) {
                            return new LazyTypeVariable.OfMethod(original.getSymbol(), substitute, typeVariableMethodMatcher);
                        }
                    }
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
        public GenericTypeDescription getSuperType() {
            throw new IllegalStateException("A generic array does not imply a super type definition: " + this);
        }

        @Override
        public GenericTypeList getInterfaces() {
            throw new IllegalStateException("A generic array does not imply an interface type definition: " + this);
        }

        @Override
        public FieldList getDeclaredFields() {
            throw new IllegalStateException("A generic array does not imply field definitions: " + this);
        }

        @Override
        public MethodList getDeclaredMethods() {
            throw new IllegalStateException("A generic array does not imply method definitions: " + this);
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
        public <T> T accept(Visitor<T> visitor) {
            return visitor.onGenericArray(this);
        }

        @Override
        public StackSize getStackSize() {
            return StackSize.SINGLE;
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
                return arity == 1
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
            throw new IllegalStateException("A wildcard does not represent an erasable type: " + this);
        }

        @Override
        public GenericTypeDescription getSuperType() {
            throw new IllegalStateException("A wildcard does not imply a super type definition: " + this);
        }

        @Override
        public GenericTypeList getInterfaces() {
            throw new IllegalStateException("A wildcard does not imply an interface type definition: " + this);
        }

        @Override
        public FieldList getDeclaredFields() {
            throw new IllegalStateException("A wildcard does not imply field definitions: " + this);
        }

        @Override
        public MethodList getDeclaredMethods() {
            throw new IllegalStateException("A wildcard does not imply method definitions: " + this);
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
        public <T> T accept(Visitor<T> visitor) {
            return visitor.onWildcardType(this);
        }

        @Override
        public StackSize getStackSize() {
            throw new IllegalStateException("A wildcard does not imply an operand stack size: " + this);
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
            GenericTypeList bounds = getLowerBounds();
            if (!bounds.isEmpty()) {
                stringBuilder.append(" super ");
            } else {
                bounds = getUpperBounds();
                if (bounds.getOnly().equals(TypeDescription.OBJECT)) {
                    return SYMBOL;
                }
                stringBuilder.append(" extends ");
            }
            return stringBuilder.append(bounds.getOnly().getTypeName()).toString();
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
                return new Latent(Collections.singletonList(TypeDescription.OBJECT), Collections.<GenericTypeDescription>emptyList());
            }

            public static GenericTypeDescription boundedAbove(GenericTypeDescription upperBound) {
                return new Latent(Collections.singletonList(upperBound), Collections.<GenericTypeDescription>emptyList());
            }

            public static GenericTypeDescription boundedBelow(GenericTypeDescription lowerBound) {
                return new Latent(Collections.singletonList(TypeDescription.OBJECT), Collections.singletonList(lowerBound));
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
        public GenericTypeDescription getSuperType() {
            return asRawType().getSuperType().accept(Visitor.Substitutor.ForTypeVariable.bind(this));
        }

        @Override
        public GenericTypeList getInterfaces() {
            return asRawType().getInterfaces().accept(Visitor.Substitutor.ForTypeVariable.bind(this));
        }

        @Override
        public FieldList getDeclaredFields() {
            FieldList declaredFields = asRawType().getDeclaredFields();
            List<FieldDescription> resolved = new ArrayList<FieldDescription>(declaredFields.size());
            Visitor<GenericTypeDescription> visitor = Visitor.Substitutor.ForTypeVariable.bind(this);
            for (FieldDescription fieldDescription : declaredFields) {
                resolved.add(new FieldDescription.Latent(fieldDescription.getDeclaringType(), fieldDescription.asToken().accept(visitor)));
            }
            return new FieldList.Explicit(resolved);
        }

        @Override
        public MethodList getDeclaredMethods() {
            MethodList declaredMethods = asRawType().getDeclaredMethods();
            List<MethodDescription> resolved = new ArrayList<MethodDescription>(declaredMethods.size());
            Visitor<GenericTypeDescription> visitor = Visitor.Substitutor.ForTypeVariable.bind(this);
            for (MethodDescription methodDescription : declaredMethods) {
                resolved.add(new MethodDescription.Latent(methodDescription.getDeclaringType(), methodDescription.asToken().accept(visitor)));
            }
            return new MethodList.Explicit(resolved);
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
        public <T> T accept(Visitor<T> visitor) {
            return visitor.onParameterizedType(this);
        }

        @Override
        public StackSize getStackSize() {
            return StackSize.SINGLE;
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
        public GenericTypeDescription getSuperType() {
            throw new IllegalStateException("A type variable does not imply a super type definition: " + this);
        }

        @Override
        public GenericTypeList getInterfaces() {
            throw new IllegalStateException("A type variable does not imply an interface type definition: " + this);
        }

        @Override
        public FieldList getDeclaredFields() {
            throw new IllegalStateException("A type variable does not imply field definitions: " + this);
        }

        @Override
        public MethodList getDeclaredMethods() {
            throw new IllegalStateException("A type variable does not imply method definitions: " + this);
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
        public <T> T accept(Visitor<T> visitor) {
            return visitor.onTypeVariable(this);
        }

        @Override
        public StackSize getStackSize() {
            return StackSize.SINGLE;
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
                    && getSymbol().equals(genericTypeDescription.getSymbol())
                    && getVariableSource().equals(genericTypeDescription.getVariableSource());
        }

        @Override
        public String toString() {
            return getSymbol();
        }

        public abstract static class InDetachedForm extends ForTypeVariable {

            @Override
            public Sort getSort() {
                return Sort.DETACHED_VARIABLE;
            }

            @Override
            public TypeVariableSource getVariableSource() {
                throw new IllegalStateException("A detached type variable does not represent a source: " + this);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (!(other instanceof GenericTypeDescription)) return false;
                GenericTypeDescription typeVariable = (GenericTypeDescription) other;
                return typeVariable.getSort().isDetachedTypeVariable()
                        && getSymbol().equals(typeVariable.getSymbol());
            }

            @Override
            public int hashCode() {
                return getSymbol().hashCode();
            }
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
    }

    abstract class LazyProjection implements GenericTypeDescription {

        protected abstract GenericTypeDescription resolve();

        @Override
        public Sort getSort() {
            return resolve().getSort();
        }

        @Override
        public GenericTypeList getInterfaces() {
            return resolve().getInterfaces();
        }

        @Override
        public GenericTypeDescription getSuperType() {
            return resolve().getSuperType();
        }

        @Override
        public FieldList getDeclaredFields() {
            return resolve().getDeclaredFields();
        }

        @Override
        public MethodList getDeclaredMethods() {
            return resolve().getDeclaredMethods();
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
        public <T> T accept(Visitor<T> visitor) {
            return resolve().accept(visitor);
        }

        @Override
        public StackSize getStackSize() {
            return asRawType().getStackSize();
        }

        @Override
        public int hashCode() {
            return resolve().hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return resolve().equals(other);
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
