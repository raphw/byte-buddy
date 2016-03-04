package net.bytebuddy.asm.advice;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

import java.io.IOException;
import java.lang.annotation.*;
import java.util.HashMap;
import java.util.Map;

public class Advice implements AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper {

    private static final AnnotationVisitor IGNORE_ANNOTATION = null;

    private static final MethodVisitor IGNORE_METHOD = null;

    private final Map<String, MethodDescription> methodEnter;

    private final Map<String, MethodDescription> methodExit;

    private final byte[] binaryRepresentation;

    protected Advice(Map<String, MethodDescription> methodEnter, Map<String, MethodDescription> methodExit, byte[] binaryRepresentation) {
        this.methodEnter = methodEnter;
        this.methodExit = methodExit;
        this.binaryRepresentation = binaryRepresentation;
    }

    public static AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper to(Class<?> type) {
        return to(type, ClassFileLocator.ForClassLoader.of(type.getClassLoader()));
    }

    public static AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper to(Class<?> type, ClassFileLocator classFileLocator) {
        return to(new TypeDescription.ForLoadedType(type), classFileLocator);
    }

    public static AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper to(TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        try {
            Map<String, MethodDescription> methodEnter = new HashMap<String, MethodDescription>(), methodExit = new HashMap<String, MethodDescription>();
            for(MethodDescription methodDescription : typeDescription.getDeclaredMethods()) {
                considerAdvice(OnMethodEnter.class, methodEnter, methodDescription);
                considerAdvice(OnMethodExit.class, methodExit, methodDescription);
            }
            if (methodEnter.isEmpty() && methodExit.isEmpty()) {
                throw new IllegalArgumentException("No advice defined by " + typeDescription);
            }
            return new Advice(methodEnter, methodExit, classFileLocator.locate(typeDescription.getName()).resolve());
        } catch (IOException exception) {
            throw new IllegalStateException("Error reading class file of " + typeDescription, exception);
        }
    }

    private static void considerAdvice(Class<? extends Annotation> annotation, Map<String, MethodDescription> methods, MethodDescription methodDescription) {
        if (methodDescription.getDeclaredAnnotations().isAnnotationPresent(annotation)) {
            if (!methodDescription.isStatic()) {
                throw new IllegalStateException("Advice is not static: " + methodDescription);
            }
            methods.put(methodDescription.getInternalName() + methodDescription.getDescriptor(), methodDescription);
        }
    }

    @Override
    public MethodVisitor wrap(TypeDescription instrumentedType, MethodDescription.InDefinedShape methodDescription, MethodVisitor methodVisitor) {
        return new AsmAdvice(methodVisitor, methodDescription);
    }

    protected class AsmAdvice extends AdviceAdapter {

        protected AsmAdvice(MethodVisitor methodVisitor, MethodDescription methodDescription) {
            super(Opcodes.ASM5, methodVisitor, methodDescription.getActualModifiers(), methodDescription.getInternalName(), methodDescription.getDescriptor());
        }

        @Override
        protected void onMethodEnter() {
            new ClassReader(binaryRepresentation).accept(new CodeInliner(methodEnter), ClassReader.SKIP_DEBUG);
        }

        @Override
        protected void onMethodExit(int opcode) {
            new ClassReader(binaryRepresentation).accept(new CodeInliner(methodExit), ClassReader.SKIP_DEBUG);
        }

        protected class CodeInliner extends ClassVisitor {

            private final Map<String, MethodDescription> methods;

            protected CodeInliner(Map<String, MethodDescription> methods) {
                super(Opcodes.ASM5);
                this.methods = methods;
            }

            @Override
            public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exception) {
                MethodDescription methodDescription = methods.get(internalName + descriptor);
                return methodDescription == null
                        ? IGNORE_METHOD
                        : new TransferingVisitor(AsmAdvice.this);
            }

            protected class TransferingVisitor extends MethodVisitor {

                protected TransferingVisitor(MethodVisitor methodVisitor) {
                    super(Opcodes.ASM5, methodVisitor);
                }

                @Override
                public void visitParameter(String name, int modifiers) {
                    /* do nothing */
                }

                @Override
                public AnnotationVisitor visitAnnotationDefault() {
                    return IGNORE_ANNOTATION;
                }

                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    return IGNORE_ANNOTATION;
                }

                @Override
                public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
                    return IGNORE_ANNOTATION;
                }

                @Override
                public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
                    return IGNORE_ANNOTATION;
                }

                @Override
                public void visitAttribute(Attribute attr) {
                    /* do nothing */
                }

                @Override
                public void visitLineNumber(int line, Label start) {
                    /* do nothing */
                }

                @Override
                public void visitCode() {
                    /* do nothing */
                }

                @Override
                public void visitEnd() {
                    /* do nothing */
                }

                @Override
                public void visitMaxs(int maxStack, int maxLocals) {
                    /* do nothing */
                }
            }
        }
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface OnMethodEnter {

    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface OnMethodExit {

    }
}
