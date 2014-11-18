package net.bytebuddy.instrumentation.attribute.annotation;

import net.bytebuddy.instrumentation.method.MethodDescription;

public class AnnotationDescriptionEnumerationValueForLoadedEnumerationTest extends AbstractEnumerationValueTest {

    @Override
    protected AnnotationDescription.EnumerationValue describe(Enum<?> enumeration,
                                                              Class<?> carrierType,
                                                              MethodDescription annotationMethod) {
        return new AnnotationDescription.EnumerationValue.ForLoadedEnumeration(enumeration);
    }
}
