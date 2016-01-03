package net.bytebuddy.asm;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;
import java.util.List;

public interface MethodVisitorWrapper {

    MethodVisitor wrap(TypeDescription instrumentedType, MethodDescription methodDescription, MethodVisitor methodVisitor);

    enum NoOp implements MethodVisitorWrapper {

        INSTANCE;

        @Override
        public MethodVisitor wrap(TypeDescription instrumentedType, MethodDescription methodDescription, MethodVisitor methodVisitor) {
            return methodVisitor;
        }
    }

    class Matching implements MethodVisitorWrapper {

        private final ElementMatcher<? super MethodDescription> matcher;

        private final MethodVisitorWrapper delegate;

        public Matching(ElementMatcher<? super MethodDescription> matcher, MethodVisitorWrapper delegate) {
            this.matcher = matcher;
            this.delegate = delegate;
        }

        @Override
        public MethodVisitor wrap(TypeDescription instrumentedType, MethodDescription methodDescription, MethodVisitor methodVisitor) {
            return methodDescription != null && matcher.matches(methodDescription)
                    ? delegate.wrap(instrumentedType, methodDescription, methodVisitor)
                    : methodVisitor;
        }
    }

    class Compound implements MethodVisitorWrapper {

        private final List<? extends MethodVisitorWrapper> methodVisitorWrappers;

        public Compound(MethodVisitorWrapper... methodVisitorWrapper) {
            this(Arrays.asList(methodVisitorWrapper));
        }

        public Compound(List<? extends MethodVisitorWrapper> methodVisitorWrappers) {
            this.methodVisitorWrappers = methodVisitorWrappers;
        }

        @Override
        public MethodVisitor wrap(TypeDescription instrumentedType, MethodDescription methodDescription, MethodVisitor methodVisitor) {
            for (MethodVisitorWrapper methodVisitorWrapper : methodVisitorWrappers) {
                methodVisitor = methodVisitorWrapper.wrap(instrumentedType, methodDescription, methodVisitor);
            }
            return methodVisitor;
        }
    }
}
