package com.blogspot.mydailyjava.bytebuddy.type.scaffold;

import com.blogspot.mydailyjava.bytebuddy.*;
import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapperChain;
import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.MethodInterception;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.method.matcher.JunctionMethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.method.matcher.MethodExtraction;
import com.blogspot.mydailyjava.bytebuddy.method.matcher.MethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.type.instrumentation.ByteArrayDynamicProxy;
import com.blogspot.mydailyjava.bytebuddy.type.instrumentation.DynamicProxy;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.blogspot.mydailyjava.bytebuddy.method.matcher.MethodMatchers.*;

public class SubclassDynamicProxyBuilder implements DynamicProxy.Builder {

    private static final int ASM_MANUAL_WRITING_OPTIONS = 0;
    private static final JunctionMethodMatcher OVERRIDABLE = not(isFinal()).and(not(isStatic()).and(not(isPrivate())));

    private static class SubclassLocatedMethodInterception implements LocatedMethodInterception {

        private final SubclassDynamicProxyBuilder subclassDynamicProxyBuilder;
        private final MethodMatcher methodMatcher;

        private SubclassLocatedMethodInterception(SubclassDynamicProxyBuilder subclassDynamicProxyBuilder,
                                                  MethodMatcher methodMatcher) {
            this.subclassDynamicProxyBuilder = subclassDynamicProxyBuilder;
            this.methodMatcher = methodMatcher;
        }

        @Override
        public DynamicProxy.Builder intercept(ByteCodeAppender.Factory byteCodeAppenderFactory) {
            return new SubclassDynamicProxyBuilder(subclassDynamicProxyBuilder.superClass,
                    subclassDynamicProxyBuilder.interfaces,
                    subclassDynamicProxyBuilder.classVersion,
                    subclassDynamicProxyBuilder.namingStrategy,
                    subclassDynamicProxyBuilder.visibility,
                    subclassDynamicProxyBuilder.typeManifestation,
                    subclassDynamicProxyBuilder.syntheticState,
                    subclassDynamicProxyBuilder.ignoredMethods,
                    subclassDynamicProxyBuilder.classVisitorWrapperChain,
                    subclassDynamicProxyBuilder.methodInterceptions
                            .onTop(new MethodInterception(methodMatcher, byteCodeAppenderFactory)));
        }
    }

    public static SubclassDynamicProxyBuilder of(Class<?> type, ByteBuddy byteBuddy) {
        if (type.isPrimitive()) {
            throw new IllegalArgumentException("Cannot subclass primitive type " + type);
        } else if (type.isArray()) {
            throw new IllegalArgumentException("Cannot subclass array type " + type);
        } else if (Modifier.isFinal(type.getModifiers())) {
            throw new IllegalArgumentException("Cannot subclass final type " + type);
        } else if (type.isInterface()) {
            return new SubclassDynamicProxyBuilder(Object.class,
                    Collections.<Class<?>>singletonList(type),
                    byteBuddy.getClassVersion(),
                    byteBuddy.getNamingStrategy(),
                    byteBuddy.getVisibility(),
                    byteBuddy.getTypeManifestation(),
                    byteBuddy.getSyntheticState(),
                    byteBuddy.getIgnoredMethods(),
                    byteBuddy.getClassVisitorWrapperChain(),
                    new MethodInterception.Stack());
        } else {
            return new SubclassDynamicProxyBuilder(type,
                    Collections.<Class<?>>emptyList(),
                    byteBuddy.getClassVersion(),
                    byteBuddy.getNamingStrategy(),
                    byteBuddy.getVisibility(),
                    byteBuddy.getTypeManifestation(),
                    byteBuddy.getSyntheticState(),
                    byteBuddy.getIgnoredMethods(),
                    byteBuddy.getClassVisitorWrapperChain(),
                    new MethodInterception.Stack());
        }
    }

    private static List<Class<?>> join(List<Class<?>> interfaces, Class<?> anInterface) {
        List<Class<?>> result = new ArrayList<Class<?>>(interfaces.size() + 1);
        result.addAll(interfaces);
        result.add(anInterface);
        return Collections.unmodifiableList(result);
    }

    private static Class<?> checkInterface(Class<?> type) {
        if (!type.isInterface()) {
            throw new IllegalArgumentException(type + " is not an interface type");
        }
        return type;
    }

    private static <T> T checkNotNull(T type) {
        if (type == null) {
            throw new NullPointerException();
        }
        return type;
    }

    private final Class<?> superClass;
    private final List<Class<?>> interfaces;
    private final ClassVersion classVersion;
    private final NamingStrategy namingStrategy;
    private final Visibility visibility;
    private final TypeManifestation typeManifestation;
    private final SyntheticState syntheticState;
    private final MethodMatcher ignoredMethods;
    private final ClassVisitorWrapperChain classVisitorWrapperChain;
    private final MethodInterception.Stack methodInterceptions;

