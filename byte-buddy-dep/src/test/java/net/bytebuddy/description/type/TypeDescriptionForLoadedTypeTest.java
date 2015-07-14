package net.bytebuddy.description.type;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TypeDescriptionForLoadedTypeTest extends AbstractTypeDescriptionTest {

    @Override
    protected TypeDescription describe(Class<?> type) {
        return new TypeDescription.ForLoadedType(type);
    }

    @Override
    protected GenericTypeDescription describe(Field field) {
        return GenericTypeDescription.Sort.describe(field.getGenericType());
    }

    @Override
    protected GenericTypeDescription describe(Method method) {
        return new MethodDescription.ForLoadedMethod(method).getReturnType();
    }
}
