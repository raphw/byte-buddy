package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapper;
import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.FieldRegistry;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.MethodRegistry;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass.LoadedSuperclassDynamicTypeBuilder;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.isDefaultFinalize;
import static com.blogspot.mydailyjava.bytebuddy.utility.ByteBuddyCommons.*;

public class ByteBuddy {

    private static final String BYTE_BUDDY_DEFAULT_PREFIX = "ByteBuddy";

    public static class MethodAnnotationTarget extends ByteBuddy {

        protected final MethodMatcher methodMatcher;
        protected final Instrumentation instrumentation;
        protected final MethodAttributeAppender.Factory attributeAppenderFactory;

        protected MethodAnnotationTarget(ClassFormatVersion classFormatVersion,
                                         NamingStrategy namingStrategy,
                                         List<Class<?>> interfaceTypes,
                                         MethodMatcher ignoredMethods,
                                         ClassVisitorWrapper.Chain classVisitorWrapperChain,
                                         MethodRegistry methodRegistry,
                                         FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                         MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                         MethodMatcher methodMatcher,
                                         Instrumentation instrumentation,
                                         MethodAttributeAppender.Factory attributeAppenderFactory) {
            super(classFormatVersion,
                    namingStrategy,
                    interfaceTypes,
                    ignoredMethods,
                    classVisitorWrapperChain,
                    methodRegistry,
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory);
            this.methodMatcher = methodMatcher;
            this.instrumentation = instrumentation;
            this.attributeAppenderFactory = attributeAppenderFactory;
        }

        public MethodAnnotationTarget attribute(MethodAttributeAppender.Factory attributeAppenderFactory) {
            return new MethodAnnotationTarget(classFormatVersion,
                    namingStrategy,
                    interfaceTypes,
                    ignoredMethods,
                    classVisitorWrapperChain,
                    methodRegistry,
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory,
                    methodMatcher,
                    instrumentation,
                    new MethodAttributeAppender.Factory.Compound(this.attributeAppenderFactory, attributeAppenderFactory));
        }

        public MethodAnnotationTarget annotateMethod(Annotation annotation) {
            return attribute(new MethodAttributeAppender.ForAnnotation(annotation));
        }

        public MethodAnnotationTarget annotateParameter(int parameterIndex, Annotation annotation) {
            return attribute(new MethodAttributeAppender.ForAnnotation(annotation, parameterIndex));
        }

        @Override
        public ClassFormatVersion getClassFormatVersion() {
            return materialize().getClassFormatVersion();
        }

        @Override
        public NamingStrategy getNamingStrategy() {
            return materialize().getNamingStrategy();
        }

        @Override
        public List<Class<?>> getInterfaceTypes() {
            return materialize().getInterfaceTypes();
        }

        @Override
        public MethodMatcher getIgnoredMethods() {
            return materialize().getIgnoredMethods();
        }

        @Override
        public ClassVisitorWrapper.Chain getClassVisitorWrapperChain() {
            return materialize().getClassVisitorWrapperChain();
        }

        @Override
        public FieldAttributeAppender.Factory getDefaultFieldAttributeAppenderFactory() {
            return materialize().getDefaultFieldAttributeAppenderFactory();
        }

        @Override
        public MethodAttributeAppender.Factory getDefaultMethodAttributeAppenderFactory() {
            return materialize().getDefaultMethodAttributeAppenderFactory();
        }

        @Override
        public <T> DynamicType.Builder<T> subclass(Class<T> superType) {
            return materialize().subclass(superType);
        }

        @Override
        public <T> DynamicType.Builder<T> subclass(Class<T> superType, ConstructorStrategy constructorStrategy) {
            return materialize().subclass(superType, constructorStrategy);
        }

        @Override
        public ByteBuddy withClassFormatVersion(ClassFormatVersion classFormatVersion) {
            return materialize().withClassFormatVersion(classFormatVersion);
        }

        @Override
        public ByteBuddy withNamingStrategy(NamingStrategy namingStrategy) {
            return materialize().withNamingStrategy(namingStrategy);
        }

        @Override
        public ByteBuddy implementInterface(Class<?> type) {
            return materialize().implementInterface(type);
        }

