package com.blogspot.mydailyjava.bytebuddy.type.scaffold;

import com.blogspot.mydailyjava.bytebuddy.ByteBuddy;
import com.blogspot.mydailyjava.bytebuddy.DynamicProxy;
import com.blogspot.mydailyjava.bytebuddy.NameMaker;
import com.blogspot.mydailyjava.bytebuddy.method.Interception;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.SuperClassDelegationByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.method.matcher.MethodExtractor;
import com.blogspot.mydailyjava.bytebuddy.method.matcher.MethodMatcher;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.blogspot.mydailyjava.bytebuddy.method.matcher.MethodMatchers.*;

public class SubclassDynamicProxyBuilder implements DynamicProxy.Builder {

    private static final MethodMatcher OVERRIDABLE = not(isFinal()).and(not(isStatic()).and(not(isPrivate())));
    private static final int ASM_MANUAL_WRITING = 0;
    private static final String CONSTRUCTOR_METHOD_NAME = "<init>";

    public static SubclassDynamicProxyBuilder of(Class<?> type) {
        if (type.isPrimitive()) {
            throw new IllegalArgumentException("Cannot subclass primitive type " + type);
        } else if (type.isArray()) {
            throw new IllegalArgumentException("Cannot subclass array type " + type);
        } else if (Modifier.isFinal(type.getModifiers())) {
            throw new IllegalArgumentException("Cannot subclass final type " + type);
        } else if (type.isInterface()) {
            return new SubclassDynamicProxyBuilder(Object.class,
                    Collections.<Class<?>>singletonList(type),
                    ByteBuddy.DEFAULT_CLASS_VERSION,
                    new NameMaker.PrefixingRandom(ByteBuddy.DEFAULT_NAME_PREFIX),
                    ByteBuddy.DEFAULT_VISIBILITY,
                    ByteBuddy.DEFAULT_MANIFESTATION,
                    ByteBuddy.DEFAULT_FINAL_STATE,
                    ByteBuddy.DEFAULT_SYNTHETIC_STATE,
                    new Interception.Stack());
        } else {
            return new SubclassDynamicProxyBuilder(type,
                    Collections.<Class<?>>emptyList(),
                    ByteBuddy.DEFAULT_CLASS_VERSION,
                    new NameMaker.PrefixingRandom(ByteBuddy.DEFAULT_NAME_PREFIX),
                    ByteBuddy.DEFAULT_VISIBILITY,
                    ByteBuddy.DEFAULT_MANIFESTATION,
                    ByteBuddy.DEFAULT_FINAL_STATE,
                    ByteBuddy.DEFAULT_SYNTHETIC_STATE,
                    new Interception.Stack());
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

    private static int checkClassVersion(int classVersion) {
        if (!(classVersion > 0)) {
            throw new IllegalArgumentException("Class version " + classVersion + " is not valid");
        }
        return classVersion;
    }

    private static <T> T checkNotNull(T type) {
        if (type == null) {
            throw new NullPointerException();
        }
        return type;
    }

    private final Class<?> superClass;
    private final List<Class<?>> interfaces;
    private final int classVersion;
    private final NameMaker nameMaker;
    private final ByteBuddy.Visibility visibility;
    private final ByteBuddy.Manifestation manifestation;
    private final ByteBuddy.FinalState finalState;
    private final ByteBuddy.SyntheticState syntheticState;
    private final Interception.Stack interceptions;

    protected SubclassDynamicProxyBuilder(Class<?> type,
                                          List<Class<?>> interfaces,
                                          int classVersion,
                                          NameMaker nameMaker,
                                          ByteBuddy.Visibility visibility,
                                          ByteBuddy.Manifestation manifestation,
                                          ByteBuddy.FinalState finalState,
                                          ByteBuddy.SyntheticState syntheticState,
                                          Interception.Stack interceptions) {
        this.superClass = type;
        this.interfaces = interfaces;
        this.classVersion = classVersion;
        this.nameMaker = nameMaker;
        this.visibility = visibility;
        this.manifestation = manifestation;
        this.finalState = finalState;
        this.syntheticState = syntheticState;
        this.interceptions = interceptions;
    }

    @Override
    public DynamicProxy.Builder implementInterface(Class<?> interfaceType) {
        return new SubclassDynamicProxyBuilder(superClass,
                join(interfaces, checkInterface(interfaceType)),
                classVersion,
                nameMaker,
                visibility,
                manifestation,
                finalState,
                syntheticState,
                interceptions);
    }

    @Override
    public DynamicProxy.Builder version(int classVersion) {
        return new SubclassDynamicProxyBuilder(superClass,
                interfaces,
                checkClassVersion(classVersion),
                nameMaker,
                visibility,
                manifestation,
                finalState,
                syntheticState,
                interceptions);
    }

    @Override
    public DynamicProxy.Builder name(String name) {
        return new SubclassDynamicProxyBuilder(superClass,
                interfaces,
                classVersion,
                new NameMaker.Fixed(name),
                visibility,
                manifestation,
                finalState,
                syntheticState,
                interceptions);
    }

    @Override
    public DynamicProxy.Builder visibility(ByteBuddy.Visibility visibility) {
        return new SubclassDynamicProxyBuilder(superClass,
                interfaces,
                classVersion,
                nameMaker,
                checkNotNull(visibility),
                manifestation,
                finalState,
                syntheticState,
                interceptions);
    }

    @Override
    public DynamicProxy.Builder manifestation(ByteBuddy.Manifestation manifestation) {
        return new SubclassDynamicProxyBuilder(superClass,
                interfaces,
                classVersion,
                nameMaker,
                visibility,
                checkNotNull(manifestation),
                finalState,
                syntheticState,
                interceptions);
    }

    @Override
    public DynamicProxy.Builder makeFinal(boolean isFinal) {
        return new SubclassDynamicProxyBuilder(superClass,
                interfaces,
                classVersion,
                nameMaker,
                visibility,
                manifestation,
                ByteBuddy.FinalState.is(isFinal),
                syntheticState,
                interceptions);
    }

    @Override
    public DynamicProxy.Builder makeSynthetic(boolean synthetic) {
        return new SubclassDynamicProxyBuilder(superClass,
                interfaces,
                classVersion,
                nameMaker,
                visibility,
                manifestation,
                finalState,
                ByteBuddy.SyntheticState.is(synthetic),
                interceptions);
    }

    @Override
    public DynamicProxy.Builder intercept(MethodMatcher methodMatcher, ByteCodeAppender byteCodeAppender) {
        return new SubclassDynamicProxyBuilder(superClass,
                interfaces,
                classVersion,
                nameMaker,
                visibility,
                manifestation,
                finalState,
                syntheticState,
                interceptions.append(new Interception(checkNotNull(methodMatcher), checkNotNull(byteCodeAppender))));
    }

    @Override
    public DynamicProxy make() {
        ClassWriter classWriter = new ClassWriter(ASM_MANUAL_WRITING);
        String typeName = nameMaker.getName(superClass);
        classWriter.visit(classVersion, makeTypeModifier(), toInternalName(typeName), null, Type.getInternalName(superClass), makeInternalNameArray(interfaces));
        applyConstructorDelegation(classWriter);
        applyMethodInterception(classWriter);
        classWriter.visitEnd();
        return new ByteArrayDynamicProxy(typeName, classWriter.toByteArray());
    }

    private void applyConstructorDelegation(ClassWriter classWriter) {
        for (Constructor<?> constructor : superClass.getDeclaredConstructors()) {
            if (!Modifier.isPrivate(constructor.getModifiers())) {
                MethodVisitor methodVisitor = classWriter.visitMethod(constructor.getModifiers(), CONSTRUCTOR_METHOD_NAME,
                        Type.getConstructorDescriptor(constructor), null, makeInternalNameArray(Arrays.asList(constructor.getExceptionTypes())));
                methodVisitor.visitCode();
                ByteCodeAppender.Size size = new SuperClassDelegationByteCodeAppender(superClass).apply(methodVisitor, constructor);
                methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                methodVisitor.visitEnd();
            }
        }
    }

    private void applyMethodInterception(ClassWriter classWriter) {
        for (Method method : MethodExtractor.matching(OVERRIDABLE).extractUniqueMethodsFrom(superClass)) {
            ByteCodeAppender byteCodeAppender = interceptions.findInterceptorFor(method);
            if (byteCodeAppender != null) {
                MethodVisitor methodVisitor = classWriter.visitMethod(method.getModifiers(), method.getName(),
                        Type.getMethodDescriptor(method), null, makeInternalNameArray(Arrays.asList(method.getExceptionTypes())));
                methodVisitor.visitCode();
                ByteCodeAppender.Size size = byteCodeAppender.apply(methodVisitor, method);
                methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                methodVisitor.visitEnd();
            }
        }
    }

    private int makeTypeModifier() {
        return visibility.getMask() + manifestation.getMask() + finalState.getMask() + syntheticState.getMask() + Opcodes.ACC_SUPER;
    }

    private static String toInternalName(String name) {
        return name.replace('.', '/');
    }

    private static String[] makeInternalNameArray(List<Class<?>> types) {
        if (types.size() == 0) {
            return null;
        }
        String[] internalName = new String[types.size()];
        int i = 0;
        for (Class<?> type : types) {
            internalName[i] = Type.getInternalName(type);
        }
        return internalName;
    }
}
