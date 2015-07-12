package net.bytebuddy.implementation;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
        when(instrumentedType.withField(any(FieldDescription.Token.class))).thenReturn(instrumentedType);
    }

    @Test
    public void testPreparationDefineField() throws Exception {
        assertThat(FieldAccessor.ofField(FOO).defineAs(TYPE).prepare(instrumentedType), is(instrumentedType));
        verify(instrumentedType).withField(new FieldDescription.Token(FOO, NO_MODIFIERS, new TypeDescription.ForLoadedType(TYPE)));
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
