package net.bytebuddy.description.type;

import net.bytebuddy.description.TypeVariableSource;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypeDescriptionGenericOfTypeVariableWithAnnotationOverlayTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    private TypeDescription.Generic typeVariable;

    @Mock
    private TypeDescription.Generic original, upperBound, lowerBound;

    @Mock
    private TypeVariableSource typeVariableSource;

    @Mock
    private AnnotationDescription annotationDescription;

    @Before
    public void setUp() throws Exception {
        when(original.getSymbol()).thenReturn(FOO);
        when(upperBound.asGenericType()).thenReturn(upperBound);
        when(lowerBound.asGenericType()).thenReturn(lowerBound);
        when(original.getUpperBounds()).thenReturn(new TypeList.Generic.Explicit(upperBound));
        when(original.getLowerBounds()).thenReturn(new TypeList.Generic.Explicit(lowerBound));
        when(original.getTypeVariableSource()).thenReturn(typeVariableSource);
        typeVariable = new TypeDescription.Generic.OfTypeVariable.WithAnnotationOverlay(original, Collections.singletonList(annotationDescription));
    }

    @Test
    public void testSymbol() throws Exception {
        assertThat(typeVariable.getSymbol(), is(FOO));
    }

    @Test
    public void testTypeName() throws Exception {
        assertThat(typeVariable.getTypeName(), is(FOO));
    }

    @Test
    public void testToString() throws Exception {
        assertThat(typeVariable.toString(), is(FOO));
    }

    @Test
    public void testSort() throws Exception {
        assertThat(typeVariable.getSort(), is(TypeDefinition.Sort.VARIABLE));
    }

    @Test
    public void testStackSize() throws Exception {
        assertThat(typeVariable.getStackSize(), is(StackSize.SINGLE));
    }

    @Test
    public void testArray() throws Exception {
        assertThat(typeVariable.isArray(), is(false));
    }

    @Test
    public void testPrimitive() throws Exception {
        assertThat(typeVariable.isPrimitive(), is(false));
    }

    @Test
    public void testEquals() throws Exception {
        assertThat(typeVariable, is(typeVariable));
        assertThat(typeVariable, is(typeVariable(FOO, typeVariableSource, annotationDescription)));
        assertThat(typeVariable, is(typeVariable(FOO, typeVariableSource)));
        assertThat(typeVariable, not(typeVariable(BAR, typeVariableSource, annotationDescription)));
        assertThat(typeVariable, not(typeVariable(FOO, mock(TypeVariableSource.class), annotationDescription)));
        assertThat(typeVariable, not(TypeDescription.Generic.OBJECT));
        assertThat(typeVariable, not(new Object()));
        assertThat(typeVariable, not(equalTo(null)));
    }

    private static TypeDescription.Generic typeVariable(String symbol, TypeVariableSource typeVariableSource, AnnotationDescription... annotationDescription) {
        TypeDescription.Generic typeVariable = mock(TypeDescription.Generic.class);
        when(typeVariable.getSort()).thenReturn(TypeDefinition.Sort.VARIABLE);
        when(typeVariable.getSymbol()).thenReturn(symbol);
        when(typeVariable.getTypeVariableSource()).thenReturn(typeVariableSource);
        when(typeVariable.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(annotationDescription));
        return typeVariable;
    }

    @Test
    public void testAnnotations() throws Exception {
        assertThat(typeVariable.getDeclaredAnnotations().size(), is(1));
        assertThat(typeVariable.getDeclaredAnnotations().contains(annotationDescription), is(true));
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(typeVariable.hashCode(), is(typeVariableSource.hashCode() ^ FOO.hashCode()));
    }

}
