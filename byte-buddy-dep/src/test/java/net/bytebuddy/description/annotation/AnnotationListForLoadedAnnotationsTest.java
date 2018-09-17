package net.bytebuddy.description.annotation;

import java.lang.annotation.Annotation;
import java.util.List;

public class AnnotationListForLoadedAnnotationsTest extends AbstractAnnotationListTest<Annotation> {

    protected Annotation getFirst() throws Exception {
        return Holder.class.getAnnotation(Foo.class);
    }

    protected Annotation getSecond() throws Exception {
        return Holder.class.getAnnotation(Bar.class);
    }

    protected AnnotationList asList(List<Annotation> elements) {
        return new AnnotationList.ForLoadedAnnotations(elements);
    }

    protected AnnotationDescription asElement(Annotation element) {
        return new AnnotationDescription.ForLoadedAnnotation<Annotation>(element);
    }
}
