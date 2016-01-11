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
    protected TypeDescription.Generic describeType(Field field) {
        return TypeDefinition.Sort.describe(field.getGenericType());
    }

    @Override
    protected TypeDescription.Generic describeReturnType(Method method) {
        return TypeDefinition.Sort.describe(method.getGenericReturnType());
    }

    @Override
    protected TypeDescription.Generic describeParameterType(Method method, int index) {
        return TypeDefinition.Sort.describe(method.getGenericParameterTypes()[index]);
    }

    @Override
    protected TypeDescription.Generic describeExceptionType(Method method, int index) {
        return TypeDefinition.Sort.describe(method.getGenericExceptionTypes()[index]);
    }

    @Override
    protected TypeDescription.Generic describeSuperType(Class<?> type) {
        return TypeDefinition.Sort.describe(type.getGenericSuperclass());
    }

    @Override
    protected TypeDescription.Generic describeInterfaceType(Class<?> type, int index) {
        return TypeDefinition.Sort.describe(type.getGenericInterfaces()[index]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArity() throws Exception {
        TypeDescription.ArrayProjection.of(mock(TypeDescription.class), -1);
    }
}
