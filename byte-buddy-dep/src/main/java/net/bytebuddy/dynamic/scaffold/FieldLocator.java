package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.*;

public interface FieldLocator<T extends FieldDescription> {

    T locate(String name);

    T locate(String name, TypeDescription type);

    abstract class AbstractBase<S extends FieldDescription> implements FieldLocator<S> {

        private final TypeDescription accessingType;

        protected AbstractBase(TypeDescription accessingType) {
            this.accessingType = accessingType;
        }

        @Override
        public S locate(String name) {
            FieldList<S> candidates = locate(named(name).and(isVisibleTo(accessingType)));
            if (candidates.size() == 1) {
                return candidates.getOnly();
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        public S locate(String name, TypeDescription type) {
            FieldList<S> candidates = locate(named(name).and(fieldType(type)).and(isVisibleTo(accessingType)));
            if (candidates.isEmpty()) {
                throw new IllegalStateException();
            } else {
                return candidates.getOnly();
            }
        }

        protected abstract FieldList<S> locate(ElementMatcher<? super S> matcher);
    }

    class ForExactType extends AbstractBase<FieldDescription.InDefinedShape> {

        private final TypeDescription typeDescription;

        public ForExactType(TypeDescription typeDescription) {
            this(typeDescription, typeDescription);
        }

        protected ForExactType(TypeDescription typeDescription, TypeDescription accessingType) {
            super(accessingType);
            this.typeDescription = typeDescription;
        }

        @Override
        protected FieldList<FieldDescription.InDefinedShape> locate(ElementMatcher<? super FieldDescription.InDefinedShape> matcher) {
            return typeDescription.getDeclaredFields().filter(matcher);
        }
    }

    class ForClassHierarchy extends AbstractBase<FieldDescription> {

        private final TypeDescription typeDescription;

        public ForClassHierarchy(TypeDescription typeDescription) {
            this(typeDescription, typeDescription);
        }

        public ForClassHierarchy(TypeDescription typeDescription, TypeDescription accessingType) {
            super(accessingType);
            this.typeDescription = typeDescription;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected FieldList<FieldDescription> locate(ElementMatcher<? super FieldDescription> matcher) {
            for (TypeDefinition typeDefinition : typeDescription) {
                FieldList<?> candidates = typeDefinition.getDeclaredFields().filter(matcher);
                if (!candidates.isEmpty()) {
                    return (FieldList<FieldDescription>) candidates;
                }
            }
            return new FieldList.Empty<FieldDescription>();
        }
    }
}
