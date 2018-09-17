package net.bytebuddy.description.enumeration;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

public class EnumerationDescriptionLatentTest extends AbstractEnumerationDescriptionTest {

    protected EnumerationDescription describe(Enum<?> enumeration,
                                              Class<?> carrierType,
                                              MethodDescription.InDefinedShape annotationMethod) {
        return new EnumerationDescription.Latent(TypeDescription.ForLoadedType.of(enumeration.getDeclaringClass()), enumeration.name());
    }
}
