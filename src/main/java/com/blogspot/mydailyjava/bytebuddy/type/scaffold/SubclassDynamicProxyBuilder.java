package com.blogspot.mydailyjava.bytebuddy.type.scaffold;

import com.blogspot.mydailyjava.bytebuddy.*;
import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapperChain;
import com.blogspot.mydailyjava.bytebuddy.method.JavaMethod;
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

    private static final JunctionMethodMatcher OVERRIDABLE = not(isFinal()).and(not(isStatic()).and(not(isPrivate())));
    private static final int ASM_MANUAL_WRITING_OPTIONS = 0;

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
                    byteBuddy.getNameMaker(),
                    byteBuddy.getVisibility(),
                    byteBuddy.getTypeManifestation(),
                    byteBuddy.getSyntheticState(),
                    byteBuddy.getClassVisitorWrapperChain(),
                    new MethodInterception.Stack());
        } else {
            return new SubclassDynamicProxyBuilder(type,
                    Collections.<Class<?>>emptyList(),
                    byteBuddy.getClassVersion(),
                    byteBuddy.getNameMaker(),
                    byteBuddy.getVisibility(),
                    byteBuddy.getTypeManifestation(),
                    byteBuddy.getSyntheticState(),
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
    private final Visibility visibility;
    private final TypeManifestation typeManifestation;
    private final SyntheticState syntheticState;
    private final ClassVisitorWrapperChain classVisitorWrapperChain;
    private final MethodInterception.Stack methodInterceptions;

    protected SubclassDynamicProxyBuilder(Class<?> type,
                                          List<Class<?>> interfaces,
                                          int classVersion,
                                          NameMaker nameMaker,
                                          Visibility visibility,
                                          TypeManifestation typeManifestation,
                                          SyntheticState syntheticState,
                                          ClassVisitorWrapperChain classVisitorWrapperChain,
                                          MethodInterception.Stack methodInterceptions) {
        this.superClass = type;
        this.interfaces = interfaces;
        this.classVersion = classVersion;
        this.nameMaker = nameMaker;
        this.visibility = visibility;
        this.typeManifestation = typeManifestation;
        this.syntheticState = syntheticState;
        this.classVisitorWrapperChain = classVisitorWrapperChain;
        this.methodInterceptions = methodInterceptions;
    }

    @Override
    public DynamicProxy.Builder implementInterface(Class<?> interfaceType) {
        return new SubclassDynamicProxyBuilder(superClass,
                join(interfaces, checkInterface(interfaceType)),
                classVersion,
                nameMaker,
                visibility,
                typeManifestation,
                syntheticState,
                classVisitorWrapperChain,
                methodInterceptions);
    }

    @Override
    public DynamicProxy.Builder version(int classVersion) {
        return new SubclassDynamicProxyBuilder(superClass,
                interfaces,
                checkClassVersion(classVersion),
                nameMaker,
                visibility,
                typeManifestation,
                syntheticState,
                classVisitorWrapperChain,
                methodInterceptions);
    }

    @Override
    public DynamicProxy.Builder name(String name) {
        return new SubclassDynamicProxyBuilder(superClass,
                interfaces,
                classVersion,
                new NameMaker.Fixed(name),
                visibility,
                typeManifestation,
                syntheticState,
                classVisitorWrapperChain,
                methodInterceptions);
    }

    @Override
    public DynamicProxy.Builder visibility(Visibility visibility) {
        return new SubclassDynamicProxyBuilder(superClass,
                interfaces,
                classVersion,
                nameMaker,
                checkNotNull(visibility),
                typeManifestation,
                syntheticState,
                classVisitorWrapperChain,
                methodInterceptions);
    }

    @Override
    public DynamicProxy.Builder manifestation(TypeManifestation typeManifestation) {
        return new SubclassDynamicProxyBuilder(superClass,
                interfaces,
                classVersion,
                nameMaker,
                visibility,
                checkNotNull(typeManifestation),
                syntheticState,
                classVisitorWrapperChain,
                methodInterceptions);
    }

    @Override
    public DynamicProxy.Builder makeSynthetic(boolean synthetic) {
        return new SubclassDynamicProxyBuilder(superClass,
                interfaces,
                classVersion,
                nameMaker,
                visibility,
                typeManifestation,
                SyntheticState.is(synthetic),
                classVisitorWrapperChain,
                methodInterceptions);
    }

    @Override
    public DynamicProxy.Builder intercept(MethodMatcher methodMatcher, ByteCodeAppender byteCodeAppender) {
        return new SubclassDynamicProxyBuilder(superClass,
                interfaces,
                classVersion,
                nameMaker,
                visibility,
                typeManifestation,
                syntheticState,
                classVisitorWrapperChain,
                methodInterceptions.append(new MethodInterception(checkNotNull(methodMatcher), checkNotNull(byteCodeAppender))));
    }

    @Override
    public DynamicProxy make() {
        ClassWriter classWriter = new ClassWriter(ASM_MANUAL_WRITING_OPTIONS);
        ClassVisitor classVisitor = classVisitorWrapperChain.wrap(classWriter);
        TypeDescription typeDescription = new TypeDescription(classVersion, superClass, interfaces, visibility, typeManifestation, syntheticState, nameMaker);
        classVisitor.visit(typeDescription.getClassVersion(),
                typeDescription.getTypeModifier(),
                typeDescription.getSuperClassInternalName(),
                null,
                typeDescription.getSuperClassInternalName(),
                typeDescription.getInterfacesInternalNames());
        for (JavaMethod method : MethodExtraction.matching(OVERRIDABLE).extract(superClass).appendInterfaces(interfaces).asList()) {
            methodInterceptions.lookUp(method).applyTo(classVisitor);
        }
        classVisitor.visitEnd();
        return new ByteArrayDynamicProxy(typeDescription.getName(), classWriter.toByteArray());
    }
}
