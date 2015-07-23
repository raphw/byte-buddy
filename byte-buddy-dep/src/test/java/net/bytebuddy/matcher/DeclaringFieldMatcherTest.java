package net.bytebuddy.matcher;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class DeclaringFieldMatcherTest extends AbstractElementMatcherTest<DeclaringFieldMatcher<?>> {

    @Mock
    private ElementMatcher<? super FieldList<?>> fieldMatcher;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private FieldList<FieldDescription.InDefinedShape> fieldList;

    @SuppressWarnings("unchecked")
    public DeclaringFieldMatcherTest() {
        super((Class<DeclaringFieldMatcher<?>>) (Object) DeclaringFieldMatcher.class, "declaresFields");
    }

    @Test
    public void testMatch() throws Exception {
        when(typeDescription.getDeclaredFields()).thenReturn(fieldList);
        when(fieldMatcher.matches(fieldList)).thenReturn(true);
        assertThat(new DeclaringFieldMatcher<TypeDescription>(fieldMatcher).matches(typeDescription), is(true));
        verify(fieldMatcher).matches(fieldList);
        verifyNoMoreInteractions(fieldMatcher);
        verify(typeDescription).getDeclaredFields();
        verifyNoMoreInteractions(typeDescription);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(typeDescription.getDeclaredFields()).thenReturn(fieldList);
        when(fieldMatcher.matches(fieldList)).thenReturn(false);
        assertThat(new DeclaringFieldMatcher<TypeDescription>(fieldMatcher).matches(typeDescription), is(false));
        verify(fieldMatcher).matches(fieldList);
        verifyNoMoreInteractions(fieldMatcher);
        verify(typeDescription).getDeclaredFields();
        verifyNoMoreInteractions(typeDescription);
    }
}
