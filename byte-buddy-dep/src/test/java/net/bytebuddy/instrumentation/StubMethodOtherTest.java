package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.utility.MockitoRule;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;

public class StubMethodOtherTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private InstrumentedType instrumentedType;

    @Test
    public void testPreparation() throws Exception {
        assertThat(StubMethod.INSTANCE.prepare(instrumentedType), is(instrumentedType));
        verifyZeroInteractions(instrumentedType);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(SuperMethodCall.Appender.class).skipSynthetic().apply();
    }
}