        @Override
        public ByteBuddy ignoreMethods(MethodMatcher ignoredMethods) {
            return materialize().ignoreMethods(ignoredMethods);
        }

        @Override
        public ByteBuddy withClassVisitor(ClassVisitorWrapper classVisitorWrapper) {
            return materialize().withClassVisitor(classVisitorWrapper);
        }

        @Override
        public ByteBuddy withDefaultFieldAttributeAppender(FieldAttributeAppender.Factory attributeAppenderFactory) {
            return materialize().withDefaultFieldAttributeAppender(attributeAppenderFactory);
        }

        @Override
        public ByteBuddy withDefaultMethodAttributeAppender(MethodAttributeAppender.Factory attributeAppenderFactory) {
            return materialize().withDefaultMethodAttributeAppender(attributeAppenderFactory);
        }

        @Override
        public MatchedMethodInterception intercept(MethodMatcher methodMatcher) {
            return materialize().intercept(methodMatcher);
        }

        protected ByteBuddy materialize() {
            return new ByteBuddy(classFormatVersion,
                    namingStrategy,
                    interfaceTypes,
                    ignoredMethods,
                    classVisitorWrapperChain,
                    methodRegistry.prepend(new MethodRegistry.LatentMethodMatcher.Simple(methodMatcher),
                            instrumentation,
                            attributeAppenderFactory),
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory);
        }
    }

    public class MatchedMethodInterception {

        protected final MethodMatcher methodMatcher;

        public MatchedMethodInterception(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }


        public MethodAnnotationTarget intercept(Instrumentation instrumentation) {
            return new MethodAnnotationTarget(classFormatVersion,
                    namingStrategy,
                    interfaceTypes,
                    ignoredMethods,
                    classVisitorWrapperChain,
                    methodRegistry,
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory,
                    methodMatcher,
                    instrumentation,
                    MethodAttributeAppender.NoOp.INSTANCE);
        }

        public MethodAnnotationTarget withoutCode() {
            return intercept(Instrumentation.ForAbstractMethod.INSTANCE);
        }
    }

    protected final ClassFormatVersion classFormatVersion;
    protected final NamingStrategy namingStrategy;
    protected final List<Class<?>> interfaceTypes;
    protected final MethodMatcher ignoredMethods;
    protected final ClassVisitorWrapper.Chain classVisitorWrapperChain;
    protected final MethodRegistry methodRegistry;
    protected final FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory;
    protected final MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory;


    public ByteBuddy() {
        this(ClassFormatVersion.forCurrentJavaVersion());
    }

    public ByteBuddy(ClassFormatVersion classFormatVersion) {
        this.classFormatVersion = classFormatVersion;
        namingStrategy = new NamingStrategy.SuffixingRandom(BYTE_BUDDY_DEFAULT_PREFIX);
        interfaceTypes = Collections.emptyList();
        ignoredMethods = isDefaultFinalize();
        classVisitorWrapperChain = new ClassVisitorWrapper.Chain();
        methodRegistry = new MethodRegistry.Default();
        defaultFieldAttributeAppenderFactory = FieldAttributeAppender.NoOp.INSTANCE;
        defaultMethodAttributeAppenderFactory = MethodAttributeAppender.NoOp.INSTANCE;
    }

    protected ByteBuddy(ClassFormatVersion classFormatVersion,
                        NamingStrategy namingStrategy,
                        List<Class<?>> interfaceTypes,
                        MethodMatcher ignoredMethods,
                        ClassVisitorWrapper.Chain classVisitorWrapperChain,
                        MethodRegistry methodRegistry,
                        FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                        MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory) {
        this.classFormatVersion = classFormatVersion;
        this.namingStrategy = namingStrategy;
        this.interfaceTypes = interfaceTypes;
        this.ignoredMethods = ignoredMethods;
        this.classVisitorWrapperChain = classVisitorWrapperChain;
        this.methodRegistry = methodRegistry;
        this.defaultFieldAttributeAppenderFactory = defaultFieldAttributeAppenderFactory;
        this.defaultMethodAttributeAppenderFactory = defaultMethodAttributeAppenderFactory;
    }

