package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

public class FieldAccessorPreparationTest {

    private static final String FOO = "foo";
    private static final int NO_MODIFIERS = 0;
    private static final Class<?> TYPE = Void.class;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private InstrumentedType instrumentedType;

    @Before
    public void setUp() throws Exception {
        when(instrumentedType.withField(any(String.class), any(TypeDescription.class), anyInt()))
                .thenReturn(instrumentedType);
    }

    @Test
    public void testPreparationDefineField() throws Exception {
        assertThat(FieldAccessor.ofField(FOO).defineAs(TYPE).prepare(instrumentedType), is(instrumentedType));
        verify(instrumentedType).withField(FOO, new TypeDescription.ForLoadedType(TYPE), NO_MODIFIERS);
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testPreparationNoDefineField() throws Exception {
        assertThat(FieldAccessor.ofField(FOO).prepare(instrumentedType), is(instrumentedType));
        verifyZeroInteractions(instrumentedType);
    }

    @Test
    public void testPreparationPropertyAccessor() throws Exception {
        assertThat(FieldAccessor.ofBeanProperty().prepare(instrumentedType), is(instrumentedType));
        verifyZeroInteractions(instrumentedType);
    }
}
