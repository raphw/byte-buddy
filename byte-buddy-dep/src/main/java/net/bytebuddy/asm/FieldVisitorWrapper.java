package net.bytebuddy.asm;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.objectweb.asm.FieldVisitor;

import java.util.Arrays;
import java.util.List;

public interface FieldVisitorWrapper {

    FieldVisitor wrap(TypeDescription instrumentedType, FieldDescription fieldDescription, FieldVisitor fieldVisitor);

    enum NoOp implements FieldVisitorWrapper {

        INSTANCE;

        @Override
        public FieldVisitor wrap(TypeDescription instrumentedType, FieldDescription fieldDescription, FieldVisitor fieldVisitor) {
            return fieldVisitor;
        }
    }

    class Matching implements FieldVisitorWrapper {

        private final ElementMatcher<? super FieldDescription> matcher;

        private final FieldVisitorWrapper delegate;

        public Matching(ElementMatcher<? super FieldDescription> matcher, FieldVisitorWrapper delegate) {
            this.matcher = matcher;
            this.delegate = delegate;
        }

        @Override
        public FieldVisitor wrap(TypeDescription instrumentedType, FieldDescription fieldDescription, FieldVisitor fieldVisitor) {
            return fieldDescription != null && matcher.matches(fieldDescription)
                    ? delegate.wrap(instrumentedType, fieldDescription, fieldVisitor)
                    : fieldVisitor;
        }
    }

    class Compound implements FieldVisitorWrapper {

        private final List<? extends FieldVisitorWrapper> fieldVisitorWrappers;

        public Compound(FieldVisitorWrapper... fieldVisitorWrapper) {
            this(Arrays.asList(fieldVisitorWrapper));
        }

        public Compound(List<? extends FieldVisitorWrapper> fieldVisitorWrappers) {
            this.fieldVisitorWrappers = fieldVisitorWrappers;
        }

        @Override
        public FieldVisitor wrap(TypeDescription instrumentedType, FieldDescription fieldDescription, FieldVisitor fieldVisitor) {
            for (FieldVisitorWrapper fieldVisitorWrapper : fieldVisitorWrappers) {
                fieldVisitor = fieldVisitorWrapper.wrap(instrumentedType, fieldDescription, fieldVisitor);
            }
            return fieldVisitor;
        }
    }
}
