package com.blogspot.mydailyjava.bytebuddy.method;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.method.matcher.MethodMatcher;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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

                private final Method method;
                private final ByteCodeAppender byteCodeAppender;

                public ByteCodeWriter(ByteCodeAppender byteCodeAppender, Method method) {
                    this.byteCodeAppender = byteCodeAppender;
                    this.method = method;
                }

                @Override
                public boolean applyTo(ClassVisitor classVisitor) {
                    MethodVisitor methodVisitor = classVisitor.visitMethod(
                            method.getModifiers(),
                            method.getName(),
                            Type.getMethodDescriptor(method),
                            null,
                            makeInternalNameArray(Arrays.asList(method.getExceptionTypes())));
                    methodVisitor.visitCode();
                    ByteCodeAppender.Size size = byteCodeAppender.apply(methodVisitor, method);
                    methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                    methodVisitor.visitEnd();
                    return true;
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

            boolean applyTo(ClassVisitor classVisitor);
        }

        private final List<MethodInterception> methodInterceptions;

        public Stack() {
            this.methodInterceptions = Collections.emptyList();
        }

        protected Stack(List<MethodInterception> methodInterceptions) {
            this.methodInterceptions = methodInterceptions;
        }

        public Handler lookUp(Method method) {
            for (MethodInterception methodInterception : methodInterceptions) {
                if (methodInterception.getMethodMatcher().matches(method)) {
                    return new Handler.ByteCodeWriter(methodInterception.getByteCodeAppender(), method);
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
