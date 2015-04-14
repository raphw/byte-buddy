package net.bytebuddy.description.annotation;

import net.bytebuddy.description.method.MethodDescription;

public class AnnotationDescriptionEnumerationValueForLoadedEnumerationTest extends AbstractEnumerationValueTest {

    @Override
    protected AnnotationDescription.EnumerationValue describe(Enum<?> enumeration,
                                                              Class<?> carrierType,
                                                              MethodDescription annotationMethod) {
        return new AnnotationDescription.EnumerationValue.ForLoadedEnumeration(enumeration);
    }
}
