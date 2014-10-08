package net.bytebuddy.pool;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.field.FieldList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.objectweb.asm.*;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.hasMethodDescriptor;
import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.named;

public interface TypePool {

    TypeDescription describe(String name);

    void clear();

    abstract static class AbstractBase implements TypePool {

        private static final String ARRAY_SYMBOL = "[]";

        protected static final Map<String, TypeDescription> PRIMITIVE_TYPES;

        static {
            Map<String, TypeDescription> primitiveTypes = new HashMap<String, TypeDescription>();
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
            }
            PRIMITIVE_TYPES = Collections.unmodifiableMap(primitiveTypes);
        }

        @Override
        public TypeDescription describe(String name) {
            int arity = 0;
            while (name.endsWith(ARRAY_SYMBOL)) {
                arity++;
                name = name.substring(0, name.length() - 2);
            }
            TypeDescription typeDescription = PRIMITIVE_TYPES.get(name);
            return TypeDescription.ArrayProjection.of(typeDescription == null ? doDescribe(name) : typeDescription, arity);
        }

        protected abstract TypeDescription doDescribe(String name);
    }

    static class Default extends AbstractBase {

        private static final int ASM_VERSION = Opcodes.ASM5;

        private static final int ASM_MANUAL = 0;

        public static TypePool ofClassPath() {
            return new Default(new TypeSourceLocator.ForClassLoader(ClassLoader.getSystemClassLoader()));
        }

        private final TypeSourceLocator typeSourceLocator;

        private final ConcurrentHashMap<String, TypeDescription> typeCache;

        public Default(TypeSourceLocator typeSourceLocator) {
            this.typeSourceLocator = typeSourceLocator;
            typeCache = new ConcurrentHashMap<String, TypeDescription>();
        }

        protected TypeDescription doDescribe(String name) {
            TypeDescription typeDescription = typeCache.get(name);
            if (typeDescription != null) {
                return typeDescription;
            }
            byte[] binaryRepresentation = typeSourceLocator.locate(name);
            if (binaryRepresentation == null) {
                throw new IllegalArgumentException("Cannot locate " + name + " using " + typeSourceLocator);
            }
            typeDescription = parse(binaryRepresentation);
            TypeDescription cachedDescription = typeCache.putIfAbsent(name, typeDescription);
            return cachedDescription == null ? typeCache.get(name) : cachedDescription;
        }

        private TypeDescription parse(byte[] binaryRepresentation) {
            ClassReader classReader = new ClassReader(binaryRepresentation);
            TypeInterpreter typeInterpreter = new TypeInterpreter();
            classReader.accept(typeInterpreter, ASM_MANUAL);
            return typeInterpreter.toTypeDescription();
        }

        @Override
        public void clear() {
            typeCache.clear();
        }

        private class TypeInterpreter extends ClassVisitor {

            private int modifiers;

            private String internalName;

            private String superTypeName;

            private String[] interfaceName;

            private boolean anonymousType;

            private UnloadedTypeDescription.DeclarationContext declarationContext;

            private final List<UnloadedTypeDescription.AnnotationToken> annotationTokens;

            private final List<UnloadedTypeDescription.FieldToken> fieldTokens;

            private final List<UnloadedTypeDescription.MethodToken> methodTokens;

            private TypeInterpreter() {
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
                }
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                return new AnnotationExtractor(Default.this,
                        new OnTypeCollector(descriptor),
                        new AnnotationExtractor.ComponentTypeLookup.ForAnnotationProperty(Default.this, descriptor));
            }

            private class OnTypeCollector implements AnnotationExtractor.Registrant {

                private final String descriptor;

                private final LinkedHashMap<String, UnloadedTypeDescription.AnnotationValue<?, ?>> values;

                private OnTypeCollector(String descriptor) {
                    this.descriptor = descriptor;
                    values = new LinkedHashMap<String, UnloadedTypeDescription.AnnotationValue<?, ?>>();
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
                return new FieldAnnotationCollector(modifiers, internalName, descriptor);
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
                return new MethodAnnotationCollector(modifiers, internalName, descriptor, exceptionName);
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

            private class FieldAnnotationCollector extends FieldVisitor {

                private final int modifiers;

                private final String internalName;

                private final String descriptor;

                private final List<UnloadedTypeDescription.AnnotationToken> annotationTokens;

                private FieldAnnotationCollector(int modifiers, String internalName, String descriptor) {
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
                            new AnnotationExtractor.ComponentTypeLookup.ForAnnotationProperty(Default.this, descriptor));
                }

                private class OnFieldCollector implements AnnotationExtractor.Registrant {

                    private final String descriptor;

                    private final LinkedHashMap<String, UnloadedTypeDescription.AnnotationValue<?, ?>> values;

                    private OnFieldCollector(String descriptor) {
                        this.descriptor = descriptor;
                        values = new LinkedHashMap<String, UnloadedTypeDescription.AnnotationValue<?, ?>>();
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

            private class MethodAnnotationCollector extends MethodVisitor implements AnnotationExtractor.Registrant {

                private final int modifiers;

                private final String internalName;

                private final String descriptor;

                private final String[] exceptionName;

                private final List<UnloadedTypeDescription.AnnotationToken> annotationTokens;

                private final Map<Integer, List<UnloadedTypeDescription.AnnotationToken>> parameterAnnotationTokens;

                private UnloadedTypeDescription.AnnotationValue<?, ?> defaultValue;

                private MethodAnnotationCollector(int modifiers,
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
                            new AnnotationExtractor.ComponentTypeLookup.ForAnnotationProperty(Default.this, descriptor));
                }

                @Override
                public AnnotationVisitor visitParameterAnnotation(int index, String descriptor, boolean visible) {
                    return new AnnotationExtractor(Default.this,
                            new OnMethodParameterCollector(descriptor, index),
                            new AnnotationExtractor.ComponentTypeLookup.ForAnnotationProperty(Default.this, descriptor));
                }

                @Override
                public AnnotationVisitor visitAnnotationDefault() {
                    return new AnnotationExtractor(Default.this,
                            this,
                            new AnnotationExtractor.ComponentTypeLookup.FixedArrayReturnType(descriptor));
                }

                @Override
                public void register(String ignored, UnloadedTypeDescription.AnnotationValue<?, ?> annotationValue) {
                    defaultValue = annotationValue;
                }

                @Override
                public void onComplete() {
                    /* do nothing */
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

                    private final LinkedHashMap<String, UnloadedTypeDescription.AnnotationValue<?, ?>> values;

                    private OnMethodCollector(String descriptor) {
                        this.descriptor = descriptor;
                        values = new LinkedHashMap<String, UnloadedTypeDescription.AnnotationValue<?, ?>>();
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

                    private final LinkedHashMap<String, UnloadedTypeDescription.AnnotationValue<?, ?>> values;

                    private OnMethodParameterCollector(String descriptor, int index) {
                        this.descriptor = descriptor;
                        this.index = index;
                        values = new LinkedHashMap<String, UnloadedTypeDescription.AnnotationValue<?, ?>>();
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

            private final ComponentTypeLookup componentTypeLookup;

            public AnnotationExtractor(TypePool typePool, Registrant registrant, ComponentTypeLookup componentTypeLookup) {
                super(ASM_VERSION);
                this.typePool = typePool;
                this.registrant = registrant;
                this.componentTypeLookup = componentTypeLookup;
            }

            @Override
            public void visit(String name, Object value) {
                UnloadedTypeDescription.AnnotationValue<?, ?> annotationValue;
                if (value instanceof Type) {
                    annotationValue = new UnloadedTypeDescription.AnnotationValue.TypeToken((Type) value);
                } else if (value.getClass().isArray()) {
                    annotationValue = new UnloadedTypeDescription.AnnotationValue.ArrayToken.Trivial<Object>(value);
                } else {
                    annotationValue = new UnloadedTypeDescription.AnnotationValue.Trivial<Object>(value);
                }
                registrant.register(name, annotationValue);
            }

            @Override
            public void visitEnum(String name, String descriptor, String value) {
                registrant.register(name, new UnloadedTypeDescription.AnnotationValue.Enumeration(descriptor, value));
            }

            @Override
            public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                return new AnnotationExtractor(typePool,
                        new AnnotationLookup(name, descriptor),
                        new ComponentTypeLookup.ForAnnotationProperty(typePool, descriptor));
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                return new AnnotationExtractor(typePool,
                        new ArrayLookup(name, componentTypeLookup.lookup(name)),
                        ComponentTypeLookup.Illegal.INSTANCE);
            }

            @Override
            public void visitEnd() {
                registrant.onComplete();
            }

            private class ArrayLookup implements Registrant {

                private final String name;

                private final String componentTypeDescriptor;

                private final LinkedList<UnloadedTypeDescription.AnnotationValue<?, ?>> values;

                private ArrayLookup(String name, String componentTypeDescriptor) {
                    this.name = name;
                    this.componentTypeDescriptor = componentTypeDescriptor;
                    values = new LinkedList<UnloadedTypeDescription.AnnotationValue<?, ?>>();
                }

                @Override
                public void register(String ignored, UnloadedTypeDescription.AnnotationValue<?, ?> annotationValue) {
                    values.add(annotationValue);
                }

                @Override
                public void onComplete() {
                    registrant.register(name, new UnloadedTypeDescription.AnnotationValue.ArrayToken.Complex(values, componentTypeDescriptor));
                }
            }

            private class AnnotationLookup implements Registrant {

                private final String name;

                private final String descriptor;

                private final LinkedHashMap<String, UnloadedTypeDescription.AnnotationValue<?, ?>> values;

                private AnnotationLookup(String name, String descriptor) {
                    this.name = name;
                    this.descriptor = descriptor;
                    values = new LinkedHashMap<String, UnloadedTypeDescription.AnnotationValue<?, ?>>();
                }

                @Override
                public void register(String name, UnloadedTypeDescription.AnnotationValue<?, ?> annotationValue) {
                    values.put(name, annotationValue);
                }

                @Override
                public void onComplete() {
                    registrant.register(name, new UnloadedTypeDescription.AnnotationValue
                            .Annotation(new UnloadedTypeDescription.AnnotationToken(descriptor, values)));
                }
            }

            public static interface ComponentTypeLookup {

                static class ForAnnotationProperty implements ComponentTypeLookup {

                    private final TypePool typePool;

                    private final String annotationName;

                    public ForAnnotationProperty(TypePool typePool, String annotationDescriptor) {
                        this.typePool = typePool;
                        annotationName = annotationDescriptor.replace('/', '.');
                    }

                    @Override
                    public String lookup(String name) {
                        return typePool.describe(annotationName)
                                .getDeclaredMethods()
                                .filter(named(name))
                                .getOnly()
                                .getReturnType()
                                .getComponentType()
                                .getName();
                    }
                }

                static class FixedArrayReturnType implements ComponentTypeLookup {

                    private final String componentType;

                    public FixedArrayReturnType(String methodDescriptor) {
                        String arrayType = Type.getMethodType(methodDescriptor).getReturnType().getClassName();
                        componentType = arrayType.substring(0, arrayType.length() - 2);
                    }

                    @Override
                    public String lookup(String name) {
                        return componentType;
                    }
                }

                static enum Illegal implements ComponentTypeLookup {

                    INSTANCE;

                    @Override
                    public String lookup(String name) {
                        throw new IllegalStateException("Unexpected lookup of component type");
                    }
                }

                String lookup(String name);
            }

            public static interface Registrant {

                void register(String name, UnloadedTypeDescription.AnnotationValue<?, ?> annotationValue);

                void onComplete();
            }
        }
    }

    static class UnloadedTypeDescription extends TypeDescription.AbstractTypeDescription.OfSimpleType {

        private final TypePool typePool;

        private final int modifiers;

        private final String name;

        private final String superTypeName;

        private final String[] interfaceName;

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
                public boolean isDeclaredInType() {
                    return true;
                }

                @Override
                public boolean isDeclaredInMethod() {
                    return false;
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
                            .filter(named(methodName).and(hasMethodDescriptor(methodDescriptor))).getOnly();
                }

                @Override
                public TypeDescription getEnclosingType(TypePool typePool) {
                    return typePool.describe(name);
                }

                @Override
                public boolean isDeclaredInType() {
                    return false;
                }

                @Override
                public boolean isDeclaredInMethod() {
                    return true;
                }
            }

            MethodDescription getEnclosingMethod(TypePool typePool);

            TypeDescription getEnclosingType(TypePool typePool);

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
            if (interfaceName != null) {
                this.interfaceName = new String[interfaceName.length];
                int index = 0;
                for (String anInterfaceName : interfaceName) {
                    this.interfaceName[index++] = anInterfaceName.replace('/', '.');
                }
            } else {
                this.interfaceName = null;
            }
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
            return superTypeName == null
                    ? null
                    : typePool.describe(superTypeName);
        }

        @Override
        public TypeList getInterfaces() {
            return interfaceName == null
                    ? new TypeList.Empty()
                    : new TypePoolTypeList(interfaceName);
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
            return name;
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
            return declarationContext.isDeclaredInMethod()
                    ? null
                    : declarationContext.getEnclosingType(typePool);
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
                fieldTypeName = Type.getType(descriptor).getClassName();
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
                returnTypeName = Type.getReturnType(methodDescriptor).getClassName();
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
                    if (tokens == null) {
                        annotationDescriptions = new AnnotationList.Empty();
                    } else {
                        annotationDescriptions = new ArrayList<AnnotationDescription>(tokens.size());
                        for (AnnotationToken annotationToken : tokens) {
                            annotationDescriptions.add(annotationToken.toAnnotationDescription(typePool));
                        }
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
                        ? super.getDefaultValue()
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
                    name[index++] = aParameterType.getClassName();
                    internalName[index++] = aParameterType.getInternalName();
                    stackSize += aParameterType.getSize();
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
        }

        protected static class AnnotationToken {

            private final String descriptor;

            private final LinkedHashMap<String, AnnotationValue<?, ?>> values;

            protected AnnotationToken(String descriptor, LinkedHashMap<String, AnnotationValue<?, ?>> values) {
                this.descriptor = descriptor;
                this.values = values;
            }

            public String getDescriptor() {
                return descriptor;
            }

            public LinkedHashMap<String, AnnotationValue<?, ?>> getValues() {
                return values;
            }

            private AnnotationDescription toAnnotationDescription(TypePool typePool) {
                return new UnloadedAnnotationDescription(typePool, descriptor, values);
            }
        }

        private static class UnloadedAnnotationDescription extends AnnotationDescription.AbstractAnnotationDescription {

            protected final TypePool typePool;

            private final String annotationDescriptor;

            protected final LinkedHashMap<String, AnnotationValue<?, ?>> values;

            private UnloadedAnnotationDescription(TypePool typePool,
                                                  String annotationDescriptor,
                                                  LinkedHashMap<String, AnnotationValue<?, ?>> values) {
                this.typePool = typePool;
                this.annotationDescriptor = annotationDescriptor;
                this.values = values;
                // TODO: Use type pool to look up annotation default values that are not contained in the values map.
            }

            @Override
            public Object getValue(MethodDescription methodDescription) {
                AnnotationValue<?, ?> annotationValue = values.get(methodDescription.getName());
                return annotationValue == null
                        ? methodDescription.getDefaultValue()
                        : annotationValue.resolve(typePool);
            }

            @Override
            public TypeDescription getAnnotationType() {
                return typePool.describe(annotationDescriptor);
            }

            @Override
            public <T extends Annotation> Loadable<T> prepare(Class<T> annotationType) {
                return new Loadable<T>(typePool, annotationDescriptor, values, annotationType);
            }

            private static class Loadable<S extends Annotation> extends UnloadedAnnotationDescription
                    implements AnnotationDescription.Loadable<S> {

                private final Class<S> annotationType;

                private Loadable(TypePool typePool,
                                 String annotationDescriptor,
                                 LinkedHashMap<String, AnnotationValue<?, ?>> values,
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

                public Trivial(U value) {
                    this.value = value;
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
                public String toStringRepresentation() {
                    return value.toString();
                }

                @Override
                public int hashCodeRepresentation(ClassLoader classLoader) {
                    return value.hashCode();
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && value.equals(((Trivial) other).value);
                }

                @Override
                public int hashCode() {
                    return value.hashCode();
                }
            }

            static class Annotation implements AnnotationValue<AnnotationDescription, Annotation> {

                private final AnnotationToken annotationToken;

                public Annotation(AnnotationToken annotationToken) {
                    this.annotationToken = annotationToken;
                }

                @Override
                public AnnotationDescription resolve(TypePool typePool) {
                    return annotationToken.toAnnotationDescription(typePool);
                }

                @Override
                public Annotation load(ClassLoader classLoader) throws ClassNotFoundException {
                    Class<?> annotationType = classLoader.loadClass(annotationToken.getDescriptor().replace('/', '.'));
                    return (Annotation) Proxy.newProxyInstance(classLoader, new Class<?>[]{annotationType},
                            new AnnotationInvocationHandler(classLoader, annotationType, annotationToken.getValues()));
                }

                @Override
                public String toStringRepresentation() {
                    StringBuilder toString = new StringBuilder();
                    toString.append('@');
                    toString.append(annotationToken.getDescriptor().replace('/', '.'));
                    toString.append('(');
                    boolean firstMember = true;
                    for (Map.Entry<String, AnnotationValue<?, ?>> entry : annotationToken.getValues().entrySet()) {
                        if (firstMember) {
                            firstMember = false;
                        } else {
                            toString.append(", ");
                        }
                        toString.append(entry.getKey());
                        toString.append('=');
                        toString.append(entry.getValue().toStringRepresentation());
                    }
                    toString.append(')');
                    return toString.toString();
                }

                @Override
                public int hashCodeRepresentation(ClassLoader classLoader) throws ClassNotFoundException {
                    int hashCode = 0;
                    for (Map.Entry<String, AnnotationValue<?, ?>> entry : annotationToken.getValues().entrySet()) {
                        hashCode += (127 * entry.getKey().hashCode()) ^ entry.getValue().hashCodeRepresentation(classLoader);
                    }
                    return hashCode;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && annotationToken.equals(((Annotation) other).annotationToken);
                }

                @Override
                public int hashCode() {
                    return annotationToken.hashCode();
                }
            }

            static class Enumeration implements AnnotationValue<AnnotationDescription.EnumerationValue, Enum<?>> {

                private final String descriptor;

                private final String value;

                public Enumeration(String descriptor, String value) {
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
                    return Enum.valueOf((Class) (classLoader.loadClass(descriptor.replace('/', '.'))), value);
                }

                @Override
                public String toStringRepresentation() {
                    return value;
                }

                @Override
                public int hashCodeRepresentation(ClassLoader classLoader) throws ClassNotFoundException {
                    return load(classLoader).hashCode();
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && descriptor.equals(((Enumeration) other).descriptor)
                            && value.equals(((Enumeration) other).value);
                }

                @Override
                public int hashCode() {
                    return 31 * descriptor.hashCode() + value.hashCode();
                }

                private class UnloadedEnumerationValue implements AnnotationDescription.EnumerationValue {

                    private final TypePool typePool;

                    private UnloadedEnumerationValue(TypePool typePool) {
                        this.typePool = typePool;
                    }

                    @Override
                    public String getValue() {
                        return value;
                    }

                    @Override
                    public TypeDescription getEnumerationType() {
                        return typePool.describe(descriptor);
                    }

                    @Override
                    public <T extends Enum<T>> Loadable<T> prepare(Class<T> type) {
                        return new LoadableUnloadedEnumerationValue<T>(typePool, type);
                    }
                }

                private class LoadableUnloadedEnumerationValue<S extends Enum<S>> extends UnloadedEnumerationValue
                        implements AnnotationDescription.EnumerationValue.Loadable<S> {

                    private final Class<S> type;

                    private LoadableUnloadedEnumerationValue(TypePool typePool, Class<S> type) {
                        super(typePool);
                        this.type = type;
                    }

                    @Override
                    public S load() {
                        return Enum.valueOf(type, value);
                    }
                }
            }

            static class TypeToken implements AnnotationValue<TypeDescription, Class<?>> {

                private final String name;

                public TypeToken(Type type) {
                    name = type.getClassName();
                }

                @Override
                public TypeDescription resolve(TypePool typePool) {
                    return typePool.describe(name);
                }

                @Override
                public Class<?> load(ClassLoader classLoader) throws ClassNotFoundException {
                    return classLoader.loadClass(name);
                }

                @Override
                public String toStringRepresentation() {
                    return name;
                }

                @Override
                public int hashCodeRepresentation(ClassLoader classLoader) throws ClassNotFoundException {
                    return load(classLoader).hashCode();
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && name.equals(((TypeToken) other).name);
                }

                @Override
                public int hashCode() {
                    return name.hashCode();
                }
            }

            abstract static class ArrayToken<U, V, W> implements AnnotationValue<U, V> {

                protected W value;

                protected ArrayToken(W value) {
                    this.value = value;
                }

                public static class Trivial<U> extends ArrayToken<U, U, U> {

                    public Trivial(U value) {
                        super(value);
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
                    public String toStringRepresentation() {
                        Class<?> componentType = value.getClass().getComponentType();
                        if (componentType == boolean.class) {
                            return Arrays.toString((boolean[]) value);
                        } else if (componentType == byte.class) {
                            return Arrays.toString((byte[]) value);
                        } else if (componentType == short.class) {
                            return Arrays.toString((short[]) value);
                        } else if (componentType == char.class) {
                            return Arrays.toString((char[]) value);
                        } else if (componentType == int.class) {
                            return Arrays.toString((int[]) value);
                        } else if (componentType == long.class) {
                            return Arrays.toString((long[]) value);
                        } else if (componentType == float.class) {
                            return Arrays.toString((float[]) value);
                        } else if (componentType == double.class) {
                            return Arrays.toString((double[]) value);
                        } else {
                            return Arrays.toString((Object[]) value);
                        }
                    }

                    @Override
                    public int hashCodeRepresentation(ClassLoader classLoader) {
                        Class<?> componentType = value.getClass().getComponentType();
                        if (componentType == boolean.class) {
                            return Arrays.hashCode((boolean[]) value);
                        } else if (componentType == byte.class) {
                            return Arrays.hashCode((byte[]) value);
                        } else if (componentType == short.class) {
                            return Arrays.hashCode((short[]) value);
                        } else if (componentType == char.class) {
                            return Arrays.hashCode((char[]) value);
                        } else if (componentType == int.class) {
                            return Arrays.hashCode((int[]) value);
                        } else if (componentType == long.class) {
                            return Arrays.hashCode((long[]) value);
                        } else if (componentType == float.class) {
                            return Arrays.hashCode((float[]) value);
                        } else if (componentType == double.class) {
                            return Arrays.hashCode((double[]) value);
                        } else {
                            return Arrays.hashCode((Object[]) value);
                        }
                    }
                }

                public static class Complex extends ArrayToken<Object[], Object[], List<AnnotationValue<?, ?>>> {

                    private final String componentTypeName;

                    public Complex(List<AnnotationValue<?, ?>> value, String componentTypeDescriptor) {
                        super(value);
                        componentTypeName = Type.getType(componentTypeDescriptor).getClassName();
                    }

                    @Override
                    public Object[] resolve(TypePool typePool) {
                        TypeDescription componentTypeDescription = typePool.describe(componentTypeName);
                        Class<?> componentType;
                        if (componentTypeDescription.represents(Class.class)) {
                            componentType = TypeDescription.class;
                        } else if (componentTypeDescription.isAssignableFrom(Enum.class)) {
                            componentType = AnnotationDescription.EnumerationValue.class;
                        } else if (componentTypeDescription.isAssignableFrom(Annotation.class)) {
                            componentType = AnnotationDescription.class;
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
                        Object[] array = (Object[]) Array.newInstance(classLoader.loadClass(componentTypeName), value.size());
                        int index = 0;
                        for (AnnotationValue<?, ?> annotationValue : value) {
                            Array.set(array, index++, annotationValue.load(classLoader));
                        }
                        return array;
                    }

                    @Override
                    public String toStringRepresentation() {
                        StringBuilder toString = new StringBuilder(componentTypeName).append('[');
                        boolean first = true;
                        for (AnnotationValue<?, ?> annotationValue : value) {
                            if (first) {
                                first = false;
                            } else {
                                toString.append(", ");
                            }
                            toString.append(annotationValue.toStringRepresentation());
                        }
                        return toString.append(']').toString();
                    }

                    @Override
                    public int hashCodeRepresentation(ClassLoader classLoader) throws ClassNotFoundException {
                        int hashCode = 1;
                        for (AnnotationValue<?, ?> annotationValue : value) {
                            hashCode = 31 * hashCode + annotationValue.hashCodeRepresentation(classLoader);
                        }
                        return hashCode;
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && componentTypeName.equals(((Complex) other).componentTypeName);
                    }

                    @Override
                    public int hashCode() {
                        return componentTypeName.hashCode();
                    }
                }
            }

            T resolve(TypePool typePool);

            S load(ClassLoader classLoader) throws ClassNotFoundException;

            String toStringRepresentation();

            int hashCodeRepresentation(ClassLoader classLoader) throws ClassNotFoundException;
        }

        protected static class AnnotationInvocationHandler implements InvocationHandler {

            private static final String HASH_CODE = "hashCode";

            private static final String EQUALS = "equals";

            private static final String TO_STRING = "toString";

            private final Class<?> annotationType;

            private final ClassLoader classLoader;

            private final LinkedHashMap<String, AnnotationValue<?, ?>> values;

            public AnnotationInvocationHandler(ClassLoader classLoader,
                                               Class<?> annotationType,
                                               LinkedHashMap<String, AnnotationValue<?, ?>> values) {
                this.classLoader = classLoader;
                this.annotationType = annotationType;
                this.values = values;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
                if (method.getDeclaringClass() != annotationType) {
                    if (method.getName().equals(HASH_CODE)) {
                        return hashCodeRepresentation();
                    } else if (method.getName().equals(EQUALS)) {
                        return equalsRepresentation(arguments[0]);
                    } else if (method.getName().equals(TO_STRING)) {
                        return toStringRepresentation();
                    } else /* method.getName().equals("annotationType") */ {
                        return annotationType;
                    }
                }
                return invoke(method);
            }

            private Object invoke(Method method) throws ClassNotFoundException {
                AnnotationValue<?, ?> annotationValue = values.get(method.getName());
                return annotationValue == null
                        ? method.getDefaultValue()
                        : annotationValue.load(classLoader);
            }

            protected String toStringRepresentation() throws ClassNotFoundException {
                StringBuilder toString = new StringBuilder();
                toString.append('@');
                toString.append(annotationType.getName());
                toString.append('(');
                boolean firstMember = true;
                for (Map.Entry<String, AnnotationValue<?, ?>> entry : values.entrySet()) {
                    if (firstMember) {
                        firstMember = false;
                    } else {
                        toString.append(", ");
                    }
                    toString.append(entry.getKey());
                    toString.append('=');
                    toString.append(entry.getValue().toStringRepresentation());
                }
                toString.append(')');
                return toString.toString();
            }

            private int hashCodeRepresentation() throws ClassNotFoundException {
                int hashCode = 0;
                for (Map.Entry<String, AnnotationValue<?, ?>> entry : values.entrySet()) {
                    hashCode += (127 * entry.getKey().hashCode()) ^ entry.getValue().hashCodeRepresentation(classLoader);
                }
                return hashCode;
            }

            private boolean equalsRepresentation(Object other) throws InvocationTargetException, IllegalAccessException, ClassNotFoundException {
                if (!annotationType.isInstance(other)) {
                    return false;
                }
                for (Method method : annotationType.getDeclaredMethods()) {
                    if (!invoke(method).equals(method.invoke(other))) {
                        return false;
                    }
                }
                return true;
            }
        }
    }
}
