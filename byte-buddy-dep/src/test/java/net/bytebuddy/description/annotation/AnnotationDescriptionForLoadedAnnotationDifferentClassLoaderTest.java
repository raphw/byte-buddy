package net.bytebuddy.description.annotation;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Before;

import java.lang.annotation.Annotation;

public class AnnotationDescriptionForLoadedAnnotationDifferentClassLoaderTest extends AbstractAnnotationDescriptionTest {

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                ClassFileLocator.ForClassLoader.readToNames(Sample.class,
                        SampleDefault.class,
                        Other.class,
                        SampleEnumeration.class,
                        ExplicitTarget.class));
    }

    @SuppressWarnings("unchecked")
    protected AnnotationDescription describe(Annotation annotation, Class<?> declaringType) {
        try {
            return AnnotationDescription.ForLoadedAnnotation.of(AnnotationDescription.ForLoadedAnnotation.of(annotation)
                    .prepare((Class<Annotation>) classLoader.loadClass(annotation.annotationType().getName()))
                    .load());
        } catch (ClassNotFoundException exception) {
            throw new AssertionError(exception);
        }
    }
}