    protected SubclassDynamicProxyBuilder(Class<?> superClass,
                                          List<Class<?>> interfaces,
                                          ClassVersion classVersion,
                                          NamingStrategy namingStrategy,
                                          Visibility visibility,
                                          TypeManifestation typeManifestation,
                                          SyntheticState syntheticState,
                                          MethodMatcher ignoredMethods,
                                          ClassVisitorWrapperChain classVisitorWrapperChain,
                                          MethodInterception.Stack methodInterceptions) {
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.classVersion = classVersion;
        this.namingStrategy = namingStrategy;
        this.visibility = visibility;
        this.typeManifestation = typeManifestation;
        this.syntheticState = syntheticState;
        this.ignoredMethods = ignoredMethods;
        this.classVisitorWrapperChain = classVisitorWrapperChain;
        this.methodInterceptions = methodInterceptions;
    }

    @Override
    public DynamicProxy.Builder implementInterface(Class<?> interfaceType) {
        return new SubclassDynamicProxyBuilder(superClass,
                join(interfaces, checkInterface(interfaceType)),
                classVersion,
                namingStrategy,
                visibility,
                typeManifestation,
                syntheticState,
                ignoredMethods,
                classVisitorWrapperChain,
                methodInterceptions);
    }

    @Override
    public DynamicProxy.Builder classVersion(int versionNumber) {
        return new SubclassDynamicProxyBuilder(superClass,
                interfaces,
                new ClassVersion(versionNumber),
                namingStrategy,
                visibility,
                typeManifestation,
                syntheticState,
                ignoredMethods,
                classVisitorWrapperChain,
                methodInterceptions);
    }

    @Override
    public DynamicProxy.Builder name(String name) {
        return new SubclassDynamicProxyBuilder(superClass,
                interfaces,
                classVersion,
                new NamingStrategy.Fixed(checkNotNull(name)),
                visibility,
                typeManifestation,
                syntheticState,
                ignoredMethods,
                classVisitorWrapperChain,
                methodInterceptions);
    }

    @Override
    public DynamicProxy.Builder visibility(Visibility visibility) {
        return new SubclassDynamicProxyBuilder(superClass,
                interfaces,
                classVersion,
                namingStrategy,
                checkNotNull(visibility),
                typeManifestation,
                syntheticState,
                ignoredMethods,
                classVisitorWrapperChain,
                methodInterceptions);
    }

    @Override
    public DynamicProxy.Builder manifestation(TypeManifestation typeManifestation) {
        return new SubclassDynamicProxyBuilder(superClass,
                interfaces,
                classVersion,
                namingStrategy,
                visibility,
                checkNotNull(typeManifestation),
                syntheticState,
                ignoredMethods,
                classVisitorWrapperChain,
                methodInterceptions);
    }

    @Override
    public DynamicProxy.Builder makeSynthetic(boolean synthetic) {
        return new SubclassDynamicProxyBuilder(superClass,
                interfaces,
                classVersion,
                namingStrategy,
                visibility,
                typeManifestation,
                SyntheticState.is(synthetic),
                ignoredMethods,
                classVisitorWrapperChain,
                methodInterceptions);
    }

    @Override
    public DynamicProxy.Builder ignoredMethods(MethodMatcher ignoredMethods) {
        return new SubclassDynamicProxyBuilder(superClass,
                interfaces,
                classVersion,
                namingStrategy,
                visibility,
                typeManifestation,
                syntheticState,
                checkNotNull(ignoredMethods),
                classVisitorWrapperChain,
                methodInterceptions);
    }

    @Override
    public LocatedMethodInterception method(MethodMatcher methodMatcher) {
        return new SubclassLocatedMethodInterception(this, checkNotNull(methodMatcher));
    }

    @Override
    public DynamicProxy make() {
        ClassWriter classWriter = new ClassWriter(ASM_MANUAL_WRITING_OPTIONS);
        ClassVisitor classVisitor = classVisitorWrapperChain.wrap(classWriter);
        TypeDescription typeDescription = new TypeDescription(classVersion,
                superClass,
                interfaces,
                visibility,
                typeManifestation,
                syntheticState,
                namingStrategy);
        classVisitor.visit(typeDescription.getClassVersion(),
                typeDescription.getTypeModifier(),
                typeDescription.getInternalName(),
                null,
                typeDescription.getSuperClassInternalName(),
                typeDescription.getInterfacesInternalNames());
        MethodInterception.Handler handler = methodInterceptions.handler(typeDescription);
        for (MethodDescription method : MethodExtraction.matching(OVERRIDABLE.and(not(ignoredMethods)))
                .extractFrom(superClass)
                .appendInterfaceMethods(interfaces)
                .asList()) {
            handler.find(method).handle(classVisitor);
        }
        classVisitor.visitEnd();
        return new ByteArrayDynamicProxy(typeDescription.getName(), classWriter.toByteArray());
    }
}
