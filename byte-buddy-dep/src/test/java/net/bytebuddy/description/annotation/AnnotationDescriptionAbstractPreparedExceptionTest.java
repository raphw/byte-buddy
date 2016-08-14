package net.bytebuddy.description.annotation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import java.lang.annotation.Annotation;

public class AnnotationDescriptionAbstractPreparedExceptionTest {

    @Test(expected = IllegalStateException.class)
    public void testThrowWithoutClassLoader() throws Exception {
        new PseudoDescription().loadSilent();
    }

    private static class PseudoDescription extends AnnotationDescription.AbstractBase.ForPrepared<Annotation> {

        @Override
        public Annotation load() throws ClassNotFoundException {
            throw new ClassNotFoundException();
        }

        @Override
        public AnnotationValue<?, ?> getValue(MethodDescription.InDefinedShape property) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TypeDescription getAnnotationType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends Annotation> Loadable<T> prepare(Class<T> annotationType) {
            throw new UnsupportedOperationException();
        }
    }
}
