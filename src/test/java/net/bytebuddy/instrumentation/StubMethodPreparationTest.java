package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;

public class StubMethodPreparationTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private InstrumentedType instrumentedType;

    @Test
    public void testPreparation() throws Exception {
        assertThat(StubMethod.INSTANCE.prepare(instrumentedType), is(instrumentedType));
        verifyZeroInteractions(instrumentedType);
    }
}
