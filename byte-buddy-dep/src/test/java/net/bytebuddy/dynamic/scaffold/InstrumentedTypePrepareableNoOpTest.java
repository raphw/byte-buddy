package net.bytebuddy.dynamic.scaffold;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class InstrumentedTypePrepareableNoOpTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private InstrumentedType instrumentedType;

    @Test
    public void testNoOp() {
        assertThat(InstrumentedType.Prepareable.NoOp.INSTANCE.prepare(instrumentedType), is(instrumentedType));
    }
}
