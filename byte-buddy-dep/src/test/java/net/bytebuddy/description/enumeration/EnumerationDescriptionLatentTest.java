package net.bytebuddy.description.enumeration;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

public class EnumerationDescriptionLatentTest extends AbstractEnumerationDescriptionTest {

    @Override
    protected EnumerationDescription describe(Enum<?> enumeration,
                                              Class<?> carrierType,
                                              MethodDescription.InDefinedShape annotationMethod) {
        return new EnumerationDescription.Latent(new TypeDescription.ForLoadedType(enumeration.getDeclaringClass()), enumeration.name());
    }
}
