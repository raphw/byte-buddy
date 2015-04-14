package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FieldRegistryCompiledNoOpTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private FieldDescription fieldDescription;

    @Test
    public void testReturnsNoOp() throws Exception {
        assertThat(FieldRegistry.Compiled.NoOp.INSTANCE.target(fieldDescription),
                is((TypeWriter.FieldPool.Entry) TypeWriter.FieldPool.Entry.NoOp.INSTANCE));
    }
}
