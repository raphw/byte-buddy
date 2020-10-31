package net.bytebuddy.description.type;

import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.mockito.Mockito.mock;

public class TypeDescriptionArrayProjectionTest extends AbstractTypeDescriptionTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    protected TypeDescription describe(Class<?> type) {
        return TypeDescription.ArrayProjection.of(TypeDescription.ForLoadedType.of(type), 0);
    }

    protected TypeDescription.Generic describeType(Field field) {
        return TypeDefinition.Sort.describe(field.getGenericType(), TypeDescription.Generic.AnnotationReader.DISPATCHER.resolveFieldType(field));
    }

    protected TypeDescription.Generic describeReturnType(Method method) {
        return TypeDefinition.Sort.describe(method.getGenericReturnType(), TypeDescription.Generic.AnnotationReader.DISPATCHER.resolveReturnType(method));
    }

    protected TypeDescription.Generic describeParameterType(Method method, int index) {
        return TypeDefinition.Sort.describe(method.getGenericParameterTypes()[index],
                TypeDescription.Generic.AnnotationReader.DISPATCHER.resolveParameterType(method, index));
    }

    protected TypeDescription.Generic describeExceptionType(Method method, int index) {
        return TypeDefinition.Sort.describe(method.getGenericExceptionTypes()[index],
                TypeDescription.Generic.AnnotationReader.DISPATCHER.resolveExceptionType(method, index));
    }

    protected TypeDescription.Generic describeSuperClass(Class<?> type) {
        return TypeDefinition.Sort.describe(type.getGenericSuperclass(), TypeDescription.Generic.AnnotationReader.DISPATCHER.resolveSuperClassType(type));
    }

    protected TypeDescription.Generic describeInterfaceType(Class<?> type, int index) {
        return TypeDefinition.Sort.describe(type.getGenericInterfaces()[index], TypeDescription.Generic.AnnotationReader.DISPATCHER.resolveInterfaceType(type, index));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArity() throws Exception {
        TypeDescription.ArrayProjection.of(mock(TypeDescription.class), -1);
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(16)
    public void testTypeVariableU() throws Exception {
        super.testTypeVariableU();
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(16)
    public void testTypeVariableV() throws Exception {
        super.testTypeVariableV();
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(16)
    public void testTypeVariableW() throws Exception {
        super.testTypeVariableW();
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(16)
    public void testTypeVariableX() throws Exception {
        super.testTypeVariableX();
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(9)
    public void testTypeAnnotationOwnerType() throws Exception {
        super.testTypeAnnotationOwnerType();
    }

    @Test
    @Override
    @Ignore("The OpenJDK reflection API does not currently support generic receiver types")
    public void testGenericTypeAnnotationReceiverTypeOnMethod() throws Exception {
        super.testGenericTypeAnnotationReceiverTypeOnMethod();
    }

    @Test
    @Override
    @Ignore("The OpenJDK reflection API does not currently support generic receiver types")
    public void testGenericNestedTypeAnnotationReceiverTypeOnMethod() throws Exception {
        super.testGenericNestedTypeAnnotationReceiverTypeOnMethod();
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(9)
    public void testGenericNestedTypeAnnotationReceiverTypeOnConstructor() throws Exception {
        super.testGenericNestedTypeAnnotationReceiverTypeOnConstructor();
    }

    @Test
    @Override
    @Ignore("The OpenJDK reflection API does not currently support generic receiver types")
    public void testGenericInnerTypeAnnotationReceiverTypeOnConstructor() throws Exception {
        super.testGenericInnerTypeAnnotationReceiverTypeOnConstructor();
    }

    @Test
    @Override
    @Ignore("The OpenJDK reflection API does not currently support generic receiver types")
    public void testGenericInnerTypeAnnotationReceiverTypeOnMethod() throws Exception {
        super.testGenericInnerTypeAnnotationReceiverTypeOnMethod();
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(9)
    public void testTypeAnnotationNonGenericInnerType() throws Exception {
        super.testTypeAnnotationNonGenericInnerType();
    }
}
