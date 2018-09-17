package net.bytebuddy.description.field;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;

import java.lang.reflect.Field;

public class FieldDescriptionLatentTest extends AbstractFieldDescriptionTest {

    protected FieldDescription.InDefinedShape describe(Field field) {
        return new FieldDescription.Latent(TypeDescription.ForLoadedType.of(field.getDeclaringClass()),
                field.getName(),
                field.getModifiers(),
                TypeDefinition.Sort.describe(field.getGenericType()),
                new AnnotationList.ForLoadedAnnotations(field.getDeclaredAnnotations()));
    }
}
