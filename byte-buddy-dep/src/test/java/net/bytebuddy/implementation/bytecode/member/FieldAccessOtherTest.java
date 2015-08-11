package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FieldAccessOtherTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private EnumerationDescription enumerationDescription;

    @Mock
    private GenericTypeDescription genericType, declaredType;

    @Mock
    private TypeDescription enumerationType;

    @Mock
    private FieldDescription.InDefinedShape fieldDescription;

    @Mock
    private FieldDescription genericField;

    @Before
    public void setUp() throws Exception {
        when(genericField.asDefined()).thenReturn(fieldDescription);
        when(genericField.getType()).thenReturn(genericType);
        when(fieldDescription.getType()).thenReturn(declaredType);
        when(enumerationDescription.getEnumerationType()).thenReturn(enumerationType);
        when(enumerationDescription.getValue()).thenReturn(FOO);
        when(enumerationType.getDeclaredFields())
                .thenReturn(new FieldList.Explicit<FieldDescription.InDefinedShape>(Collections.singletonList(fieldDescription)));
    }

    @Test
    public void testEnumerationDescription() throws Exception {
        when(fieldDescription.isPublic()).thenReturn(true);
        when(fieldDescription.isStatic()).thenReturn(true);
        when(fieldDescription.isEnum()).thenReturn(true);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        StackManipulation stackManipulation = FieldAccess.forEnumeration(enumerationDescription);
        assertThat(stackManipulation.isValid(), is(true));
    }

    @Test
    public void testEnumerationDescriptionWithIllegalName() throws Exception {
        when(fieldDescription.isPublic()).thenReturn(true);
        when(fieldDescription.isStatic()).thenReturn(true);
        when(fieldDescription.isEnum()).thenReturn(true);
        when(fieldDescription.getSourceCodeName()).thenReturn(BAR);
        StackManipulation stackManipulation = FieldAccess.forEnumeration(enumerationDescription);
        assertThat(stackManipulation.isValid(), is(false));
    }

    @Test
    public void testEnumerationDescriptionWithIllegalOwnership() throws Exception {
        when(fieldDescription.isPublic()).thenReturn(true);
        when(fieldDescription.isStatic()).thenReturn(false);
        when(fieldDescription.isEnum()).thenReturn(true);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        StackManipulation stackManipulation = FieldAccess.forEnumeration(enumerationDescription);
        assertThat(stackManipulation.isValid(), is(false));
    }

    @Test
    public void testEnumerationDescriptionWithIllegalVisibility() throws Exception {
        when(fieldDescription.isPublic()).thenReturn(false);
        when(fieldDescription.isStatic()).thenReturn(true);
        when(fieldDescription.isEnum()).thenReturn(true);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        StackManipulation stackManipulation = FieldAccess.forEnumeration(enumerationDescription);
        assertThat(stackManipulation.isValid(), is(false));
    }

    @Test
    public void testEnumerationDescriptionNonEnumeration() throws Exception {
        when(fieldDescription.isPublic()).thenReturn(true);
        when(fieldDescription.isStatic()).thenReturn(true);
        when(fieldDescription.isEnum()).thenReturn(false);
        when(fieldDescription.getSourceCodeName()).thenReturn(FOO);
        StackManipulation stackManipulation = FieldAccess.forEnumeration(enumerationDescription);
        assertThat(stackManipulation.isValid(), is(false));
    }

    @Test
    public void testGenericFieldAccessGetter() throws Exception {
        TypeDescription genericErasure = mock(TypeDescription.class), declaredErasure = mock(TypeDescription.class);
        when(genericType.asErasure()).thenReturn(genericErasure);
        when(declaredType.asErasure()).thenReturn(declaredErasure);
        StackManipulation stackManipulation = FieldAccess.forField(genericField).getter();
        assertThat(stackManipulation.isValid(), is(true));
        assertThat(stackManipulation, is((StackManipulation) new StackManipulation.Compound(FieldAccess.forField(fieldDescription).getter(),
                TypeCasting.to(genericErasure))));
    }

    @Test
    public void testGenericFieldAccessPutter() throws Exception {
        TypeDescription genericErasure = mock(TypeDescription.class), declaredErasure = mock(TypeDescription.class);
        when(genericType.asErasure()).thenReturn(genericErasure);
        when(declaredType.asErasure()).thenReturn(declaredErasure);
        StackManipulation stackManipulation = FieldAccess.forField(genericField).putter();
        assertThat(stackManipulation.isValid(), is(true));
        assertThat(stackManipulation, is(FieldAccess.forField(fieldDescription).putter()));
    }

    @Test
    public void testGenericFieldAccessGetterEqualErasure() throws Exception {
        TypeDescription declaredErasure = mock(TypeDescription.class);
        when(genericType.asErasure()).thenReturn(declaredErasure);
        when(declaredType.asErasure()).thenReturn(declaredErasure);
        StackManipulation stackManipulation = FieldAccess.forField(genericField).getter();
        assertThat(stackManipulation.isValid(), is(true));
        assertThat(stackManipulation, is(FieldAccess.forField(fieldDescription).getter()));
    }

    @Test
    public void testGenericFieldAccessPutterEqualErasure() throws Exception {
        TypeDescription declaredErasure = mock(TypeDescription.class);
        when(genericType.asErasure()).thenReturn(declaredErasure);
        when(declaredType.asErasure()).thenReturn(declaredErasure);
        StackManipulation stackManipulation = FieldAccess.forField(genericField).putter();
        assertThat(stackManipulation.isValid(), is(true));
        assertThat(stackManipulation, is(FieldAccess.forField(fieldDescription).putter()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldAccess.class).apply();
        ObjectPropertyAssertion.of(FieldAccess.OfGenericField.class).apply();
        ObjectPropertyAssertion.of(FieldAccess.AccessDispatcher.class).apply();
        ObjectPropertyAssertion.of(FieldAccess.AccessDispatcher.FieldGetInstruction.class).apply();
        ObjectPropertyAssertion.of(FieldAccess.AccessDispatcher.FieldPutInstruction.class).apply();
    }
}