    public ClassFormatVersion getClassFormatVersion() {
        return classFormatVersion;
    }

    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public List<Class<?>> getInterfaceTypes() {
        return interfaceTypes;
    }

    public MethodMatcher getIgnoredMethods() {
        return ignoredMethods;
    }

    public ClassVisitorWrapper.Chain getClassVisitorWrapperChain() {
        return classVisitorWrapperChain;
    }

    public FieldAttributeAppender.Factory getDefaultFieldAttributeAppenderFactory() {
        return defaultFieldAttributeAppenderFactory;
    }

    public MethodAttributeAppender.Factory getDefaultMethodAttributeAppenderFactory() {
        return defaultMethodAttributeAppenderFactory;
    }

    public <T> DynamicType.Builder<T> subclass(Class<T> superType) {
        return subclass(superType, ConstructorStrategy.Default.IMITATE_SUPER_TYPE);
    }

    public <T> DynamicType.Builder<T> subclass(Class<T> superType, ConstructorStrategy constructorStrategy) {
        Class<?> actualSuperType = superType;
        List<Class<?>> interfaceTypes = this.interfaceTypes;
        if (nonNull(superType).isInterface()) {
            actualSuperType = Object.class;
            interfaceTypes = join(superType, interfaceTypes);
        }
        return new LoadedSuperclassDynamicTypeBuilder<T>(classFormatVersion,
                namingStrategy,
                actualSuperType,
                interfaceTypes,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                TypeAttributeAppender.NoOp.INSTANCE,
                ignoredMethods,
                classVisitorWrapperChain,
                new FieldRegistry.Default(),
                methodRegistry,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                nonNull(constructorStrategy));
    }

    public ByteBuddy withClassFormatVersion(ClassFormatVersion classFormatVersion) {
        return new ByteBuddy(classFormatVersion,
                namingStrategy,
                interfaceTypes,
                ignoredMethods,
                classVisitorWrapperChain,
                methodRegistry,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    public ByteBuddy withNamingStrategy(NamingStrategy namingStrategy) {
        return new ByteBuddy(classFormatVersion,
                nonNull(namingStrategy),
                interfaceTypes,
                ignoredMethods,
                classVisitorWrapperChain,
                methodRegistry,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    public ByteBuddy implementInterface(Class<?> type) {
        return new ByteBuddy(classFormatVersion,
                namingStrategy,
                join(interfaceTypes, isInterface(type)),
                ignoredMethods,
                classVisitorWrapperChain,
                methodRegistry,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    public ByteBuddy ignoreMethods(MethodMatcher ignoredMethods) {
        return new ByteBuddy(classFormatVersion,
                namingStrategy,
                interfaceTypes,
                nonNull(ignoredMethods),
                classVisitorWrapperChain,
                methodRegistry,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    public ByteBuddy withClassVisitor(ClassVisitorWrapper classVisitorWrapper) {
        return new ByteBuddy(classFormatVersion,
                namingStrategy,
                interfaceTypes,
                ignoredMethods,
                classVisitorWrapperChain.append(nonNull(classVisitorWrapper)),
                methodRegistry,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    public ByteBuddy withDefaultFieldAttributeAppender(FieldAttributeAppender.Factory attributeAppenderFactory) {
        return new ByteBuddy(classFormatVersion,
                namingStrategy,
                interfaceTypes,
                ignoredMethods,
                classVisitorWrapperChain,
                methodRegistry,
                nonNull(attributeAppenderFactory),
                defaultMethodAttributeAppenderFactory);
    }

    public ByteBuddy withDefaultMethodAttributeAppender(MethodAttributeAppender.Factory attributeAppenderFactory) {
        return new ByteBuddy(classFormatVersion,
                namingStrategy,
                interfaceTypes,
                ignoredMethods,
                classVisitorWrapperChain,
                methodRegistry,
                defaultFieldAttributeAppenderFactory,
                nonNull(attributeAppenderFactory));
    }

    public MatchedMethodInterception intercept(MethodMatcher methodMatcher) {
        return new MatchedMethodInterception(methodMatcher);
    }
}
