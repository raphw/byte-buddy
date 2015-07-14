package net.bytebuddy.description.annotation;

import java.lang.annotation.Annotation;
import java.util.List;

public class AnnotationListForLoadedAnnotationTest extends AbstractAnnotationListTest<Annotation> {

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
        return new AnnotationList.ForLoadedAnnotation(elements);
    }

    @Override
    protected AnnotationDescription asElement(Annotation element) {
        return new AnnotationDescription.ForLoadedAnnotation<Annotation>(element);
    }
}
