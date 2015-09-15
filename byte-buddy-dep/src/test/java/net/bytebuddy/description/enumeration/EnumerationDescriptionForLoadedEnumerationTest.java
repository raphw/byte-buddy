package net.bytebuddy.description.enumeration;

import net.bytebuddy.description.method.MethodDescription;

public class EnumerationDescriptionForLoadedEnumerationTest extends AbstractEnumerationDescriptionTest {

    @Override
    protected EnumerationDescription describe(Enum<?> enumeration,
                                              Class<?> carrierType,
                                              MethodDescription.InDefinedShape annotationMethod) {
        return new EnumerationDescription.ForLoadedEnumeration(enumeration);
    }
}
