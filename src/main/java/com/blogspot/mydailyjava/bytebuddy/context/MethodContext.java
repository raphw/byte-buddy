package com.blogspot.mydailyjava.bytebuddy.context;

import com.blogspot.mydailyjava.bytebuddy.method.utility.MethodDescriptor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MethodContext {

    private static class MethodArgumentAnalyzer implements MethodDescriptor.Visitor {

        private final List<String> methodArguments;
        private final List<Integer> aggregateStackSize;

        private MethodArgumentAnalyzer() {
            this.methodArguments = new ArrayList<String>();
            this.aggregateStackSize = new ArrayList<Integer>();
        }

        public List<String> getMethodArguments() {
            return methodArguments;
        }

        public List<Integer> getAggregateArgumentSize() {
            return aggregateStackSize;
        }

        @Override
        public void visitObject(String descriptor, int localVariableIndex) {
            methodArguments.add(descriptor);
            aggregateStackSize.add(localVariableIndex);
        }

        @Override
        public void visitArray(String descriptor, int localVariableIndex) {
            methodArguments.add(descriptor);
            aggregateStackSize.add(localVariableIndex);
        }

        @Override
        public void visitDouble(int localVariableIndex) {
            methodArguments.add(String.valueOf(MethodDescriptor.DOUBLE_SYMBOL));
            aggregateStackSize.add(localVariableIndex);
        }

        @Override
        public void visitFloat(int localVariableIndex) {
            methodArguments.add(String.valueOf(MethodDescriptor.FLOAT_SYMBOL));
            aggregateStackSize.add(localVariableIndex);
        }

        @Override
        public void visitLong(int localVariableIndex) {
            methodArguments.add(String.valueOf(MethodDescriptor.LONG_SYMBOL));
            aggregateStackSize.add(localVariableIndex);
        }

        @Override
        public void visitInt(int localVariableIndex) {
            methodArguments.add(String.valueOf(MethodDescriptor.INT_SYMBOL));
            aggregateStackSize.add(localVariableIndex);
        }

        @Override
        public void visitChar(int localVariableIndex) {
            methodArguments.add(String.valueOf(MethodDescriptor.CHAR_SYMBOL));
            aggregateStackSize.add(localVariableIndex);
        }

        @Override
        public void visitShort(int localVariableIndex) {
            methodArguments.add(String.valueOf(MethodDescriptor.SHORT_SYMBOL));
            aggregateStackSize.add(localVariableIndex);
        }

        @Override
        public void visitByte(int localVariableIndex) {
            methodArguments.add(String.valueOf(MethodDescriptor.BYTE_SYMBOL));
            aggregateStackSize.add(localVariableIndex);
        }

        @Override
        public void visitBoolean(int localVariableIndex) {
            methodArguments.add(String.valueOf(MethodDescriptor.BOOLEAN_SYMBOL));
            aggregateStackSize.add(localVariableIndex);
        }
    }

    private static String[] asInternalNameArray(Class<?>[] type) {
        String[] internalNames = new String[type.length];
        for (int i = 0; i < type.length; i++) {
            internalNames[i] = Type.getInternalName(type[i]);
        }
        return internalNames;
    }

    private final int access;
    private final String name;
    private final String descriptor;
    private final List<String> argumentTypes;
    private final List<Integer> aggregateArgumentSize;
    private final String returnType;
    private final String signature;
    private final List<String> exceptions;

    public MethodContext(Method method) {
        this(method.getModifiers(), method.getName(), Type.getMethodDescriptor(method), "", asInternalNameArray(method.getExceptionTypes()));
    }

    public MethodContext(int access, String name, String desc, String signature, String[] exception) {
        this.access = access;
        this.name = name;
        this.descriptor = desc;
        MethodArgumentAnalyzer methodArgumentAnalyzer = new MethodDescriptor(desc).apply(new MethodArgumentAnalyzer());
        this.argumentTypes = Collections.unmodifiableList(methodArgumentAnalyzer.getMethodArguments());
        this.aggregateArgumentSize = Collections.unmodifiableList(methodArgumentAnalyzer.getAggregateArgumentSize());
        this.returnType = Type.getReturnType(descriptor).getInternalName();
        this.signature = signature == null ? "" : signature;
        this.exceptions = exception == null ? Collections.<String>emptyList() : Arrays.asList(exception);
    }

    public int getAccess() {
        return access;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public List<String> getArgumentTypes() {
        return argumentTypes;
    }

    public List<Integer> getAggregateArgumentSize() {
        return aggregateArgumentSize;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getSignature() {
        return signature;
    }

    public List<String> getExceptions() {
        return exceptions;
    }
}
