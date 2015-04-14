package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

public class MethodLookupEngineFindingDefaultTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private MethodList methodList, otherMethodList;

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodLookupEngine.Finding.Default.class).apply();
    }
}
