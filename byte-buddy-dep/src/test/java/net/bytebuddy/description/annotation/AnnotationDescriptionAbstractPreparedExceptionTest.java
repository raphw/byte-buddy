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

    @Test(expected = IllegalStateException.class)
    public void testThrowWithClassLoader() throws Exception {
        new PseudoDescription().loadSilent(getClass().getClassLoader());
    }

    private static class PseudoDescription extends AnnotationDescription.AbstractBase.ForPrepared<Annotation> {

        @Override
        public Annotation load() throws ClassNotFoundException {
            throw new ClassNotFoundException();
        }

        @Override
        public Annotation load(ClassLoader classLoader) throws ClassNotFoundException {
            throw new ClassNotFoundException();
        }

        @Override
        public Object getValue(MethodDescription.InDefinedShape methodDescription) {
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
