package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapper;
import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.FieldRegistry;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.MethodRegistry;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass.SubclassDynamicTypeBuilder;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.List;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.isDefaultFinalize;
import static com.blogspot.mydailyjava.bytebuddy.utility.UserInput.*;

public class ByteBuddy {

    private static final String JAVA_VERSION = "java.version";
    private static final String BYTE_BUDDY_DEFAULT_PREFIX = "ByteBuddy";

    private final ClassFormatVersion classFormatVersion;
    private final NamingStrategy namingStrategy;
    private final List<Class<?>> interfaceTypes;
    private final MethodMatcher ignoredMethods;
    private final ClassVisitorWrapper.Chain classVisitorWrapperChain;
    private final MethodRegistry methodRegistry;
    private final FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory;
    private final MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory;

    public ByteBuddy() {
        classFormatVersion = ClassFormatVersion.forJavaVersion(Integer.parseInt(System.getProperty(JAVA_VERSION)));
        namingStrategy = new NamingStrategy.PrefixingRandom(BYTE_BUDDY_DEFAULT_PREFIX);
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
        return subclass(superType, ConstructorStrategy.IMITATE_SUPER_TYPE);
    }

    public <T> DynamicType.Builder<T> subclass(Class<T> superType, ConstructorStrategy constructorStrategy) {
        return new SubclassDynamicTypeBuilder<T>(classFormatVersion,
                namingStrategy,
                superType,
                interfaceTypes,
                Opcodes.ACC_PUBLIC,
                TypeAttributeAppender.NoOp.INSTANCE,
                ignoredMethods,
                classVisitorWrapperChain,
                new FieldRegistry.Default(),
                methodRegistry,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                constructorStrategy);
    }

    public ByteBuddy withClassFormatVersion(int classFormatVersion) {
        return new ByteBuddy(new ClassFormatVersion(classFormatVersion),
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

    public void intercept(MethodMatcher methodMatcher) {
        // TODO
    }
}
