package net.bytebuddy.pool;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.field.FieldList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.PropertyDispatcher;
import org.objectweb.asm.*;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;

public interface TypePool {

    TypeDescription describe(String name);

    void clear();

    static interface CacheProvider {

        TypeDescription find(String name);

        TypeDescription register(TypeDescription typeDescription);

        void clear();

        static class Simple implements CacheProvider {

            private final ConcurrentMap<String, TypeDescription> cache;

            public Simple() {
                cache = new ConcurrentHashMap<String, TypeDescription>();
            }

            @Override
            public TypeDescription find(String name) {
                return cache.get(name);
            }

            @Override
            public TypeDescription register(TypeDescription typeDescription) {
                TypeDescription cached = cache.putIfAbsent(typeDescription.getName(), typeDescription);
                return cached == null ? typeDescription : cached;
            }

            @Override
            public void clear() {
                cache.clear();
            }

            @Override
            public String toString() {
                return "TypePool.CacheProvider.Simple{cache=" + cache + '}';
            }
        }

        static enum NoOp implements CacheProvider {

            INSTANCE;

            @Override
            public TypeDescription find(String name) {
                return null;
            }

            @Override
            public TypeDescription register(TypeDescription typeDescription) {
                return typeDescription;
            }

            @Override
            public void clear() {
                /* do nothing */
            }
        }
    }

    abstract static class AbstractBase implements TypePool {

        private static final String ARRAY_SYMBOL = "[";

        protected static final Map<String, TypeDescription> PRIMITIVE_TYPES;

        protected static final Map<String, String> PRIMITIVE_DESCRIPTORS;

        static {
            Map<String, TypeDescription> primitiveTypes = new HashMap<String, TypeDescription>();
            Map<String, String> primitiveDescriptors = new HashMap<String, String>();
            for (Class<?> primitiveType : new Class<?>[]{boolean.class,
                    byte.class,
                    short.class,
                    char.class,
                    int.class,
                    long.class,
                    float.class,
                    double.class,
                    void.class}) {
                primitiveTypes.put(primitiveType.getName(), new TypeDescription.ForLoadedType(primitiveType));
                primitiveDescriptors.put(Type.getDescriptor(primitiveType), primitiveType.getName());
            }
            PRIMITIVE_TYPES = Collections.unmodifiableMap(primitiveTypes);
            PRIMITIVE_DESCRIPTORS = Collections.unmodifiableMap(primitiveDescriptors);
        }

        protected final CacheProvider cacheProvider;

        protected AbstractBase(CacheProvider cacheProvider) {
            this.cacheProvider = cacheProvider;
        }

        @Override
        public TypeDescription describe(String name) {
            if (name.contains("/")) {
                throw new IllegalArgumentException(name + " contains the illegal character '/'");
            }
            int arity = 0;
            while (name.startsWith(ARRAY_SYMBOL)) {
                arity++;
                name = name.substring(1);
            }
            if (arity > 0) {
                String primitiveName = PRIMITIVE_DESCRIPTORS.get(name);
                name = primitiveName == null ? name.substring(1, name.length() - 1) : primitiveName;
            }
            TypeDescription typeDescription = PRIMITIVE_TYPES.get(name);
            typeDescription = typeDescription == null ? cacheProvider.find(name) : typeDescription;
            return TypeDescription.ArrayProjection.of(typeDescription == null
                    ? cacheProvider.register(doDescribe(name))
                    : typeDescription, arity);
        }

        @Override
        public void clear() {
            cacheProvider.clear();
        }

        protected abstract TypeDescription doDescribe(String name);

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && cacheProvider.equals(((AbstractBase) other).cacheProvider);
        }

