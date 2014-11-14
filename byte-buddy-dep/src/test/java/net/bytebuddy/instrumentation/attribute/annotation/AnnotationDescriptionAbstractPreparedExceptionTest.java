package net.bytebuddy.instrumentation.attribute.annotation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Test;

import java.lang.annotation.Annotation;

public class AnnotationDescriptionAbstractPreparedExceptionTest {

    @Test(expected = ClassNotFoundException.class)
    public void testThrowWithoutClassLoader() throws Exception {
        new PseudoDescription().loadSilent();
    }

    @Test(expected = ClassNotFoundException.class)
    public void testThrowWithClassLoader() throws Exception {
        new PseudoDescription().loadSilent(getClass().getClassLoader());
    }

    private static class PseudoDescription extends AnnotationDescription.AbstractAnnotationDescription.ForPrepared<Annotation> {

        @Override
        public Annotation load() throws ClassNotFoundException {
            throw new ClassNotFoundException();
        }

        @Override
        public Annotation load(ClassLoader classLoader) throws ClassNotFoundException {
            throw new ClassNotFoundException();
        }

        @Override
        public Object getValue(MethodDescription methodDescription) {
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
