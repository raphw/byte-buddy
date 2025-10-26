package net.bytebuddy.matcher;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class FieldTypeMatcherTest extends AbstractElementMatcherTest<FieldTypeMatcher<?>> {

    @Mock
    private ElementMatcher<? super TypeDescription.Generic> typeMatcher;

    @Mock
    private TypeDescription.Generic fieldType;

    @Mock
    private FieldDescription fieldDescription;

    @SuppressWarnings("unchecked")
    public FieldTypeMatcherTest() {
        super((Class<? extends FieldTypeMatcher<?>>) (Object) FieldTypeMatcher.class, "ofType");
    }

    @Before
    public void setUp() throws Exception {
        when(fieldDescription.getType()).thenReturn(fieldType);
    }

    @Test
    public void testMatch() throws Exception {
        when(typeMatcher.matches(fieldType)).thenReturn(true);
        assertThat(new FieldTypeMatcher<FieldDescription>(typeMatcher).matches(fieldDescription), is(true));
        verify(typeMatcher).matches(fieldType);
        verifyNoMoreInteractions(typeMatcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(typeMatcher.matches(fieldType)).thenReturn(false);
        assertThat(new FieldTypeMatcher<FieldDescription>(typeMatcher).matches(fieldDescription), is(false));
        verify(typeMatcher).matches(fieldType);
        verifyNoMoreInteractions(typeMatcher);
    }
}
