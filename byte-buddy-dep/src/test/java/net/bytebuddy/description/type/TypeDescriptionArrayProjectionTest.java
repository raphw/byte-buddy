package net.bytebuddy.description.type;

import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Comparator;

import static org.mockito.Mockito.mock;

public class TypeDescriptionArrayProjectionTest extends AbstractTypeDescriptionTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    protected TypeDescription describe(Class<?> type) {
        return TypeDescription.ArrayProjection.of(TypeDescription.ForLoadedType.of(type), 0);
    }

    protected TypeDescription.Generic describeType(Field field) {
        return TypeDefinition.Sort.describe(field.getGenericType(),
                new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field));
    }

    protected TypeDescription.Generic describeReturnType(Method method) {
        return TypeDefinition.Sort.describe(method.getGenericReturnType(),
                new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedMethodReturnType(method));
    }

    protected TypeDescription.Generic describeParameterType(Method method, int index) {
        return TypeDefinition.Sort.describe(method.getGenericParameterTypes()[index],
                new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedExecutableParameterType(method, index));
    }

    protected TypeDescription.Generic describeExceptionType(Method method, int index) {
        Type[] type = method.getGenericExceptionTypes();
        Arrays.sort(type, new Comparator<Type>() {
            public int compare(Type left, Type right) {
                return left.toString().compareTo(right.toString());
            }
        });
        return TypeDefinition.Sort.describe(type[index],
                new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedExecutableExceptionType(method, index));
    }

    protected TypeDescription.Generic describeSuperClass(Class<?> type) {
        return TypeDefinition.Sort.describe(type.getGenericSuperclass(),
                new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedSuperClass(type));
    }

    protected TypeDescription.Generic describeInterfaceType(Class<?> type, int index) {
        return TypeDefinition.Sort.describe(type.getGenericInterfaces()[index],
                new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedInterface(type, index));
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
    @JavaVersionRule.Enforce(16)
    public void testGenericTypeAnnotationReceiverTypeOnMethod() throws Exception {
        super.testGenericTypeAnnotationReceiverTypeOnMethod();
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(16)
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
    @JavaVersionRule.Enforce(16)
    public void testGenericInnerTypeAnnotationReceiverTypeOnConstructor() throws Exception {
        super.testGenericInnerTypeAnnotationReceiverTypeOnConstructor();
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(16)
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
