package com.blogspot.mydailyjava.bytebuddy.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive.PrimitiveTypeAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive.VoidAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.reference.ReferenceTypeAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.MethodDelegationBinder;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.MethodNameEqualityResolver;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.MostSpecificTypeResolver;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation.AnnotationDrivenBinder;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation.Argument;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation.This;
import com.blogspot.mydailyjava.bytebuddy.method.matcher.MethodExtraction;
import com.blogspot.mydailyjava.bytebuddy.method.matcher.MethodMatchers;
import com.blogspot.mydailyjava.bytebuddy.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;
import java.util.List;

public class MethodDelegation implements ByteCodeAppender.Factory {

    public static ByteCodeAppender.Factory to(Class<?> type) {
        if (type.isInterface()) {
            throw new IllegalArgumentException("Cannot delegate to interface " + type);
        } else if (type.isArray()) {
            throw new IllegalArgumentException("Cannot delegate to array " + type);
        } else if (type.isPrimitive()) {
            throw new IllegalArgumentException("Cannot delegate to primitive " + type);
        }
        return new MethodDelegation(
                new AnnotationDrivenBinder(
                        Arrays.<AnnotationDrivenBinder.ArgumentBinder<?>>asList(Argument.Binder.INSTANCE, This.Binder.INSTANCE),
                        Argument.NextUnboundAsDefaultHandler.INSTANCE,
                        new VoidAwareAssigner(new PrimitiveTypeAwareAssigner(ReferenceTypeAwareAssigner.INSTANCE), false)),
                new MethodDelegationBinder.AmbiguityResolver.Chain(
                        Arrays.<MethodDelegationBinder.AmbiguityResolver>asList(
                                MethodNameEqualityResolver.INSTANCE,
                                MostSpecificTypeResolver.INSTANCE)),
                MethodExtraction.matching(MethodMatchers.isStatic()).extractFrom(type).asList());
    }

    private final MethodDelegationBinder.Processor processor;
    private final List<MethodDescription> methods;

    protected MethodDelegation(MethodDelegationBinder methodDelegationBinder,
                               MethodDelegationBinder.AmbiguityResolver ambiguityResolver,
                               List<MethodDescription> methods) {
        processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        this.methods = methods;
    }

    private class AppenderDelegate implements ByteCodeAppender {

        private final TypeDescription typeDescription;

        private AppenderDelegate(TypeDescription typeDescription) {
            this.typeDescription = typeDescription;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
            return processor.process(typeDescription, methodDescription, methods).apply(methodVisitor, methodDescription);
        }
    }

    @Override
    public ByteCodeAppender make(TypeDescription typeDescription) {
        return new AppenderDelegate(typeDescription);
    }
}
