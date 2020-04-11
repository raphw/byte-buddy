package net.bytebuddy.description.type;

import net.bytebuddy.description.DeclaredByType;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.method.MethodDescription;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public interface RecordComponentDescription extends DeclaredByType, NamedElement, AnnotationSource {

    TypeDescription.Generic getType();

    MethodDescription.InDefinedShape getAccessor();

    abstract class AbstractBase implements RecordComponentDescription {

        @Override
        public int hashCode() {
            return getActualName().hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (!(other instanceof RecordComponentDescription)) {
                return false;
            }
            RecordComponentDescription recordComponentDescription = (RecordComponentDescription) other;
            return getActualName().equals(recordComponentDescription.getActualName());
        }

        @Override
        public String toString() {
            return getType().getTypeName() + " " + getActualName();
        }
    }

    class ForLoadedRecordComponent extends AbstractBase {

        protected static final Dispatcher DISPATCHER = null; // TODO

        private final AnnotatedElement recordComponent;

        protected ForLoadedRecordComponent(AnnotatedElement recordComponent) {
            this.recordComponent = recordComponent;
        }

        public static RecordComponentDescription of(Object recordComponent) {
            if (!DISPATCHER.isInstance(recordComponent)) {
                throw new IllegalArgumentException("Not a record component: " + recordComponent);
            }
            return new ForLoadedRecordComponent((AnnotatedElement) recordComponent);
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription.Generic getType() {
            return new TypeDescription.Generic.LazyProjection.OfRecordComponent(recordComponent);
        }

        /**
         * {@inheritDoc}
         */
        public MethodDescription.InDefinedShape getAccessor() {
            return new MethodDescription.ForLoadedMethod(DISPATCHER.getAccessor(recordComponent));
        }

        /**
         * {@inheritDoc}
         */
        public TypeDefinition getDeclaringType() {
            return TypeDescription.ForLoadedType.of(DISPATCHER.getDeclaringType(recordComponent));
        }

        /**
         * {@inheritDoc}
         */
        public String getActualName() {
            return DISPATCHER.getName(recordComponent);
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.ForLoadedAnnotations(recordComponent.getDeclaredAnnotations());
        }

        protected interface Dispatcher {

            boolean isInstance(Object instance);

            String getName(Object recordComponent);

            Class<?> getDeclaringType(Object recordComponent);

            Method getAccessor(Object recordComponent);

            Class<?> getType(Object recordComponent);

            Type getGenericType(Object recordComponent);

            AnnotatedElement getAnnotatedType(Object recordComponent);
        }
    }
}