        @Override
        public int hashCode() {
            return cacheProvider.hashCode();
        }
    }

    static class Default extends AbstractBase {

        private static final int ASM_VERSION = Opcodes.ASM5;

        private static final int ASM_MANUAL = 0;

        public static TypePool ofClassPath() {
            return new Default(new CacheProvider.Simple(),
                    new TypeSourceLocator.ForClassLoader(ClassLoader.getSystemClassLoader()));
        }

        private final TypeSourceLocator typeSourceLocator;

        public Default(CacheProvider cacheProvider, TypeSourceLocator typeSourceLocator) {
            super(cacheProvider);
            this.typeSourceLocator = typeSourceLocator;
        }

        protected TypeDescription doDescribe(String name) {
            byte[] binaryRepresentation = typeSourceLocator.locate(name);
            if (binaryRepresentation == null) {
                throw new IllegalArgumentException("Cannot locate " + name + " using " + typeSourceLocator);
            }
            return parse(binaryRepresentation);
        }

        private TypeDescription parse(byte[] binaryRepresentation) {
            ClassReader classReader = new ClassReader(binaryRepresentation);
            TypeExtractor typeExtractor = new TypeExtractor();
            classReader.accept(typeExtractor, ASM_MANUAL);
            return typeExtractor.toTypeDescription();
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && super.equals(other)
                    && typeSourceLocator.equals(((Default) other).typeSourceLocator);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + typeSourceLocator.hashCode();
        }

        @Override
        public String toString() {
            return "TypePool.Default{" +
                    "typeSourceLocator=" + typeSourceLocator +
                    ", cacheProvider=" + cacheProvider +
                    '}';
        }

        private class TypeExtractor extends ClassVisitor {

            private int modifiers;

            private String internalName;

            private String superTypeName;

            private String[] interfaceName;

            private boolean anonymousType;

            private UnloadedTypeDescription.DeclarationContext declarationContext;

            private final List<UnloadedTypeDescription.AnnotationToken> annotationTokens;

            private final List<UnloadedTypeDescription.FieldToken> fieldTokens;

            private final List<UnloadedTypeDescription.MethodToken> methodTokens;

            private TypeExtractor() {
                super(ASM_VERSION);
                declarationContext = UnloadedTypeDescription.DeclarationContext.SelfDeclared.INSTANCE;
                anonymousType = false;
                annotationTokens = new LinkedList<UnloadedTypeDescription.AnnotationToken>();
                fieldTokens = new LinkedList<UnloadedTypeDescription.FieldToken>();
                methodTokens = new LinkedList<UnloadedTypeDescription.MethodToken>();
            }

            @Override
            public void visit(int classFileVersion,
                              int modifiers,
                              String internalName,
                              String genericSignature,
                              String superTypeName,
                              String[] interfaceName) {
                this.modifiers = modifiers;
                this.internalName = internalName;
                this.superTypeName = superTypeName;
                this.interfaceName = interfaceName;
            }

            @Override
            public void visitOuterClass(String typeName, String methodName, String methodDescriptor) {
                if (methodName != null) {
                    declarationContext = new UnloadedTypeDescription.DeclarationContext.DeclaredInMethod(typeName,
                            methodName,
                            methodDescriptor);
                } else if (typeName != null) {
                    declarationContext = new UnloadedTypeDescription.DeclarationContext.DeclaredInType(typeName);
                }
            }

            @Override
            public void visitInnerClass(String internalName, String outerName, String innerName, int modifiers) {
                if (internalName.equals(this.internalName)) {
                    this.modifiers = modifiers;
                    if (innerName == null) {
                        anonymousType = true;
                    }
                    if (declarationContext.isSelfDeclared()) {
                        declarationContext = new UnloadedTypeDescription.DeclarationContext.DeclaredInType(outerName);
                    }
                }
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                return new AnnotationExtractor(Default.this,
                        new OnTypeCollector(descriptor),
                        new AnnotationExtractor.ComponentTypeLocator.ForAnnotationProperty(Default.this, descriptor));
            }

            private class OnTypeCollector implements AnnotationExtractor.Registrant {

                private final String descriptor;

                private final Map<String, UnloadedTypeDescription.AnnotationValue<?, ?>> values;

                private OnTypeCollector(String descriptor) {
                    this.descriptor = descriptor;
                    values = new HashMap<String, UnloadedTypeDescription.AnnotationValue<?, ?>>();
                }

                @Override
                public void register(String name, UnloadedTypeDescription.AnnotationValue<?, ?> annotationValue) {
                    values.put(name, annotationValue);
                }

                @Override
                public void onComplete() {
                    annotationTokens.add(new UnloadedTypeDescription.AnnotationToken(descriptor, values));
                }
            }

            @Override
            public FieldVisitor visitField(int modifiers,
                                           String internalName,
                                           String descriptor,
                                           String genericSignature,
                                           Object defaultValue) {
                return new FieldExtractor(modifiers, internalName, descriptor);
            }

            @Override
            public MethodVisitor visitMethod(int modifiers,
                                             String internalName,
                                             String descriptor,
                                             String genericSignature,
                                             String[] exceptionName) {
                if (internalName.equals(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME)) {
                    return null;
                }
                return new MethodExtractor(modifiers, internalName, descriptor, exceptionName);
            }

            public TypeDescription toTypeDescription() {
                return new UnloadedTypeDescription(Default.this,
                        modifiers,
                        internalName,
                        superTypeName,
                        interfaceName,
                        declarationContext,
                        anonymousType,
                        annotationTokens,
                        fieldTokens,
                        methodTokens);
            }

            private class FieldExtractor extends FieldVisitor {

                private final int modifiers;

                private final String internalName;

                private final String descriptor;

                private final List<UnloadedTypeDescription.AnnotationToken> annotationTokens;

                private FieldExtractor(int modifiers, String internalName, String descriptor) {
                    super(ASM_VERSION);
                    this.modifiers = modifiers;
                    this.internalName = internalName;
                    this.descriptor = descriptor;
                    annotationTokens = new LinkedList<UnloadedTypeDescription.AnnotationToken>();
                }

                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    return new AnnotationExtractor(Default.this,
                            new OnFieldCollector(descriptor),
                            new AnnotationExtractor.ComponentTypeLocator.ForAnnotationProperty(Default.this, descriptor));
                }

                private class OnFieldCollector implements AnnotationExtractor.Registrant {

                    private final String descriptor;

                    private final Map<String, UnloadedTypeDescription.AnnotationValue<?, ?>> values;

                    private OnFieldCollector(String descriptor) {
                        this.descriptor = descriptor;
                        values = new HashMap<String, UnloadedTypeDescription.AnnotationValue<?, ?>>();
                    }

                    @Override
                    public void register(String name, UnloadedTypeDescription.AnnotationValue<?, ?> annotationValue) {
                        values.put(name, annotationValue);
                    }

                    @Override
                    public void onComplete() {
                        annotationTokens.add(new UnloadedTypeDescription.AnnotationToken(descriptor, values));
                    }
                }

                @Override
                public void visitEnd() {
                    fieldTokens.add(new UnloadedTypeDescription.FieldToken(modifiers, internalName, descriptor, annotationTokens));
                }
            }

            private class MethodExtractor extends MethodVisitor implements AnnotationExtractor.Registrant {

                private final int modifiers;

                private final String internalName;

                private final String descriptor;

                private final String[] exceptionName;

                private final List<UnloadedTypeDescription.AnnotationToken> annotationTokens;

                private final Map<Integer, List<UnloadedTypeDescription.AnnotationToken>> parameterAnnotationTokens;

                private UnloadedTypeDescription.AnnotationValue<?, ?> defaultValue;

                private MethodExtractor(int modifiers,
                                        String internalName,
                                        String descriptor,
                                        String[] exceptionName) {
                    super(ASM_VERSION);
                    this.modifiers = modifiers;
                    this.internalName = internalName;
                    this.descriptor = descriptor;
                    this.exceptionName = exceptionName;
                    annotationTokens = new LinkedList<UnloadedTypeDescription.AnnotationToken>();
                    parameterAnnotationTokens = new HashMap<Integer, List<UnloadedTypeDescription.AnnotationToken>>();
                    for (int i = 0; i < Type.getMethodType(descriptor).getArgumentTypes().length; i++) {
                        parameterAnnotationTokens.put(i, new LinkedList<UnloadedTypeDescription.AnnotationToken>());
                    }
                }

                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    return new AnnotationExtractor(Default.this,
                            new OnMethodCollector(descriptor),
                            new AnnotationExtractor.ComponentTypeLocator.ForAnnotationProperty(Default.this, descriptor));
                }

                @Override
                public AnnotationVisitor visitParameterAnnotation(int index, String descriptor, boolean visible) {
                    return new AnnotationExtractor(Default.this,
                            new OnMethodParameterCollector(descriptor, index),
                            new AnnotationExtractor.ComponentTypeLocator.ForAnnotationProperty(Default.this, descriptor));
                }

                @Override
                public AnnotationVisitor visitAnnotationDefault() {
                    return new AnnotationExtractor(Default.this,
                            this,
                            new AnnotationExtractor.ComponentTypeLocator.FixedArrayReturnType(descriptor));
                }

                @Override
                public void register(String ignored, UnloadedTypeDescription.AnnotationValue<?, ?> annotationValue) {
                    defaultValue = annotationValue;
                }

                @Override
                public void onComplete() {
                    /* do nothing, as the register method is called at most once for default values */
                }

                @Override
                public void visitEnd() {
                    methodTokens.add(new UnloadedTypeDescription.MethodToken(modifiers,
                            internalName,
                            descriptor,
                            exceptionName,
                            annotationTokens,
                            parameterAnnotationTokens,
                            defaultValue));
                }

                private class OnMethodCollector implements AnnotationExtractor.Registrant {

                    private final String descriptor;

                    private final Map<String, UnloadedTypeDescription.AnnotationValue<?, ?>> values;

                    private OnMethodCollector(String descriptor) {
                        this.descriptor = descriptor;
                        values = new HashMap<String, UnloadedTypeDescription.AnnotationValue<?, ?>>();
                    }

                    @Override
                    public void register(String name, UnloadedTypeDescription.AnnotationValue<?, ?> annotationValue) {
                        values.put(name, annotationValue);
                    }

                    @Override
                    public void onComplete() {
                        annotationTokens.add(new UnloadedTypeDescription.AnnotationToken(descriptor, values));
                    }
                }

                private class OnMethodParameterCollector implements AnnotationExtractor.Registrant {

                    private final String descriptor;

                    private final int index;

                    private final Map<String, UnloadedTypeDescription.AnnotationValue<?, ?>> values;

                    private OnMethodParameterCollector(String descriptor, int index) {
                        this.descriptor = descriptor;
                        this.index = index;
                        values = new HashMap<String, UnloadedTypeDescription.AnnotationValue<?, ?>>();
                    }

                    @Override
                    public void register(String name, UnloadedTypeDescription.AnnotationValue<?, ?> annotationValue) {
                        values.put(name, annotationValue);
                    }

                    @Override
                    public void onComplete() {
                        parameterAnnotationTokens.get(index).add(new UnloadedTypeDescription.AnnotationToken(descriptor, values));
                    }
                }
            }
        }

        protected static class AnnotationExtractor extends AnnotationVisitor {

            private final TypePool typePool;

            private final Registrant registrant;

            private final ComponentTypeLocator componentTypeLocator;

            public AnnotationExtractor(TypePool typePool,
                                       Registrant registrant,
                                       ComponentTypeLocator componentTypeLocator) {
                super(ASM_VERSION);
                this.typePool = typePool;
                this.registrant = registrant;
                this.componentTypeLocator = componentTypeLocator;
            }

            @Override
            public void visit(String name, Object value) {
                UnloadedTypeDescription.AnnotationValue<?, ?> annotationValue;
                if (value instanceof Type) {
                    annotationValue = new UnloadedTypeDescription.AnnotationValue.ForType((Type) value);
                } else if (value.getClass().isArray()) {
                    annotationValue = new UnloadedTypeDescription.AnnotationValue.Trivial<Object>(value);
                } else {
                    annotationValue = new UnloadedTypeDescription.AnnotationValue.Trivial<Object>(value);
                }
                registrant.register(name, annotationValue);
            }

            @Override
            public void visitEnum(String name, String descriptor, String value) {
                registrant.register(name, new UnloadedTypeDescription.AnnotationValue.ForEnumeration(descriptor, value));
            }

            @Override
            public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                return new AnnotationExtractor(typePool,
                        new AnnotationLookup(name, descriptor),
                        new ComponentTypeLocator.ForAnnotationProperty(typePool, descriptor));
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                return new AnnotationExtractor(typePool,
                        new ArrayLookup(name, componentTypeLocator.bind(name)),
                        ComponentTypeLocator.Illegal.INSTANCE);
            }

            @Override
            public void visitEnd() {
                registrant.onComplete();
            }

            private class ArrayLookup implements Registrant {

                private final String name;

                private final UnloadedTypeDescription.AnnotationValue.ForComplexArray.ComponentTypeReference componentTypeReference;

                private final LinkedList<UnloadedTypeDescription.AnnotationValue<?, ?>> values;

                private ArrayLookup(String name,
                                    UnloadedTypeDescription.AnnotationValue.ForComplexArray.ComponentTypeReference componentTypeReference) {
                    this.name = name;
                    this.componentTypeReference = componentTypeReference;
                    values = new LinkedList<UnloadedTypeDescription.AnnotationValue<?, ?>>();
                }

                @Override
                public void register(String ignored, UnloadedTypeDescription.AnnotationValue<?, ?> annotationValue) {
                    values.add(annotationValue);
                }

                @Override
                public void onComplete() {
                    registrant.register(name, new UnloadedTypeDescription.AnnotationValue.ForComplexArray(values, componentTypeReference));
                }
            }

            private class AnnotationLookup implements Registrant {

                private final String name;

                private final String descriptor;

                private final Map<String, UnloadedTypeDescription.AnnotationValue<?, ?>> values;

                private AnnotationLookup(String name, String descriptor) {
                    this.name = name;
                    this.descriptor = descriptor;
                    values = new HashMap<String, UnloadedTypeDescription.AnnotationValue<?, ?>>();
                }

                @Override
                public void register(String name, UnloadedTypeDescription.AnnotationValue<?, ?> annotationValue) {
                    values.put(name, annotationValue);
                }

                @Override
                public void onComplete() {
                    registrant.register(name, new UnloadedTypeDescription.AnnotationValue
                            .ForAnnotation(new UnloadedTypeDescription.AnnotationToken(descriptor, values)));
                }
            }

            public static interface Registrant {

                void register(String name, UnloadedTypeDescription.AnnotationValue<?, ?> annotationValue);

                void onComplete();
            }

            public static interface ComponentTypeLocator {

                static class ForAnnotationProperty implements ComponentTypeLocator {

                    private final TypePool typePool;

                    private final String annotationName;

                    public ForAnnotationProperty(TypePool typePool, String annotationDescriptor) {
                        this.typePool = typePool;
                        annotationName = annotationDescriptor.substring(1, annotationDescriptor.length() - 1).replace('/', '.');
                    }

                    @Override
                    public UnloadedTypeDescription.AnnotationValue.ForComplexArray.ComponentTypeReference bind(String name) {
                        return new Bound(name);
                    }

                    private class Bound implements UnloadedTypeDescription.AnnotationValue.ForComplexArray.ComponentTypeReference {

                        private final String name;

                        private Bound(String name) {
                            this.name = name;
                        }

                        @Override
                        public String lookup() {
                            return typePool.describe(annotationName)
                                    .getDeclaredMethods()
                                    .filter(named(name))
                                    .getOnly()
                                    .getReturnType()
                                    .getComponentType()
                                    .getName();
                        }
                    }
                }

                static class FixedArrayReturnType implements ComponentTypeLocator, UnloadedTypeDescription.AnnotationValue.ForComplexArray.ComponentTypeReference {

                    private final String componentType;

                    public FixedArrayReturnType(String methodDescriptor) {
                        String arrayType = Type.getMethodType(methodDescriptor).getReturnType().getClassName();
                        componentType = arrayType.substring(0, arrayType.length() - 2);
                    }

                    @Override
                    public UnloadedTypeDescription.AnnotationValue.ForComplexArray.ComponentTypeReference bind(String name) {
                        return this;
                    }

                    @Override
                    public String lookup() {
                        return componentType;
                    }
                }

                static enum Illegal implements ComponentTypeLocator {

                    INSTANCE;

                    @Override
                    public UnloadedTypeDescription.AnnotationValue.ForComplexArray.ComponentTypeReference bind(String name) {
                        throw new IllegalStateException("Unexpected lookup of component type for " + name);
                    }
                }

                UnloadedTypeDescription.AnnotationValue.ForComplexArray.ComponentTypeReference bind(String name);
            }
        }
    }

    static class UnloadedTypeDescription extends TypeDescription.AbstractTypeDescription.OfSimpleType {

        private final TypePool typePool;

        private final int modifiers;

        private final String name;

        private final String superTypeName;

        private final String[] interfaceInternalName;

        private final DeclarationContext declarationContext;

        private final boolean anonymousType;

        private final List<AnnotationDescription> declaredAnnotations;

        private final List<FieldDescription> declaredFields;

        private final List<MethodDescription> declaredMethods;

        protected static interface DeclarationContext {

            static enum SelfDeclared implements DeclarationContext {

                INSTANCE;

                @Override
                public MethodDescription getEnclosingMethod(TypePool typePool) {
                    return null;
                }

                @Override
                public TypeDescription getEnclosingType(TypePool typePool) {
                    return null;
                }

                @Override
                public boolean isSelfDeclared() {
                    return true;
                }

                @Override
                public boolean isDeclaredInType() {
                    return false;
                }

                @Override
                public boolean isDeclaredInMethod() {
                    return false;
                }

            }

            static class DeclaredInType implements DeclarationContext {

                private final String name;

                public DeclaredInType(String internalName) {
                    name = internalName.replace('/', '.');
                }

                @Override
                public MethodDescription getEnclosingMethod(TypePool typePool) {
                    return null;
                }

                @Override
                public TypeDescription getEnclosingType(TypePool typePool) {
                    return typePool.describe(name);
                }

                @Override
                public boolean isSelfDeclared() {
                    return false;
                }

                @Override
                public boolean isDeclaredInType() {
                    return true;
                }

                @Override
                public boolean isDeclaredInMethod() {
                    return false;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && name.equals(((DeclaredInType) other).name);
                }

                @Override
                public int hashCode() {
                    return name.hashCode();
                }

                @Override
                public String toString() {
                    return "TypePool.UnloadedTypeDescription.DeclarationContext.DeclaredInType{" +
                            "name='" + name + '\'' +
                            '}';
                }
            }

            static class DeclaredInMethod implements DeclarationContext {

                private final String name;

                private final String methodName;

                private final String methodDescriptor;

                public DeclaredInMethod(String internalName, String methodName, String methodDescriptor) {
                    name = internalName.replace('/', '.');
                    this.methodName = methodName;
                    this.methodDescriptor = methodDescriptor;
                }

                @Override
                public MethodDescription getEnclosingMethod(TypePool typePool) {
                    return getEnclosingType(typePool).getDeclaredMethods()
                            .filter(MethodDescription.CONSTRUCTOR_INTERNAL_NAME.equals(methodName)
                                    ? isConstructor() : named(methodName)
                                    .and(hasMethodDescriptor(methodDescriptor))).getOnly();
                }

                @Override
                public TypeDescription getEnclosingType(TypePool typePool) {
                    return typePool.describe(name);
                }

                @Override
                public boolean isSelfDeclared() {
                    return false;
                }

                @Override
                public boolean isDeclaredInType() {
                    return false;
                }

                @Override
                public boolean isDeclaredInMethod() {
                    return true;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    DeclaredInMethod that = (DeclaredInMethod) other;
                    return methodDescriptor.equals(that.methodDescriptor)
                            && methodName.equals(that.methodName)
                            && name.equals(that.name);
                }

                @Override
                public int hashCode() {
                    int result = name.hashCode();
                    result = 31 * result + methodName.hashCode();
                    result = 31 * result + methodDescriptor.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "TypePool.UnloadedTypeDescription.DeclarationContext.DeclaredInMethod{" +
                            "name='" + name + '\'' +
                            ", methodName='" + methodName + '\'' +
                            ", methodDescriptor='" + methodDescriptor + '\'' +
                            '}';
                }
            }

            MethodDescription getEnclosingMethod(TypePool typePool);

            TypeDescription getEnclosingType(TypePool typePool);

            boolean isSelfDeclared();

            boolean isDeclaredInType();

            boolean isDeclaredInMethod();
        }

        protected UnloadedTypeDescription(TypePool typePool,
                                          int modifiers,
                                          String name,
                                          String superTypeName,
                                          String[] interfaceName,
                                          DeclarationContext declarationContext,
                                          boolean anonymousType,
                                          List<AnnotationToken> annotationTokens,
                                          List<FieldToken> fieldTokens,
                                          List<MethodToken> methodTokens) {
            this.typePool = typePool;
            this.modifiers = modifiers;
            this.name = name.replace('/', '.');
            this.superTypeName = superTypeName == null ? null : superTypeName.replace('/', '.');
            this.interfaceInternalName = interfaceName;
            this.declarationContext = declarationContext;
            this.anonymousType = anonymousType;
            declaredAnnotations = new ArrayList<AnnotationDescription>(annotationTokens.size());
            for (AnnotationToken annotationToken : annotationTokens) {
                declaredAnnotations.add(annotationToken.toAnnotationDescription(typePool));
            }
            declaredFields = new ArrayList<FieldDescription>(fieldTokens.size());
            for (FieldToken fieldToken : fieldTokens) {
                declaredFields.add(fieldToken.toFieldDescription(this));
            }
            declaredMethods = new ArrayList<MethodDescription>(methodTokens.size());
            for (MethodToken methodToken : methodTokens) {
                declaredMethods.add(methodToken.toMethodDescription(this));
            }
        }

        @Override
        public TypeDescription getSupertype() {
            return superTypeName == null || isInterface()
                    ? null
                    : typePool.describe(superTypeName);
        }

        @Override
        public TypeList getInterfaces() {
            return interfaceInternalName == null
                    ? new TypeList.Empty()
                    : new TypePoolTypeList(interfaceInternalName);
        }

        @Override
        public MethodDescription getEnclosingMethod() {
            return declarationContext.getEnclosingMethod(typePool);
        }

        @Override
        public TypeDescription getEnclosingClass() {
            return declarationContext.getEnclosingType(typePool);
        }

        @Override
        public String getCanonicalName() {
            return name.replace('$', '.');
        }

        @Override
        public boolean isAnonymousClass() {
            return anonymousType;
        }

        @Override
        public boolean isLocalClass() {
            return !anonymousType && declarationContext.isDeclaredInMethod();
        }

        @Override
        public boolean isMemberClass() {
            return declarationContext.isDeclaredInType();
        }

        @Override
        public FieldList getDeclaredFields() {
            return new FieldList.Explicit(declaredFields);
        }

        @Override
        public MethodList getDeclaredMethods() {
            return new MethodList.Explicit(declaredMethods);
        }

        @Override
        public boolean isSealed() {
            return false;
        }

        @Override
        public ClassLoader getClassLoader() {
            return null;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public TypeDescription getDeclaringType() {
            return declarationContext.isDeclaredInType()
                    ? declarationContext.getEnclosingType(typePool)
                    : null;
        }

        @Override
        public int getModifiers() {
            return modifiers;
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Explicit(declaredAnnotations);
        }

        protected static class FieldToken {

            private final int modifiers;

            private final String name;

            private final String descriptor;

            private final List<AnnotationToken> annotationTokens;

            protected FieldToken(int modifiers, String name, String descriptor, List<AnnotationToken> annotationTokens) {
                this.modifiers = modifiers;
                this.name = name;
                this.descriptor = descriptor;
                this.annotationTokens = annotationTokens;
            }

            protected int getModifiers() {
                return modifiers;
            }

            protected String getName() {
                return name;
            }

            protected String getDescriptor() {
                return descriptor;
            }

            public List<AnnotationToken> getAnnotationTokens() {
                return annotationTokens;
            }

            private FieldDescription toFieldDescription(UnloadedTypeDescription unloadedTypeDescription) {
                return unloadedTypeDescription.new UnloadedFieldDescription(getModifiers(),
                        getName(),
                        getDescriptor(),
                        getAnnotationTokens());
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                FieldToken that = (FieldToken) other;
                return modifiers == that.modifiers
                        && annotationTokens.equals(that.annotationTokens)
                        && descriptor.equals(that.descriptor)
                        && name.equals(that.name);
            }

            @Override
            public int hashCode() {
                int result = modifiers;
                result = 31 * result + name.hashCode();
                result = 31 * result + descriptor.hashCode();
                result = 31 * result + annotationTokens.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "TypePool.UnloadedTypeDescription.FieldToken{" +
                        "modifiers=" + modifiers +
                        ", name='" + name + '\'' +
                        ", descriptor='" + descriptor + '\'' +
                        ", annotationTokens=" + annotationTokens +
                        '}';
            }
        }

        private class UnloadedFieldDescription extends FieldDescription.AbstractFieldDescription {

            private final int modifiers;

            private final String name;

            private final String fieldTypeName;

            private final List<AnnotationDescription> declaredAnnotations;

            private UnloadedFieldDescription(int modifiers,
                                             String name,
                                             String descriptor,
                                             List<AnnotationToken> annotationTokens) {
                this.modifiers = modifiers;
                this.name = name;
                Type fieldType = Type.getType(descriptor);
                fieldTypeName = fieldType.getSort() == Type.ARRAY
                        ? fieldType.getInternalName().replace('/', '.')
                        : fieldType.getClassName();
                declaredAnnotations = new ArrayList<AnnotationDescription>(annotationTokens.size());
                for (AnnotationToken annotationToken : annotationTokens) {
                    declaredAnnotations.add(annotationToken.toAnnotationDescription(typePool));
                }
            }

            @Override
            public TypeDescription getFieldType() {
                return typePool.describe(fieldTypeName);
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return new AnnotationList.Explicit(declaredAnnotations);
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public TypeDescription getDeclaringType() {
                return UnloadedTypeDescription.this;
            }

            @Override
            public int getModifiers() {
                return modifiers;
            }
        }

        protected static class MethodToken {

            private final int modifiers;

            private final String name;

            private final String descriptor;

            private final String[] exceptionName;

            private final List<AnnotationToken> annotationTokens;

            private final Map<Integer, List<AnnotationToken>> parameterAnnotationTokens;

            private final AnnotationValue<?, ?> defaultValue;

            protected MethodToken(int modifiers,
                                  String name,
                                  String descriptor,
                                  String[] exceptionName,
                                  List<AnnotationToken> annotationTokens,
                                  Map<Integer, List<AnnotationToken>> parameterAnnotationTokens,
                                  AnnotationValue<?, ?> defaultValue) {
                this.modifiers = modifiers;
                this.name = name;
                this.descriptor = descriptor;
                this.exceptionName = exceptionName;
                this.annotationTokens = annotationTokens;
                this.parameterAnnotationTokens = parameterAnnotationTokens;
                this.defaultValue = defaultValue;
            }

            protected int getModifiers() {
                return modifiers;
            }

            protected String getName() {
                return name;
            }

            protected String getDescriptor() {
                return descriptor;
            }

            protected String[] getExceptionName() {
                return exceptionName;
            }

            public List<AnnotationToken> getAnnotationTokens() {
                return annotationTokens;
            }

            public Map<Integer, List<AnnotationToken>> getParameterAnnotationTokens() {
                return parameterAnnotationTokens;
            }

            public AnnotationValue<?, ?> getDefaultValue() {
                return defaultValue;
            }

            private MethodDescription toMethodDescription(UnloadedTypeDescription unloadedTypeDescription) {
                return unloadedTypeDescription.new UnloadedMethodDescription(getModifiers(),
                        getName(),
                        getDescriptor(),
                        getExceptionName(),
                        getAnnotationTokens(),
                        getParameterAnnotationTokens(),
                        getDefaultValue());
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                MethodToken that = (MethodToken) other;
                return modifiers == that.modifiers
                        && annotationTokens.equals(that.annotationTokens)
                        && defaultValue.equals(that.defaultValue)
                        && descriptor.equals(that.descriptor)
                        && Arrays.equals(exceptionName, that.exceptionName)
                        && name.equals(that.name)
                        && parameterAnnotationTokens.equals(that.parameterAnnotationTokens);
            }

            @Override
            public int hashCode() {
                int result = modifiers;
                result = 31 * result + name.hashCode();
                result = 31 * result + descriptor.hashCode();
                result = 31 * result + Arrays.hashCode(exceptionName);
                result = 31 * result + annotationTokens.hashCode();
                result = 31 * result + parameterAnnotationTokens.hashCode();
                result = 31 * result + defaultValue.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "TypePool.UnloadedTypeDescription.MethodToken{" +
                        "modifiers=" + modifiers +
                        ", name='" + name + '\'' +
                        ", descriptor='" + descriptor + '\'' +
                        ", exceptionName=" + Arrays.toString(exceptionName) +
                        ", annotationTokens=" + annotationTokens +
                        ", parameterAnnotationTokens=" + parameterAnnotationTokens +
                        ", defaultValue=" + defaultValue +
                        '}';
            }
        }

        private class UnloadedMethodDescription extends MethodDescription.AbstractMethodDescription {

            private final int modifiers;

            private final String internalName;

            private final String returnTypeName;

            private final TypeList parameterTypes;

            private final TypeList exceptionTypes;

            private final List<AnnotationDescription> declaredAnnotations;

            private final List<List<AnnotationDescription>> declaredParameterAnnotations;

            private final AnnotationValue<?, ?> defaultValue;

            private UnloadedMethodDescription(int modifiers,
                                              String internalName,
                                              String methodDescriptor,
                                              String[] exceptionInternalName,
                                              List<AnnotationToken> annotationTokens,
                                              Map<Integer, List<AnnotationToken>> parameterAnnotationTokens,
                                              AnnotationValue<?, ?> defaultValue) {
                this.modifiers = modifiers;
                this.internalName = internalName;
                Type returnType = Type.getReturnType(methodDescriptor);
                returnTypeName = returnType.getSort() == Type.ARRAY
                        ? returnType.getDescriptor().replace('/', '.')
                        : returnType.getClassName();
                parameterTypes = new TypePoolTypeList(methodDescriptor);
                exceptionTypes = exceptionInternalName == null
                        ? new TypeList.Empty()
                        : new TypePoolTypeList(exceptionInternalName);
                declaredAnnotations = new ArrayList<AnnotationDescription>(annotationTokens.size());
                for (AnnotationToken annotationToken : annotationTokens) {
                    declaredAnnotations.add(annotationToken.toAnnotationDescription(typePool));
                }
                declaredParameterAnnotations = new ArrayList<List<AnnotationDescription>>(parameterTypes.size());
                for (int index = 0; index < parameterTypes.size(); index++) {
                    List<AnnotationToken> tokens = parameterAnnotationTokens.get(index);
                    List<AnnotationDescription> annotationDescriptions;
                    annotationDescriptions = new ArrayList<AnnotationDescription>(tokens.size());
                    for (AnnotationToken annotationToken : tokens) {
                        annotationDescriptions.add(annotationToken.toAnnotationDescription(typePool));
                    }
                    declaredParameterAnnotations.add(annotationDescriptions);
                }
                this.defaultValue = defaultValue;
            }

            @Override
            public TypeDescription getReturnType() {
                return typePool.describe(returnTypeName);
            }

            @Override
            public TypeList getParameterTypes() {
                return parameterTypes;
            }

            @Override
            public TypeList getExceptionTypes() {
                return exceptionTypes;
            }

            @Override
            public boolean isConstructor() {
                return internalName.equals(MethodDescription.CONSTRUCTOR_INTERNAL_NAME);
            }

            @Override
            public boolean isTypeInitializer() {
                return false;
            }

            @Override
            public List<AnnotationList> getParameterAnnotations() {
                return AnnotationList.Explicit.asList(declaredParameterAnnotations);
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return new AnnotationList.Explicit(declaredAnnotations);
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
            public String getInternalName() {
                return internalName;
            }

            @Override
            public TypeDescription getDeclaringType() {
                return UnloadedTypeDescription.this;
            }

            @Override
            public int getModifiers() {
                return modifiers;
            }

            @Override
            public Object getDefaultValue() {
                return defaultValue == null
                        ? null
                        : defaultValue.resolve(typePool);
            }
        }

        protected class TypePoolTypeList extends AbstractList<TypeDescription> implements TypeList {

            private final String[] name;

            private final String[] internalName;

            private final int stackSize;

            protected TypePoolTypeList(String methodDescriptor) {
                Type[] parameterType = Type.getArgumentTypes(methodDescriptor);
                name = new String[parameterType.length];
                internalName = new String[parameterType.length];
                int index = 0, stackSize = 0;
                for (Type aParameterType : parameterType) {
                    name[index] = aParameterType.getSort() == Type.ARRAY
                            ? aParameterType.getInternalName().replace('/', '.')
                            : aParameterType.getClassName();
                    internalName[index] = aParameterType.getSort() == Type.ARRAY
                            ? aParameterType.getInternalName().replace('/', '.')
                            : aParameterType.getClassName();
                    stackSize += aParameterType.getSize();
                    index++;
                }
                this.stackSize = stackSize;
            }

            protected TypePoolTypeList(String[] internalName) {
                name = new String[internalName.length];
                this.internalName = internalName;
                int index = 0;
                for (String anInternalName : internalName) {
                    name[index++] = anInternalName.replace('/', '.');
                }
                stackSize = index;
            }

            @Override
            public TypeDescription get(int index) {
                return typePool.describe(name[index]);
            }

            @Override
            public int size() {
                return name.length;
            }

            @Override
            public String[] toInternalNames() {
                return internalName.length == 0 ? null : internalName;
            }

            @Override
            public int getStackSize() {
                return stackSize;
            }

            @Override
            public TypeList subList(int fromIndex, int toIndex) {
                if (fromIndex < 0) {
                    throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
                } else if (toIndex > internalName.length) {
                    throw new IndexOutOfBoundsException("toIndex = " + toIndex);
                } else if (fromIndex > toIndex) {
                    throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
                }
                return new TypePoolTypeList(Arrays.asList(internalName)
                        .subList(fromIndex, toIndex)
                        .toArray(new String[toIndex - fromIndex]));
            }
        }

        protected static class AnnotationToken {

            private final String descriptor;

            private final Map<String, AnnotationValue<?, ?>> values;

            protected AnnotationToken(String descriptor, Map<String, AnnotationValue<?, ?>> values) {
                this.descriptor = descriptor;
                this.values = values;
            }

            public String getDescriptor() {
                return descriptor;
            }

            public Map<String, AnnotationValue<?, ?>> getValues() {
                return values;
            }

            private AnnotationDescription toAnnotationDescription(TypePool typePool) {
                return new UnloadedAnnotationDescription(typePool, descriptor, values);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                AnnotationToken that = (AnnotationToken) other;
                return descriptor.equals(that.descriptor)
                        && values.equals(that.values);
            }

            @Override
            public int hashCode() {
                int result = descriptor.hashCode();
                result = 31 * result + values.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "TypePool.UnloadedTypeDescription.AnnotationToken{" +
                        "descriptor='" + descriptor + '\'' +
                        ", values=" + values +
                        '}';
            }
        }

        private static class UnloadedAnnotationDescription extends AnnotationDescription.AbstractAnnotationDescription {

            protected final TypePool typePool;

            private final String annotationDescriptor;

            protected final Map<String, AnnotationValue<?, ?>> values;

            private UnloadedAnnotationDescription(TypePool typePool,
                                                  String annotationDescriptor,
                                                  Map<String, AnnotationValue<?, ?>> values) {
                this.typePool = typePool;
                this.annotationDescriptor = annotationDescriptor;
                this.values = values;
            }

            @Override
            public Object getValue(MethodDescription methodDescription) {
                if (!methodDescription.getDeclaringType().getDescriptor().equals(annotationDescriptor)) {
                    throw new IllegalArgumentException(methodDescription + " is not declared by " + getAnnotationType());
                }
                AnnotationValue<?, ?> annotationValue = values.get(methodDescription.getName());
                return annotationValue == null
                        ? getAnnotationType().getDeclaredMethods().filter(is(methodDescription)).getOnly().getDefaultValue()
                        : annotationValue.resolve(typePool);
            }

            @Override
            public TypeDescription getAnnotationType() {
                return typePool.describe(annotationDescriptor.substring(1, annotationDescriptor.length() - 1).replace('/', '.'));
            }

            @Override
            public <T extends Annotation> Loadable<T> prepare(Class<T> annotationType) {
                return new Loadable<T>(typePool, annotationDescriptor, values, annotationType);
            }

            private static class Loadable<S extends Annotation> extends UnloadedAnnotationDescription implements AnnotationDescription.Loadable<S> {

                private final Class<S> annotationType;

                private Loadable(TypePool typePool,
                                 String annotationDescriptor,
                                 Map<String, AnnotationValue<?, ?>> values,
                                 Class<S> annotationType) {
                    super(typePool, annotationDescriptor, values);
                    if (!Type.getDescriptor(annotationType).equals(annotationDescriptor)) {
                        throw new IllegalArgumentException(annotationType + " does not correspond to " + annotationDescriptor);
                    }
                    this.annotationType = annotationType;
                }

                @Override
                @SuppressWarnings("unchecked")
                public S load() {
                    return (S) Proxy.newProxyInstance(annotationType.getClassLoader(), new Class<?>[]{annotationType},
                            new AnnotationInvocationHandler(annotationType.getClassLoader(), annotationType, values));
                }
            }
        }

        protected static interface AnnotationValue<T, S> {

            static class Trivial<U> implements AnnotationValue<U, U> {

                private final U value;

                private final PropertyDispatcher propertyDispatcher;

                public Trivial(U value) {
                    this.value = value;
                    propertyDispatcher = PropertyDispatcher.of(value.getClass());
                }

                @Override
                public U resolve(TypePool typePool) {
                    return value;
                }

                @Override
                public U load(ClassLoader classLoader) throws ClassNotFoundException {
                    return value;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && propertyDispatcher.equals(value, ((Trivial) other).value);
                }

                @Override
                public int hashCode() {
                    return propertyDispatcher.hashCode(value);
                }

                @Override
                public String toString() {
                    return "TypePool.UnloadedTypeDescription.AnnotationValue.Trivial{" +
                            "value=" + value +
                            "propertyDispatcher=" + propertyDispatcher +
                            '}';
                }
            }

            static class ForAnnotation implements AnnotationValue<AnnotationDescription, Annotation> {

                private final AnnotationToken annotationToken;

                public ForAnnotation(AnnotationToken annotationToken) {
                    this.annotationToken = annotationToken;
                }

                @Override
                public AnnotationDescription resolve(TypePool typePool) {
                    return annotationToken.toAnnotationDescription(typePool);
                }

                @Override
                public Annotation load(ClassLoader classLoader) throws ClassNotFoundException {
                    Class<?> annotationType = classLoader.loadClass(annotationToken.getDescriptor()
                            .substring(1, annotationToken.getDescriptor().length() - 1)
                            .replace('/', '.'));
                    return (Annotation) Proxy.newProxyInstance(classLoader, new Class<?>[]{annotationType},
                            new AnnotationInvocationHandler(classLoader, annotationType, annotationToken.getValues()));
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && annotationToken.equals(((ForAnnotation) other).annotationToken);
                }

                @Override
                public int hashCode() {
                    return annotationToken.hashCode();
                }

                @Override
                public String toString() {
                    return "TypePool.UnloadedTypeDescription.AnnotationValue.ForAnnotation{" +
                            "annotationToken=" + annotationToken +
                            '}';
                }
            }

            static class ForEnumeration implements AnnotationValue<AnnotationDescription.EnumerationValue, Enum<?>> {

                private final String descriptor;

                private final String value;

                public ForEnumeration(String descriptor, String value) {
                    this.descriptor = descriptor;
                    this.value = value;
                }

                @Override
                public AnnotationDescription.EnumerationValue resolve(TypePool typePool) {
                    return new UnloadedEnumerationValue(typePool);
                }

                @Override
                @SuppressWarnings("unchecked")
                public Enum<?> load(ClassLoader classLoader) throws ClassNotFoundException {
                    return Enum.valueOf((Class) (classLoader.loadClass(descriptor.substring(1, descriptor.length() - 1)
                            .replace('/', '.'))), value);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && descriptor.equals(((ForEnumeration) other).descriptor)
                            && value.equals(((ForEnumeration) other).value);
                }

                @Override
                public int hashCode() {
                    return 31 * descriptor.hashCode() + value.hashCode();
                }

                @Override
                public String toString() {
                    return "TypePool.UnloadedTypeDescription.AnnotationValue.ForEnumeration{" +
                            "descriptor='" + descriptor + '\'' +
                            ", value='" + value + '\'' +
                            '}';
                }

                private class UnloadedEnumerationValue extends AnnotationDescription.EnumerationValue.AbstractEnumerationValue {

                    private final TypePool typePool;

                    protected UnloadedEnumerationValue(TypePool typePool) {
                        this.typePool = typePool;
                    }

                    @Override
                    public String getValue() {
                        return value;
                    }

                    @Override
                    public TypeDescription getEnumerationType() {
                        return typePool.describe(descriptor.substring(1, descriptor.length() - 1).replace('/', '.'));
                    }

                    @Override
                    public <T extends Enum<T>> T load(Class<T> type) {
                        return Enum.valueOf(type, value);
                    }
                }
            }

            static class ForType implements AnnotationValue<TypeDescription, Class<?>> {

                private static final boolean NO_INITIALIZATION = false;

                private final String name;

                public ForType(Type type) {
                    name = type.getSort() == Type.ARRAY
                            ? type.getInternalName().replace('/', '.')
                            : type.getClassName();
                }

                @Override
                public TypeDescription resolve(TypePool typePool) {
                    return typePool.describe(name);
                }

                @Override
                public Class<?> load(ClassLoader classLoader) throws ClassNotFoundException {
                    return Class.forName(name, NO_INITIALIZATION, classLoader);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && name.equals(((ForType) other).name);
                }

                @Override
                public int hashCode() {
                    return name.hashCode();
                }

                @Override
                public String toString() {
                    return "TypePool.UnloadedTypeDescription.AnnotationValue.ForType{" +
                            "name='" + name + '\'' +
                            '}';
                }
            }

            static class ForComplexArray implements AnnotationValue<Object[], Object[]> {

                protected List<AnnotationValue<?, ?>> value;

                private final ComponentTypeReference componentTypeReference;

                public ForComplexArray(List<AnnotationValue<?, ?>> value,
                                       ComponentTypeReference componentTypeReference) {
                    this.value = value;
                    this.componentTypeReference = componentTypeReference;
                }

                @Override
                public Object[] resolve(TypePool typePool) {
                    TypeDescription componentTypeDescription = typePool.describe(componentTypeReference.lookup());
                    Class<?> componentType;
                    if (componentTypeDescription.represents(Class.class)) {
                        componentType = TypeDescription.class;
                    } else if (componentTypeDescription.isAssignableTo(Enum.class)) {
                        componentType = AnnotationDescription.EnumerationValue.class;
                    } else if (componentTypeDescription.isAssignableTo(Annotation.class)) {
                        componentType = AnnotationDescription.class;
                    } else if (componentTypeDescription.represents(String.class)) {
                        componentType = String.class;
                    } else {
                        throw new IllegalStateException("Unexpected complex array component type " + componentTypeDescription);
                    }
                    Object[] array = (Object[]) Array.newInstance(componentType, value.size());
                    int index = 0;
                    for (AnnotationValue<?, ?> annotationValue : value) {
                        Array.set(array, index++, annotationValue.resolve(typePool));
                    }
                    return array;
                }

                @Override
                public Object[] load(ClassLoader classLoader) throws ClassNotFoundException {
                    Object[] array = (Object[]) Array.newInstance(classLoader.loadClass(componentTypeReference.lookup()), value.size());
                    int index = 0;
                    for (AnnotationValue<?, ?> annotationValue : value) {
                        Array.set(array, index++, annotationValue.load(classLoader));
                    }
                    return array;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && componentTypeReference.equals(((ForComplexArray) other).componentTypeReference)
                            && value.equals(((ForComplexArray) other).value);
                }

                @Override
                public int hashCode() {
                    return 31 * value.hashCode() + componentTypeReference.hashCode();
                }

                @Override
                public String toString() {
                    return "TypePool.UnloadedTypeDescription.AnnotationValue.ForComplexArray{" +
                            "value=" + value +
                            ", componentTypeReference=" + componentTypeReference +
                            '}';
                }

                public static interface ComponentTypeReference {

                    String lookup();
                }
            }

            T resolve(TypePool typePool);

            S load(ClassLoader classLoader) throws ClassNotFoundException;
        }

        protected static class AnnotationInvocationHandler implements InvocationHandler {

            private static final String HASH_CODE = "hashCode";

            private static final String EQUALS = "equals";

            private static final String TO_STRING = "toString";

            private final Class<?> annotationType;

            private final ClassLoader classLoader;

            private final LinkedHashMap<Method, AnnotationValue<?, ?>> values;

            public AnnotationInvocationHandler(ClassLoader classLoader,
                                               Class<?> annotationType,
                                               Map<String, AnnotationValue<?, ?>> values) {
                this.classLoader = classLoader;
                this.annotationType = annotationType;
                Method[] declaredMethod = annotationType.getDeclaredMethods();
                this.values = new LinkedHashMap<Method, AnnotationValue<?, ?>>(declaredMethod.length);
                TypeDescription thisType = new ForLoadedType(getClass());
                for (Method method : declaredMethod) {
                    if (!new MethodDescription.ForLoadedMethod(method).isVisibleTo(thisType)) {
                        method.setAccessible(true);
                    }
                    AnnotationValue<?, ?> annotationValue = values.get(method.getName());
                    this.values.put(method, annotationValue == null ? ResolvedAnnotationValue.of(method) : annotationValue);
                }
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
                if (method.getDeclaringClass() != annotationType) {
                    if (method.getName().equals(HASH_CODE)) {
                        return hashCodeRepresentation();
                    } else if (method.getName().equals(EQUALS)) {
                        return equalsRepresentation(proxy, arguments[0]);
                    } else if (method.getName().equals(TO_STRING)) {
                        return toStringRepresentation();
                    } else /* method.getName().equals("annotationType") */ {
                        return annotationType;
                    }
                }
                return invoke(method);
            }

            private Object invoke(Method method) throws ClassNotFoundException {
                return values.get(method).load(classLoader);
            }

            protected String toStringRepresentation() throws ClassNotFoundException {
                StringBuilder toString = new StringBuilder();
                toString.append('@');
                toString.append(annotationType.getName());
                toString.append('(');
                boolean firstMember = true;
                for (Map.Entry<Method, AnnotationValue<?, ?>> entry : values.entrySet()) {
                    if (firstMember) {
                        firstMember = false;
                    } else {
                        toString.append(", ");
                    }
                    toString.append(entry.getKey().getName());
                    toString.append('=');
                    toString.append(PropertyDispatcher.of(entry.getKey().getReturnType())
                            .toString(entry.getValue().load(classLoader)));
                }
                toString.append(')');
                return toString.toString();
            }

            private int hashCodeRepresentation() throws ClassNotFoundException {
                int hashCode = 0;
                for (Map.Entry<Method, AnnotationValue<?, ?>> entry : values.entrySet()) {
                    hashCode += (127 * entry.getKey().getName().hashCode()) ^ PropertyDispatcher.of(entry.getKey().getReturnType())
                            .hashCode(entry.getValue().load(classLoader));
                }
                return hashCode;
            }

            private boolean equalsRepresentation(Object proxy, Object other)
                    throws InvocationTargetException, IllegalAccessException, ClassNotFoundException {
                if (!annotationType.isInstance(other)) {
                    return false;
                } else if (Proxy.isProxyClass(other.getClass())) {
                    InvocationHandler invocationHandler = Proxy.getInvocationHandler(other);
                    if (invocationHandler instanceof AnnotationInvocationHandler) {
                        return invocationHandler.equals(this);
                    }
                }
                for (Method method : annotationType.getDeclaredMethods()) {
                    Object thisValue = invoke(method);
                    if (!PropertyDispatcher.of(thisValue.getClass()).equals(thisValue, method.invoke(other))) {
                        return false;
                    }
                }
                return true;
            }

            protected static class ResolvedAnnotationValue implements AnnotationValue<Void, Object> {

                protected static AnnotationValue<?, ?> of(Method method) {
                    if (method.getDefaultValue() == null) {
                        throw new IllegalStateException("Expected " + method + " to define a default value");
                    }
                    return new ResolvedAnnotationValue(method);
                }

                protected final Method method;

                private ResolvedAnnotationValue(Method method) {
                    this.method = method;
                }

                @Override
                public Object load(ClassLoader classLoader) throws ClassNotFoundException {
                    return method.getDefaultValue();
                }

                @Override
                public final Void resolve(TypePool typePool) {
                    throw new UnsupportedOperationException("Already resolved annotation values do not support resolution");
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && method.equals(((ResolvedAnnotationValue) other).method);
                }

                @Override
                public int hashCode() {
                    return method.hashCode();
                }

                @Override
                public String toString() {
                    return "TypePool.UnloadedTypeDescription.AnnotationInvocationHandler.ResolvedAnnotationValue{" +
                            "method=" + method +
                            '}';
                }
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                AnnotationInvocationHandler that = (AnnotationInvocationHandler) other;
                return annotationType.equals(that.annotationType)
                        && classLoader.equals(that.classLoader)
                        && values.equals(that.values);
            }

            @Override
            public int hashCode() {
                int result = annotationType.hashCode();
                result = 31 * result + classLoader.hashCode();
                result = 31 * result + values.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "TypePool.UnloadedTypeDescription.AnnotationInvocationHandler{" +
                        "annotationType=" + annotationType +
                        ", classLoader=" + classLoader +
                        ", values=" + values +
                        '}';
            }
        }
    }
}
