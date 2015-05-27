package net.bytebuddy.description.type;

import net.bytebuddy.description.type.generic.GenericTypeDescription;

import java.lang.reflect.Field;

public class TypeDescriptionForLoadedTypeTest extends AbstractTypeDescriptionTest {

    @Override
    protected TypeDescription describe(Class<?> type) {
        return new TypeDescription.ForLoadedType(type);
    }

    @Override
    protected GenericTypeDescription describe(Field field) {
        return GenericTypeDescription.Sort.describe(field.getGenericType());
    }
}
