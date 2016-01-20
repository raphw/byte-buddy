package net.bytebuddy.description.type;

import net.bytebuddy.description.TypeVariableSource;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypeDescriptionGenericVisitorTypeVariableErasingTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription.Generic typeDescription, referencedType, transformedType;

    @Mock
    private AnnotationDescription annotationDescription;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(typeDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(annotationDescription));
        when(referencedType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(transformedType);
        when(referencedType.asGenericType()).thenReturn(referencedType);
        when(transformedType.asGenericType()).thenReturn(transformedType);
    }

    @Test
    public void testGenericArray() throws Exception {
        when(transformedType.getSort()).thenReturn(TypeDefinition.Sort.GENERIC_ARRAY);
        when(typeDescription.getComponentType()).thenReturn(referencedType);
        assertThat(TypeDescription.Generic.Visitor.TypeVariableErasing.INSTANCE.onGenericArray(typeDescription), is((TypeDescription.Generic)
                new TypeDescription.Generic.OfGenericArray.Latent(transformedType, Collections.singletonList(annotationDescription))));
    }

    @Test
    public void testWildcard() throws Exception {
        when(typeDescription.getUpperBounds()).thenReturn(new TypeList.Generic.Explicit(referencedType));
        when(typeDescription.getLowerBounds()).thenReturn(new TypeList.Generic.Explicit(referencedType));
        assertThat(TypeDescription.Generic.Visitor.TypeVariableErasing.INSTANCE.onWildcard(typeDescription), is((TypeDescription.Generic)
                new TypeDescription.Generic.OfWildcardType.Latent(Collections.singletonList(transformedType),
                        Collections.singletonList(transformedType),
                        Collections.singletonList(annotationDescription))));
    }

    @Test
    public void testParameterizedNoErasure() throws Exception {
        TypeDescription erasure = mock(TypeDescription.class);
        when(typeDescription.asErasure()).thenReturn(erasure);
        when(referencedType.accept(TypeDescription.Generic.Visitor.TypeVariableErasing.PartialErasureReviser.INSTANCE)).thenReturn(false);
        when(typeDescription.getTypeArguments()).thenReturn(new TypeList.Generic.Explicit(referencedType));
        when(typeDescription.getOwnerType()).thenReturn(referencedType);
        assertThat(TypeDescription.Generic.Visitor.TypeVariableErasing.INSTANCE.onParameterizedType(typeDescription), is((TypeDescription.Generic)
                new TypeDescription.Generic.OfParameterizedType.Latent(erasure,
                        transformedType,
                        Collections.singletonList(transformedType),
                        Collections.singletonList(annotationDescription))));
    }

    @Test
    public void testParameterizedErasure() throws Exception {
        TypeDescription.Generic rawType = mock(TypeDescription.Generic.class);
        when(typeDescription.asRawType()).thenReturn(rawType);
        when(referencedType.accept(TypeDescription.Generic.Visitor.TypeVariableErasing.PartialErasureReviser.INSTANCE)).thenReturn(true);
        when(typeDescription.getTypeArguments()).thenReturn(new TypeList.Generic.Explicit(referencedType));
        when(typeDescription.getOwnerType()).thenReturn(referencedType);
        assertThat(TypeDescription.Generic.Visitor.TypeVariableErasing.INSTANCE.onParameterizedType(typeDescription), is(rawType));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTypeVariableOnMethod() throws Exception {
        MethodDescription.InDefinedShape typeVariableSource = mock(MethodDescription.InDefinedShape.AbstractBase.class);
        when(typeVariableSource.accept(any(TypeVariableSource.Visitor.class))).thenCallRealMethod();
        when(typeVariableSource.asDefined()).thenCallRealMethod();
        when(typeDescription.getVariableSource()).thenReturn(typeVariableSource);
        when(typeDescription.getSymbol()).thenReturn(FOO);
        TypeDescription.Generic typeVariable = TypeDescription.Generic.Visitor.TypeVariableErasing.INSTANCE.onTypeVariable(typeDescription);
        assertThat(typeVariable.getSymbol(), is(FOO));
        assertThat(typeVariable.getDeclaredAnnotations(), is(Collections.singletonList(annotationDescription)));
        assertThat(typeVariable.getVariableSource(), is((TypeVariableSource) typeVariableSource));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTypeVariableOnType() throws Exception {
        TypeDescription typeVariableSource = mock(TypeDescription.AbstractBase.class);
        when(typeVariableSource.accept(any(TypeVariableSource.Visitor.class))).thenCallRealMethod();
        TypeDescription typeErasure = mock(TypeDescription.class);
        when(typeDescription.asErasure()).thenReturn(typeErasure);
        when(typeDescription.getVariableSource()).thenReturn(typeVariableSource);
        TypeDescription.Generic erasure = TypeDescription.Generic.Visitor.TypeVariableErasing.INSTANCE.onTypeVariable(typeDescription);
        assertThat(erasure.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(erasure.asErasure(), is(typeErasure));
        assertThat(erasure.getDeclaredAnnotations(), is(Collections.singletonList(annotationDescription)));
    }

    @Test
    public void testNonGeneric() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.TypeVariableErasing.INSTANCE.onNonGenericType(typeDescription), is(typeDescription));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.TypeVariableErasing.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.TypeVariableErasing.PartialErasureReviser.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.TypeVariableErasing.TypeVariableReviser.class).apply();
    }
}