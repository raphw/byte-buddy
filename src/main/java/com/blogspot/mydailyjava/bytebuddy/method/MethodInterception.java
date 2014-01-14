package com.blogspot.mydailyjava.bytebuddy.method;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.SuperClassDelegation;
import com.blogspot.mydailyjava.bytebuddy.method.matcher.MethodMatcher;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MethodInterception {

    public static class Stack {

        public static interface Handler {

            static enum NoOp implements Handler {
                INSTANCE;

                @Override
                public boolean applyTo(ClassVisitor classVisitor) {
                    return false;
                }
            }

            static class ByteCodeWriter implements Handler {

                private final JavaMethod javaMethod;
                private final ByteCodeAppender byteCodeAppender;

                public ByteCodeWriter(ByteCodeAppender byteCodeAppender, JavaMethod javaMethod) {
                    this.javaMethod = javaMethod;
                    this.byteCodeAppender = byteCodeAppender;
                }

                @Override
                public boolean applyTo(ClassVisitor classVisitor) {
                    MethodVisitor methodVisitor = classVisitor.visitMethod(
                            javaMethod.getModifiers(),
                            javaMethod.getName(),
                            javaMethod.getDescriptor(),
                            null,
                            javaMethod.getExceptionTypesInternalNames());
                    methodVisitor.visitCode();
                    ByteCodeAppender.Size size = byteCodeAppender.apply(methodVisitor, javaMethod);
                    methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                    methodVisitor.visitEnd();
                    return true;
                }
            }

            boolean applyTo(ClassVisitor classVisitor);
        }

        private final List<MethodInterception> methodInterceptions;

        public Stack() {
            this.methodInterceptions = Collections.emptyList();
        }

        protected Stack(List<MethodInterception> methodInterceptions) {
            this.methodInterceptions = methodInterceptions;
        }

        public Handler lookUp(JavaMethod javaMethod) {
            if(javaMethod.isConstructor()) {
                return new Handler.ByteCodeWriter(SuperClassDelegation.INSTANCE, javaMethod);
            }
            for (MethodInterception methodInterception : methodInterceptions) {
                if (methodInterception.getMethodMatcher().matches(javaMethod)) {
                    return new Handler.ByteCodeWriter(methodInterception.getByteCodeAppender(), javaMethod);
                }
            }
            return Handler.NoOp.INSTANCE;
        }

        public Stack append(MethodInterception methodInterception) {
            List<MethodInterception> methodInterceptions = new ArrayList<MethodInterception>(this.methodInterceptions.size() + 1);
            methodInterceptions.addAll(this.methodInterceptions);
            methodInterceptions.add(methodInterception);
            return new Stack(Collections.unmodifiableList(methodInterceptions));
        }
    }

    private final MethodMatcher methodMatcher;
    private final ByteCodeAppender byteCodeAppender;

    public MethodInterception(MethodMatcher methodMatcher, ByteCodeAppender byteCodeAppender) {
        this.methodMatcher = methodMatcher;
        this.byteCodeAppender = byteCodeAppender;
    }

    public MethodMatcher getMethodMatcher() {
        return methodMatcher;
    }

    public ByteCodeAppender getByteCodeAppender() {
        return byteCodeAppender;
    }
}
