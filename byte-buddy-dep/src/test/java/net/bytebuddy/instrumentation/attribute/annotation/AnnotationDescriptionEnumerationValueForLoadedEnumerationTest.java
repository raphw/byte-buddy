package net.bytebuddy.instrumentation.attribute.annotation;

public class AnnotationDescriptionEnumerationValueForLoadedEnumerationTest extends AbstractEnumerationValueTest {

    @Override
    protected AnnotationDescription.EnumerationValue describe(Enum<?> enumeration) {
        return new AnnotationDescription.EnumerationValue.ForLoadedEnumeration(enumeration);
    }
}
