package net.bytebuddy.pool;

import net.bytebuddy.description.annotation.AbstractAnnotationListTest;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import org.junit.After;
import org.junit.Before;

import java.lang.annotation.Annotation;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.anyOf;

public class TypePoolDefaultLazyAnnotationListTest extends AbstractAnnotationListTest<Annotation> {

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    @Override
    protected Annotation getFirst() throws Exception {
        return Holder.class.getAnnotation(Foo.class);
    }

    @Override
    protected Annotation getSecond() throws Exception {
        return Holder.class.getAnnotation(Bar.class);
    }

    @Override
    protected AnnotationList asList(List<Annotation> elements) {
        return typePool.describe(Holder.class.getName()).resolve().getDeclaredAnnotations().filter(anyOf(elements.toArray(new Annotation[elements.size()])));
    }

    @Override
    protected AnnotationDescription asElement(Annotation element) {
        return AnnotationDescription.ForLoadedAnnotation.of(element);
    }
}
