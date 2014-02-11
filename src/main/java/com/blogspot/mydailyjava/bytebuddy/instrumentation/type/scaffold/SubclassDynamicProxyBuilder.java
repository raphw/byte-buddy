package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.scaffold;

import com.blogspot.mydailyjava.bytebuddy.*;
import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapper;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodInterception;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType0;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.proxy.ByteArrayDynamicProxy;
import com.blogspot.mydailyjava.bytebuddy.proxy.DynamicProxy;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;

public class SubclassDynamicProxyBuilder<T> implements DynamicProxy.Builder<T> {

    private static final int ASM_MANUAL_WRITING_OPTIONS = 0;

    private static class SubclassLocatedMethodInterception<T> implements LocatedMethodInterception<T> {

        private final SubclassDynamicProxyBuilder<T> subclassDynamicProxyBuilder;
        private final MethodMatcher methodMatcher;

        private SubclassLocatedMethodInterception(SubclassDynamicProxyBuilder<T> subclassDynamicProxyBuilder,
                                                  MethodMatcher methodMatcher) {
            this.subclassDynamicProxyBuilder = subclassDynamicProxyBuilder;
            this.methodMatcher = methodMatcher;
        }

        @Override
        public DynamicProxy.Builder<T> intercept(ByteCodeAppender.Factory byteCodeAppenderFactory) {
            return new SubclassDynamicProxyBuilder<T>(subclassDynamicProxyBuilder.superClass,
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

    @SuppressWarnings("unchecked")
    public static <T> SubclassDynamicProxyBuilder<T> of(Class<? extends T> type, ByteBuddy byteBuddy) {
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
            return new SubclassDynamicProxyBuilder<T>(type,
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

    private final Class<? extends T> superClass;
    private final List<Class<?>> interfaces;
    private final ClassVersion classVersion;
    private final NamingStrategy namingStrategy;
    private final Visibility visibility;
    private final TypeManifestation typeManifestation;
    private final SyntheticState syntheticState;
    private final MethodMatcher ignoredMethods;
    private final ClassVisitorWrapper.Chain classVisitorWrapperChain;
    private final MethodInterception.Stack methodInterceptions;

    protected SubclassDynamicProxyBuilder(Class<? extends T> superClass,
                                          List<Class<?>> interfaces,
                                          ClassVersion classVersion,
                                          NamingStrategy namingStrategy,
                                          Visibility visibility,
                                          TypeManifestation typeManifestation,
                                          SyntheticState syntheticState,
                                          MethodMatcher ignoredMethods,
                                          ClassVisitorWrapper.Chain classVisitorWrapperChain,
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
    public DynamicProxy.Builder<T> implementInterface(Class<?> interfaceType) {
        return new SubclassDynamicProxyBuilder<T>(superClass,
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
    public DynamicProxy.Builder<T> classVersion(int versionNumber) {
        return new SubclassDynamicProxyBuilder<T>(superClass,
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
    public DynamicProxy.Builder<T> name(String name) {
        return new SubclassDynamicProxyBuilder<T>(superClass,
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
    public DynamicProxy.Builder<T> visibility(Visibility visibility) {
        return new SubclassDynamicProxyBuilder<T>(superClass,
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
    public DynamicProxy.Builder<T> manifestation(TypeManifestation typeManifestation) {
        return new SubclassDynamicProxyBuilder<T>(superClass,
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
    public DynamicProxy.Builder<T> makeSynthetic(boolean synthetic) {
        return new SubclassDynamicProxyBuilder<T>(superClass,
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
    public DynamicProxy.Builder<T> ignoredMethods(MethodMatcher ignoredMethods) {
        return new SubclassDynamicProxyBuilder<T>(superClass,
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
    public LocatedMethodInterception<T> method(MethodMatcher methodMatcher) {
        return new SubclassLocatedMethodInterception<T>(this, checkNotNull(methodMatcher));
    }

    private static class WritableMethodFilter implements MethodMatcher {

        private final TypeDescription typeDescription;

        private WritableMethodFilter(TypeDescription typeDescription) {
            this.typeDescription = typeDescription;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.isOverridable() || typeDescription.equals(methodDescription.getDeclaringType());
        }
    }

    @Override
    public DynamicProxy<T> make() {
        ClassWriter classWriter = new ClassWriter(ASM_MANUAL_WRITING_OPTIONS);
        ClassVisitor classVisitor = classVisitorWrapperChain.wrap(classWriter);
        InstrumentedType0 instrumentedType = new InstrumentedType0(classVersion,
                superClass,
                interfaces,
                visibility,
                typeManifestation,
                syntheticState,
                namingStrategy);
        classVisitor.visit(instrumentedType.getClassVersion(),
                instrumentedType.getModifiers(),
                instrumentedType.getInternalName(),
                null,
                instrumentedType.getSupertype().getInternalName(),
                instrumentedType.getInterfaces().toInternalNames());
        MethodInterception.Handler handler = methodInterceptions.handler(instrumentedType);
        for (MethodDescription method : instrumentedType.getReachableMethods().filter(not(ignoredMethods)
                .and(new WritableMethodFilter(instrumentedType)))) {
            handler.find(method).handle(classVisitor);
        }
        for(MethodDescription method : instrumentedType.getSupertype().getDeclaredMethods().filter(not(ignoredMethods)
                .and(isConstructor()).and(not(isPrivate())))) {
            handler.find(method).handle(classVisitor);
        }
        classVisitor.visitEnd();
        return new ByteArrayDynamicProxy<T>(instrumentedType.getName(), classWriter.toByteArray());
    }
}
