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

        public Annotation load() throws ClassNotFoundException {
            throw new ClassNotFoundException();
        }

        public AnnotationValue<?, ?> getValue(MethodDescription.InDefinedShape property) {
            throw new UnsupportedOperationException();
        }

        public TypeDescription getAnnotationType() {
            throw new UnsupportedOperationException();
        }

        public <T extends Annotation> Loadable<T> prepare(Class<T> annotationType) {
            throw new UnsupportedOperationException();
        }
    }
}
