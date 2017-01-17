package net.bytebuddy.description.type;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class TypeDescriptionGenericOfTypeVariableSymbolicTest {

    private static final String FOO = "foo", BAR = "bar";

    public TestRule mockitoRule = new MockitoRule(this);

    private TypeDescription.Generic typeVariable;

    @Mock
    private AnnotationDescription annotationDescription;

    @Before
    public void setUp() throws Exception {
        typeVariable = new TypeDescription.Generic.OfTypeVariable.Symbolic(FOO, new AnnotationSource.Explicit(annotationDescription));
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
        assertThat(typeVariable.getSort(), is(TypeDefinition.Sort.VARIABLE_SYMBOLIC));
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
        assertThat(typeVariable, is((TypeDescription.Generic) new TypeDescription.Generic.OfTypeVariable.Symbolic(FOO, new AnnotationSource.Explicit(annotationDescription))));
        assertThat(typeVariable, is((TypeDescription.Generic) new TypeDescription.Generic.OfTypeVariable.Symbolic(FOO, AnnotationSource.Empty.INSTANCE)));
        assertThat(typeVariable, not((TypeDescription.Generic) new TypeDescription.Generic.OfTypeVariable.Symbolic(BAR, AnnotationSource.Empty.INSTANCE)));
        assertThat(typeVariable, not(TypeDescription.Generic.OBJECT));
        assertThat(typeVariable, not(new Object()));
        assertThat(typeVariable, not(equalTo(null)));
    }

    @Test
    public void testAnnotations() throws Exception {
        assertThat(typeVariable.getDeclaredAnnotations().size(), is(1));
        assertThat(typeVariable.getDeclaredAnnotations().contains(annotationDescription), is(true));
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(typeVariable.hashCode(), is(FOO.hashCode()));
    }

    @Test(expected = IllegalStateException.class)
    public void testRawTypeThrowsException() throws Exception {
        typeVariable.asRawType();
    }

    @Test(expected = IllegalStateException.class)
    public void testErasureThrowsException() throws Exception {
        typeVariable.asErasure();
    }

    @Test(expected = IllegalStateException.class)
    public void testComponentTypeThrowsException() throws Exception {
        typeVariable.getComponentType();
    }

    @Test(expected = IllegalStateException.class)
    public void testDeclaredFieldsThrowsException() throws Exception {
        typeVariable.getDeclaredFields();
    }

    @Test(expected = IllegalStateException.class)
    public void testDeclaredMethodsThrowsException() throws Exception {
        typeVariable.getDeclaredMethods();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundsThrowsException() throws Exception {
        typeVariable.getLowerBounds();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundsThrowsException() throws Exception {
        typeVariable.getUpperBounds();
    }

    @Test(expected = IllegalStateException.class)
    public void testParametersThrowsException() throws Exception {
        typeVariable.getTypeArguments();
    }

    @Test(expected = IllegalStateException.class)
    public void testVariableSourceThrowsException() throws Exception {
        typeVariable.getTypeVariableSource();
    }

    @Test(expected = IllegalStateException.class)
    public void getOwnerTypeThrowsException() throws Exception {
        typeVariable.getOwnerType();
    }

    @Test(expected = IllegalStateException.class)
    public void testSuperClassThrowsException() throws Exception {
        typeVariable.getSuperClass();
    }

    @Test(expected = IllegalStateException.class)
    public void testInterfacesThrowsException() throws Exception {
        typeVariable.getInterfaces();
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorThrowsException() throws Exception {
        typeVariable.iterator();
    }

    @Test
    public void testRepresents() throws Exception {
        assertThat(typeVariable.represents(Object.class), is(false));
    }

    @Test(expected = NullPointerException.class)
    public void testRepresentsNullPointer() throws Exception {
        typeVariable.represents(null);
    }
}
