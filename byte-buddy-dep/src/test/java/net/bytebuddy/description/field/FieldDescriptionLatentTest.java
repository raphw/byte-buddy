package net.bytebuddy.description.field;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;

import java.lang.reflect.Field;

public class FieldDescriptionLatentTest extends AbstractFieldDescriptionTest {

    @Override
    protected FieldDescription.InDefinedShape describe(Field field) {
        return new FieldDescription.Latent(new TypeDescription.ForLoadedType(field.getDeclaringClass()),
                field.getName(),
                field.getModifiers(),
                GenericTypeDescription.Sort.describe(field.getGenericType()),
                new AnnotationList.ForLoadedAnnotation(field.getDeclaredAnnotations()));
    }
}
