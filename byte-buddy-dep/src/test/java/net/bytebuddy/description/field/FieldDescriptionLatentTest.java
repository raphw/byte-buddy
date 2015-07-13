package net.bytebuddy.description.field;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class FieldDescriptionLatentTest extends AbstractFieldDescriptionTest {

    private static final String FOO = "foo";

    @Override
    protected FieldDescription describe(Field field) {
        return new FieldDescription.Latent(new TypeDescription.ForLoadedType(field.getDeclaringClass()),
                field.getName(),
                field.getModifiers(),
                GenericTypeDescription.Sort.describe(field.getGenericType()),
                new AnnotationList.ForLoadedAnnotation(field.getDeclaredAnnotations()));
    }
}
