package net.bytebuddy.instrumentation.method;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodLookupEngineFindingDefaultTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;
    @Mock
    private MethodList methodList, otherMethodList;

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(new MethodLookupEngine.Finding.Default(typeDescription, methodList, Collections.<TypeDescription, Set<MethodDescription>>emptyMap()).hashCode(),
                is(new MethodLookupEngine.Finding.Default(typeDescription, methodList, Collections.<TypeDescription, Set<MethodDescription>>emptyMap()).hashCode()));
        assertThat(new MethodLookupEngine.Finding.Default(typeDescription, methodList, Collections.<TypeDescription, Set<MethodDescription>>emptyMap()),
                is(new MethodLookupEngine.Finding.Default(typeDescription, methodList, Collections.<TypeDescription, Set<MethodDescription>>emptyMap())));
        assertThat(new MethodLookupEngine.Finding.Default(typeDescription, methodList, Collections.<TypeDescription, Set<MethodDescription>>emptyMap()).hashCode(),
                not(is(new MethodLookupEngine.Finding.Default(typeDescription, otherMethodList, Collections.<TypeDescription, Set<MethodDescription>>emptyMap()).hashCode())));
        assertThat(new MethodLookupEngine.Finding.Default(typeDescription, methodList, Collections.<TypeDescription, Set<MethodDescription>>emptyMap()),
                not(is(new MethodLookupEngine.Finding.Default(typeDescription, otherMethodList, Collections.<TypeDescription, Set<MethodDescription>>emptyMap()))));

    }
}
