package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class InstrumentedTypePrepareableNoOpTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private InstrumentedType instrumentedType;

    @Test
    public void testNoOp() {
        assertThat(InstrumentedType.Prepareable.NoOp.INSTANCE.prepare(instrumentedType), is(instrumentedType));
    }
}
