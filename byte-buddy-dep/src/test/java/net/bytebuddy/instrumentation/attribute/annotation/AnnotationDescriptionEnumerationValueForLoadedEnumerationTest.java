package net.bytebuddy.instrumentation.attribute.annotation;

public class AnnotationDescriptionEnumerationValueForLoadedEnumerationTest extends AbstractEnumerationValueTest<AnnotationDescriptionEnumerationValueForLoadedEnumerationTest.Sample> {

    @Override
    protected Sample first() {
        return Sample.FIRST;
    }

    @Override
    protected Sample second() {
        return Sample.SECOND;
    }

    @Override
    protected AnnotationDescription.EnumerationValue firstValue() {
        return new AnnotationDescription.EnumerationValue.ForLoadedEnumeration(Sample.FIRST);
    }

    @Override
    protected AnnotationDescription.EnumerationValue secondValue() {
        return new AnnotationDescription.EnumerationValue.ForLoadedEnumeration(Sample.SECOND);
    }

    public static enum Sample {
        FIRST,
        SECOND
    }
}
