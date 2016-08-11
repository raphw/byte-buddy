package net.bytebuddy.description.annotation;

import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.test.utility.ClassFileExtraction;
import org.junit.Before;

import java.lang.annotation.Annotation;

public class AnnotationDescriptionForLoadedAnnotationDifferentClassLoaderTest extends AbstractAnnotationDescriptionTest {

    private ClassLoader classLoader;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                ClassFileExtraction.of(Sample.class, SampleDefault.class, Other.class, SampleEnumeration.class, ExplicitTarget.class),
                null,
                ByteArrayClassLoader.PersistenceHandler.LATENT,
                PackageDefinitionStrategy.Trivial.INSTANCE);
    }

    @Override
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
