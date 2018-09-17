package net.bytebuddy.description.annotation;

import java.util.List;

public class AnnotationListExplicitTest extends AbstractAnnotationListTest<AnnotationDescription> {

    protected AnnotationDescription getFirst() throws Exception {
        return new AnnotationDescription.ForLoadedAnnotation<Foo>(Holder.class.getAnnotation(Foo.class));
    }

    protected AnnotationDescription getSecond() throws Exception {
        return new AnnotationDescription.ForLoadedAnnotation<Bar>(Holder.class.getAnnotation(Bar.class));
    }

    protected AnnotationList asList(List<AnnotationDescription> elements) {
        return new AnnotationList.Explicit(elements);
    }

    protected AnnotationDescription asElement(AnnotationDescription element) {
        return element;
    }
}
