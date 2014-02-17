package com.blogspot.mydailyjava.bytebuddy.instrumentation.method;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.SuperClassDelegation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MethodInterception {

    public static class Stack {

        private final List<MethodInterception> methodInterceptions;

        public Stack() {
            this.methodInterceptions = Collections.emptyList();
        }

        private Stack(List<MethodInterception> methodInterceptions, MethodInterception methodInterception) {
            List<MethodInterception> joined = new ArrayList<MethodInterception>(methodInterceptions.size() + 1);
            joined.add(methodInterception);
            joined.addAll(methodInterceptions);
            this.methodInterceptions = Collections.unmodifiableList(joined);
        }

        public Stack onTop(MethodInterception methodInterception) {
            return new Stack(methodInterceptions, methodInterception);
        }

        public Handler handler(TypeDescription typeDescription) {
            return new Handler(methodInterceptions, typeDescription);
        }
    }

    public static class Handler {

        public static interface Delegate {

            boolean handle(ClassVisitor classVisitor);
        }

        private static class Entry implements MethodMatcher {

            private final MethodMatcher methodMatcher;
            private final ByteCodeAppender byteCodeAppender;

            private Entry(MethodMatcher methodMatcher, ByteCodeAppender byteCodeAppender) {
                this.methodMatcher = methodMatcher;
                this.byteCodeAppender = byteCodeAppender;
            }

            @Override
            public boolean matches(MethodDescription methodDescription) {
                return methodMatcher.matches(methodDescription);
            }

            public ByteCodeAppender getByteCodeAppender() {
                return byteCodeAppender;
            }
        }

        private static class MethodWritingDelegate implements Delegate {

            private final MethodDescription methodDescription;
            private final ByteCodeAppender byteCodeAppender;

            public MethodWritingDelegate(MethodDescription methodDescription, ByteCodeAppender byteCodeAppender) {
                this.methodDescription = methodDescription;
                this.byteCodeAppender = byteCodeAppender;
            }

            @Override
            public boolean handle(ClassVisitor classVisitor) {
                MethodVisitor methodVisitor = classVisitor.visitMethod(methodDescription.getModifiers(),
                        methodDescription.getInternalName(),
                        methodDescription.getDescriptor(),
                        null,
                        methodDescription.getExceptionTypes().toInternalNames());
                methodVisitor.visitCode();
                ByteCodeAppender.Size size = byteCodeAppender.apply(methodVisitor, null, methodDescription);
                methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                methodVisitor.visitEnd();
                return true;
            }
        }

        private static enum NoOpDelegate implements Delegate {
            INSTANCE;

            @Override
            public boolean handle(ClassVisitor classVisitor) {
                return false;
            }
        }

        private final List<Entry> entries;
        private final ByteCodeAppender constructorAppender;

        public Handler(List<MethodInterception> methodInterceptions, TypeDescription typeDescription) {
            List<Entry> entries = new ArrayList<Entry>(methodInterceptions.size());
            for (MethodInterception methodInterception : methodInterceptions) {
                entries.add(new Entry(methodInterception.getMethodMatcher(),
                        methodInterception.getByteCodeAppenderFactory().make(typeDescription)));
            }
            this.entries = Collections.unmodifiableList(entries);
            constructorAppender = SuperClassDelegation.INSTANCE.make(typeDescription);
        }

        public Delegate find(MethodDescription methodDescription) {
            // Until constructor interception is fully supported, this serves as a preliminary solution.
            if (methodDescription.isConstructor()) {
                return new MethodWritingDelegate(methodDescription, constructorAppender);
            }
            for (Entry entry : entries) {
                if (entry.matches(methodDescription)) {
                    return new MethodWritingDelegate(methodDescription, entry.getByteCodeAppender());
                }
            }
            return NoOpDelegate.INSTANCE;
        }
    }

    private final MethodMatcher methodMatcher;
    private final ByteCodeAppender.Factory byteCodeAppenderFactory;

    public MethodInterception(MethodMatcher methodMatcher, ByteCodeAppender.Factory byteCodeAppenderFactory) {
        this.methodMatcher = methodMatcher;
        this.byteCodeAppenderFactory = byteCodeAppenderFactory;
    }

    public MethodMatcher getMethodMatcher() {
        return methodMatcher;
    }

    public ByteCodeAppender.Factory getByteCodeAppenderFactory() {
        return byteCodeAppenderFactory;
    }
}
