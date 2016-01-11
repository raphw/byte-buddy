package net.bytebuddy.description.type;

import net.bytebuddy.description.method.MethodDescription;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TypeDescriptionForLoadedTypeTest extends AbstractTypeDescriptionTest {

    @Override
    protected TypeDescription describe(Class<?> type) {
        return new TypeDescription.ForLoadedType(type);
    }

    @Override
    protected TypeDescription.Generic describeType(Field field) {
        return TypeDefinition.Sort.describe(field.getGenericType());
    }

    @Override
    protected TypeDescription.Generic describeReturnType(Method method) {
        return new MethodDescription.ForLoadedMethod(method).getReturnType();
    }

    @Override
    protected TypeDescription.Generic describeParameterType(Method method, int index) {
        return new MethodDescription.ForLoadedMethod(method).getParameters().get(index).getType();
    }

    @Override
    protected TypeDescription.Generic describeExceptionType(Method method, int index) {
        return new MethodDescription.ForLoadedMethod(method).getExceptionTypes().get(index);
    }

    @Override
    protected TypeDescription.Generic describeSuperType(Class<?> type) {
        return new TypeDescription.ForLoadedType(type).getSuperType();
    }

    @Override
    protected TypeDescription.Generic describeInterfaceType(Class<?> type, int index) {
        return new TypeDescription.ForLoadedType(type).getInterfaces().get(index);
    }
}
