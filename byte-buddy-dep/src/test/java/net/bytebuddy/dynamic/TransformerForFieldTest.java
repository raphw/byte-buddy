package net.bytebuddy.dynamic;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class TransformerForFieldTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final int MODIFIERS = 42, RANGE = 3, MASK = 1;

    @Rule
    public TestRule mocktioRule = new MockitoRule(this);

    @Mock
    private TypeDescription instrumentedType, rawDeclaringType, rawReturnType, rawParameterType;

    @Mock
    private Transformer<FieldDescription.Token> tokenTransformer;

    @Mock
    private FieldDescription fieldDescription;

    @Mock
    private FieldDescription.InDefinedShape definedField;

    @Mock
    private FieldDescription.Token fieldToken;

    @Mock
    private TypeDescription.Generic fieldType, declaringType;

    @Mock
    private AnnotationDescription fieldAnnotation;

    @Mock
    private ModifierContributor.ForField modifierContributor;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(fieldType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(fieldType);
        when(fieldDescription.asToken(ElementMatchers.is(instrumentedType))).thenReturn(fieldToken);
        when(fieldDescription.getDeclaringType()).thenReturn(declaringType);
        when(fieldDescription.asDefined()).thenReturn(definedField);
        when(fieldToken.getName()).thenReturn(FOO);
        when(fieldToken.getModifiers()).thenReturn(MODIFIERS);
        when(fieldToken.getType()).thenReturn(fieldType);
        when(fieldToken.getAnnotations()).thenReturn(new AnnotationList.Explicit(fieldAnnotation));
        when(modifierContributor.getMask()).thenReturn(MASK);
        when(modifierContributor.getRange()).thenReturn(RANGE);
    }

    @Test
    public void testSimpleTransformation() throws Exception {
        when(tokenTransformer.transform(instrumentedType, fieldToken)).thenReturn(fieldToken);
        FieldDescription transformed = new Transformer.ForField(tokenTransformer).transform(instrumentedType, fieldDescription);
        assertThat(transformed.getDeclaringType(), is((TypeDefinition) declaringType));
        assertThat(transformed.getInternalName(), is(FOO));
        assertThat(transformed.getModifiers(), is(MODIFIERS));
        assertThat(transformed.getDeclaredAnnotations(), is(Collections.singletonList(fieldAnnotation)));
        assertThat(transformed.getType(), is(fieldType));
        assertThat(transformed.asDefined(), is(definedField));
    }

    @Test
    public void testModifierTransformation() throws Exception {
        FieldDescription.Token transformed = new Transformer.ForField.FieldModifierTransformer(Collections.singletonList(modifierContributor))
                .transform(instrumentedType, fieldToken);
        assertThat(transformed.getName(), is(FOO));
        assertThat(transformed.getModifiers(), is((MODIFIERS & ~RANGE) | MASK));
        assertThat(transformed.getType(), is(fieldType));
    }

    @Test
    public void testNoChangesUnlessSpecified() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(Bar.class);
        FieldDescription fieldDescription = typeDescription.getSuperClass().getDeclaredFields().filter(named(FOO)).getOnly();
        FieldDescription transformed = Transformer.ForField.withModifiers().transform(typeDescription, fieldDescription);
        assertThat(transformed, is(fieldDescription));
        assertThat(transformed.getModifiers(), is(fieldDescription.getModifiers()));
    }

    @Test
    public void testRetainsInstrumentedType() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(Bar.class);
        FieldDescription fieldDescription = typeDescription.getSuperClass().getDeclaredFields().filter(named(BAR)).getOnly();
        FieldDescription transformed = Transformer.ForField.withModifiers().transform(typeDescription, fieldDescription);
        assertThat(transformed, is(fieldDescription));
        assertThat(transformed.getModifiers(), is(fieldDescription.getModifiers()));
        assertThat(transformed.getType().asErasure(), is(typeDescription));
        assertThat(transformed.getType().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(transformed.getType().getTypeArguments().size(), is(1));
        assertThat(transformed.getType().getTypeArguments().getOnly(), is(typeDescription.getSuperClass().getDeclaredFields().filter(named(FOO)).getOnly().getType()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Transformer.ForField.class).apply();
        ObjectPropertyAssertion.of(Transformer.ForField.FieldModifierTransformer.class).apply();
    }

    private static class Foo<T> {

        T foo;

        Bar<T> bar;
    }

    private static class Bar<S> extends Foo<S> {
        /* empty */
    }
}
