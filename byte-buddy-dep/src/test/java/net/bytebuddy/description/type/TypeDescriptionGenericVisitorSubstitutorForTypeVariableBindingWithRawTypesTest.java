package net.bytebuddy.description.type;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TypeDescriptionGenericVisitorSubstitutorForTypeVariableBindingWithRawTypesTest {

    @Mock
    private TypeDescription.Generic visitedType, parameterizedType, rawType;

    @Test
    @SuppressWarnings("unchecked")
    public void testParameterizedTypeWithRawTypes() throws Exception {
        System.setProperty(TypeDefinition.RAW_TYPES_PROPERTY, "true");
        MockitoAnnotations.initMocks(this);

        when(parameterizedType.asRawType()).thenReturn(rawType);
        when(rawType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(rawType);
        when(parameterizedType.getTypeArguments()).thenReturn(new TypeList.Generic.Explicit(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)));
        TypeDescription.Generic result = new TypeDescription.Generic.Visitor.Substitutor.ForTypeVariableBinding(visitedType).onParameterizedType(parameterizedType);

        assertThat(result.getTypeArguments(), CoreMatchers.<TypeList.Generic>is(new TypeList.Generic.Explicit()));
    }
}
