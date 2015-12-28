package net.bytebuddy.description.type;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.mockito.Mockito.mock;

public class TypeDescriptionArrayProjectionTest extends AbstractTypeDescriptionTest {

    @Override
    protected TypeDescription describe(Class<?> type) {
        return TypeDescription.ArrayProjection.of(new TypeDescription.ForLoadedType(type), 0);
    }

    @Override
    protected TypeDescription.Generic describe(Field field) {
        return TypeDescription.Generic.ForGenericArray.Latent.of(TypeDefinition.Sort.describe(field.getGenericType()), 0);
    }

    @Override
    protected TypeDescription.Generic describe(Method method) {
        return TypeDescription.Generic.ForGenericArray.Latent.of(TypeDefinition.Sort.describe(method.getGenericReturnType()), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArity() throws Exception {
        TypeDescription.ArrayProjection.of(mock(TypeDescription.class), -1);
    }
}
