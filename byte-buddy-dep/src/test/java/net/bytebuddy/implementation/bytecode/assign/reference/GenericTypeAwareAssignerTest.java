package net.bytebuddy.implementation.bytecode.assign.reference;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.Serializable;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class GenericTypeAwareAssignerTest {

    @Test
    public void testNonGenericToAssignableGenericType() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class);
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedString").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(true));
    }

    @Test
    public void testNonGenericToAssignableNonGenericType() throws Exception {
        TypeDescription.Generic typeDescription = TypeDefinition.Sort.describe(Sample.class);
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(typeDescription, typeDescription, Assigner.Typing.STATIC).isValid(), is(true));
    }

    @Test
    public void testNonGenericToNonAssignableGenericType() throws Exception {
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedString").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class), target, Assigner.Typing.STATIC).isValid(), is(false));
    }

    @Test
    public void testNonGenericToNonAssignableNonGenericType() throws Exception {
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class);
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class), target, Assigner.Typing.STATIC).isValid(), is(false));
    }

    @Test
    public void testParameterizedTypeToAssignableEqualParameterizedType() throws Exception {
        TypeDescription.Generic typeDescription = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedString").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(typeDescription, typeDescription, Assigner.Typing.STATIC).isValid(), is(true));
    }

    @Test
    public void testParameterizedTypeToNonAssignableEqualParameterizedType() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedObject").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedString").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(false));
    }

    @Test
    public void testParameterizedTypeToAssignableWildcardParameterizedType() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedString").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcard").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(true));
    }

    @Test
    public void testParameterizedTypeToAssignableWildcardUpperBoundParameterizedType() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedString").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardStringUpperBound").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(true));
    }

    @Test
    public void testParameterizedTypeToAssignableWildcardLowerBoundParameterizedType() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedString").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardStringLowerBound").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(true));
    }

    @Test
    public void testParameterizedTypeToNonAssignableWildcardLowerBoundParameterizedType() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedString").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardObjectLowerBound").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(false));
    }

    @Test
    public void testParameterizedTypeToNonAssignableWildcardUpperBoundParameterizedType() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedObject").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardStringUpperBound").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(false));
    }

    @Test
    public void testParameterizedTypeToNonAssignableGenericArrayType() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedObject").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedArray").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(false));
    }

    @Test
    public void testParameterizedTypeToEqualRawType() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedString").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class);
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(true));
    }

    @Test
    public void testParameterizedTypeToSuperType() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedString").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class), Assigner.Typing.STATIC).isValid(), is(true));
    }

    @Test
    public void testParameterizedTypeToVariable() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedString").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("variable").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(false));
    }

    @Test
    public void testGenericArrayToAssignableGenericArray() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(SubSample.class.getDeclaredField("parameterizedArray").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedArray").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(true));
    }

    @Test
    public void testGenericArrayToAssignableNonGenericArray() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(SubSample.class.getDeclaredField("parameterizedArray").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(SubSample.class.getDeclaredField("objectArray").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(true));
    }

    @Test
    public void testGenericArrayToNonAssignableGenericArray() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(SubSample.class.getDeclaredField("parameterizedArray").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(SubSample.class.getDeclaredField("parameterizedNestedArray").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(false));
    }

    @Test
    public void testVariableToAssignableVariable() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(TypeVariables.class.getDeclaredField("s").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(TypeVariables.class.getDeclaredField("t").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(true));
    }

    @Test
    public void testVariableToAssignableNonGenericBound() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(TypeVariables.class.getDeclaredField("u").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(String.class);
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(true));
    }

    @Test
    public void testVariableToNonAssignableNonGenericBound() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(TypeVariables.class.getDeclaredField("s").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(String.class);
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(false));
    }

    @Test
    public void testWildcardToAssignableWildcard() throws Exception {
        TypeDescription.Generic typeDescription = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcard").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(typeDescription, typeDescription, Assigner.Typing.STATIC).isValid(), is(true));
    }

    @Test
    public void testWildcardWithUpperBoundToAssignableWildcard() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardStringUpperBound").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcard").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(true));
    }

    @Test
    public void testWildcardWithUpperBoundToAssignableWildcardWithUpperBound() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardStringUpperBound").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardSerializableUpperBound").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(true));
    }

    @Test
    public void testWildcardWithUpperBoundToNonAssignableWildcardWithUpperBound() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardSerializableUpperBound").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardStringUpperBound").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(false));
    }

    @Test
    public void testWildcardWithUpperBoundNotAssignableToWildcardWithLowerBound() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardStringUpperBound").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardStringLowerBound").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(false));
    }

    @Test
    public void testWildcardWithLowerBoundToAssignableWildcard() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardStringLowerBound").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcard").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(true));
    }

    @Test
    public void testWildcardWithLowerBoundToAssignableWildcardWithLowerBound() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardObjectLowerBound").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardStringLowerBound").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(true));
    }

    @Test
    public void testWildcardWithLowerBoundToNonAssignableWildcardWithLowerBound() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardStringLowerBound").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardObjectLowerBound").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(false));
    }

    @Test
    public void testWildcardWithLowerBoundNotAssignableToWildcardWithUpperBound() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardStringLowerBound").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardStringUpperBound").getGenericType());
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC).isValid(), is(false));
    }

    @Test
    public void testDynamicTypingRawAssignability() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardStringLowerBound").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedWildcardStringUpperBound").getGenericType());
        StackManipulation assignment = GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.DYNAMIC);
        assertThat(assignment.isValid(), is(true));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = assignment.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
    }

    @Test
    public void testDynamicTypingNoRawAssignability() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(Sample.class.getDeclaredField("parameterizedArray").getGenericType());
        TypeDescription.Generic target = TypeDefinition.Sort.describe(SubSample.class.getDeclaredField("parameterizedArray").getGenericType());
        StackManipulation assignment = GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.DYNAMIC);
        assertThat(assignment.isValid(), is(true));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = assignment.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(SubSample[].class));
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
    }

    @Test
    public void testPrimitiveAssignment() throws Exception {
        TypeDescription.Generic typeDescription = TypeDefinition.Sort.describe(int.class);
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(typeDescription, typeDescription, Assigner.Typing.DYNAMIC).isValid(), is(true));
    }

    @Test
    public void testPrimitiveAssignmentIllegal() throws Exception {
        TypeDescription.Generic source = TypeDefinition.Sort.describe(int.class);
        TypeDescription.Generic target = TypeDefinition.Sort.describe(boolean.class);
        assertThat(GenericTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.DYNAMIC).isValid(), is(false));
    }

    private static class Sample<T> {

        Sample<Object> parameterizedObject;

        Sample<String> parameterizedString;

        Sample<?> parameterizedWildcard;

        Sample<? extends String> parameterizedWildcardStringUpperBound;

        Sample<? extends Serializable> parameterizedWildcardSerializableUpperBound;

        Sample<? super String> parameterizedWildcardStringLowerBound;

        Sample<? super Object> parameterizedWildcardObjectLowerBound;

        Sample<String>[] parameterizedArray;

        T variable;
    }

    private static class SubSample<T> extends Sample<T> {

        SubSample<String>[] parameterizedArray;

        SubSample<String>[][] parameterizedNestedArray;

        Object[] objectArray;
    }

    private static class TypeVariables<T, S extends T, U extends String> {

        T t;

        S s;

        U u;
    }
}
