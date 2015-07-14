package net.bytebuddy.matcher;

import net.bytebuddy.description.field.FieldDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class FieldTokenMatcherTest extends AbstractElementMatcherTest<FieldTokenMatcher<?>> {

    @Mock
    private FieldDescription fieldDescription;

    @Mock
    private FieldDescription.Token fieldToken;

    @Mock
    private ElementMatcher<? super FieldDescription.Token> tokenMatcher;

    @SuppressWarnings("unchecked")
    public FieldTokenMatcherTest() {
        super((Class<FieldTokenMatcher<?>>) (Object) FieldTokenMatcher.class, "representedBy");
    }

    @Before
    public void setUp() throws Exception {
        when(fieldDescription.asToken()).thenReturn(fieldToken);
    }

    @Test
    public void testMatch() throws Exception {
        when(tokenMatcher.matches(fieldToken)).thenReturn(true);
        when(fieldDescription.asToken()).thenReturn(fieldToken);
        assertThat(new FieldTokenMatcher<FieldDescription>(tokenMatcher).matches(fieldDescription), is(true));
        verify(tokenMatcher).matches(fieldToken);
        verifyNoMoreInteractions(tokenMatcher);
        verify(fieldDescription).asToken();
        verifyNoMoreInteractions(fieldDescription);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(tokenMatcher.matches(fieldToken)).thenReturn(false);
        assertThat(new FieldTokenMatcher<FieldDescription>(tokenMatcher).matches(fieldDescription), is(false));
        verify(tokenMatcher).matches(fieldToken);
        verifyNoMoreInteractions(tokenMatcher);
        verify(fieldDescription).asToken();
        verifyNoMoreInteractions(fieldDescription);
    }
}
