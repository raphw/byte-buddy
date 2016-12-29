package net.bytebuddy.description.type;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.annotation.ElementType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TypeDescriptionGenericVisitorValidatorForTypeAnnotations {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private AnnotationDescription legalAnnotation, illegalAnnotation, duplicateAnnotation;

    @Mock
    private TypeDescription legalType, illegalType;

    @Mock
    private TypeDescription.Generic legal, illegal, duplicate, otherLegal, otherIllegal;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(otherLegal.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(true);
        when(otherIllegal.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(false);
        when(illegal.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(illegalAnnotation));
        when(illegalAnnotation.getElementTypes()).thenReturn(new HashSet<ElementType>());
        when(illegalAnnotation.getAnnotationType()).thenReturn(illegalType);
        when(otherLegal.asGenericType()).thenReturn(otherLegal);
        when(otherIllegal.asGenericType()).thenReturn(otherIllegal);
        try {
            Enum<?> typeUse = Enum.valueOf(ElementType.class, "TYPE_USE");
            Enum<?> typeParameter = Enum.valueOf(ElementType.class, "TYPE_PARAMETER");
            when(legalAnnotation.getElementTypes()).thenReturn(new HashSet(Arrays.asList(typeUse, typeParameter)));
            when(duplicateAnnotation.getElementTypes()).thenReturn(new HashSet(Arrays.asList(typeUse, typeParameter)));
        } catch (IllegalArgumentException ignored) {
            when(legalAnnotation.getElementTypes()).thenReturn(Collections.<ElementType>emptySet());
            when(duplicateAnnotation.getElementTypes()).thenReturn(Collections.<ElementType>emptySet());
        }
        when(legal.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(legalAnnotation));
        when(duplicate.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(legalAnnotation, duplicateAnnotation));
        when(legalAnnotation.getAnnotationType()).thenReturn(legalType);
        when(duplicateAnnotation.getAnnotationType()).thenReturn(legalType);
    }

    @Test
    public void testIllegalGenericArray() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onGenericArray(illegal), is(false));
    }

    @Test
    public void testDuplicateGenericArray() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onGenericArray(duplicate), is(false));
    }

    @Test
    public void testIllegalDelegatedGenericArray() throws Exception {
        when(legal.getComponentType()).thenReturn(otherIllegal);
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onGenericArray(legal), is(false));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testLegalGenericArray() throws Exception {
        when(legal.getComponentType()).thenReturn(otherLegal);
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onGenericArray(legal), is(true));
        verify(otherLegal).accept(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE);
    }

    @Test
    public void testIllegalNonGenericArray() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onNonGenericType(illegal), is(false));
    }

    @Test
    public void testDuplicateNonGenericArray() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onNonGenericType(duplicate), is(false));
    }

    @Test
    public void testIllegalDelegatedNonGenericArray() throws Exception {
        when(legal.isArray()).thenReturn(true);
        when(legal.getComponentType()).thenReturn(otherIllegal);
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onNonGenericType(legal), is(false));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testLegalNonGenericArray() throws Exception {
        when(legal.isArray()).thenReturn(true);
        when(legal.getComponentType()).thenReturn(otherLegal);
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onNonGenericType(legal), is(true));
        verify(otherLegal).accept(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE);
    }

    @Test
    public void testIllegalNonGeneric() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onNonGenericType(illegal), is(false));
    }

    @Test
    public void testDuplicateNonGeneric() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onNonGenericType(duplicate), is(false));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testLegalNonGeneric() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onNonGenericType(legal), is(true));
    }

    @Test
    public void testIllegalTypeVariable() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onTypeVariable(illegal), is(false));
    }

    @Test
    public void testDuplicateTypeVariable() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onTypeVariable(duplicate), is(false));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testLegalTypeVariable() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onTypeVariable(legal), is(true));
    }

    @Test
    public void testIllegalParameterized() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onParameterizedType(illegal), is(false));
    }

    @Test
    public void testDuplicateParameterized() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onParameterizedType(duplicate), is(false));
    }

    @Test
    public void testIllegalDelegatedOwnerTypeParameterized() throws Exception {
        when(legal.getOwnerType()).thenReturn(otherIllegal);
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onParameterizedType(legal), is(false));
    }

    @Test
    public void testIllegalDelegatedTypeArgumentParameterized() throws Exception {
        when(legal.getTypeArguments()).thenReturn(new TypeList.Generic.Explicit(otherIllegal));
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onParameterizedType(legal), is(false));
    }

    @Test
    public void testIllegalDuplicateParameterized() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onParameterizedType(duplicate), is(false));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testLegalParameterized() throws Exception {
        when(legal.isArray()).thenReturn(true);
        when(legal.getTypeArguments()).thenReturn(new TypeList.Generic.Explicit(otherLegal));
        when(legal.getOwnerType()).thenReturn(otherLegal);
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onParameterizedType(legal), is(true));
        verify(otherLegal, times(2)).accept(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE);
    }

    @Test
    public void testWildcardIllegal() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onWildcard(illegal), is(false));
    }

    @Test
    public void testWildcardDuplicate() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onWildcard(duplicate), is(false));
    }

    @Test
    public void testWildcardIllegalUpperBounds() throws Exception {
        when(legal.getUpperBounds()).thenReturn(new TypeList.Generic.Explicit(otherIllegal));
        when(legal.getLowerBounds()).thenReturn(new TypeList.Generic.Empty());
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onWildcard(legal), is(false));
    }

    @Test
    public void testWildcardIllegalLowerBounds() throws Exception {
        when(legal.getUpperBounds()).thenReturn(new TypeList.Generic.Explicit(TypeDescription.Generic.OBJECT));
        when(legal.getLowerBounds()).thenReturn(new TypeList.Generic.Explicit(otherIllegal));
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onWildcard(legal), is(false));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testWildcardLegal() throws Exception {
        when(legal.getUpperBounds()).thenReturn(new TypeList.Generic.Explicit(TypeDescription.Generic.OBJECT));
        when(legal.getLowerBounds()).thenReturn(new TypeList.Generic.Explicit(otherLegal));
        assertThat(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE.onWildcard(legal), is(true));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Validator.ForTypeAnnotations.class).apply();
    }
}
