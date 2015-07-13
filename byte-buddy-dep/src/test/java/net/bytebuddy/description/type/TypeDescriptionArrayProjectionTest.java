package net.bytebuddy.description.type;

import net.bytebuddy.description.type.generic.GenericTypeDescription;
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
    protected GenericTypeDescription describe(Field field) {
        return GenericTypeDescription.ForGenericArray.Latent.of(GenericTypeDescription.Sort.describe(field.getGenericType()), 0);
    }

    @Override
    protected GenericTypeDescription describe(Method method) {
        return GenericTypeDescription.ForGenericArray.Latent.of(GenericTypeDescription.Sort.describe(method.getGenericReturnType()), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArity() throws Exception {
        TypeDescription.ArrayProjection.of(mock(TypeDescription.class), -1);
    }
}
