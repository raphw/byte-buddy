package net.bytebuddy.implementation;

import net.bytebuddy.description.method.MethodDescription;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FieldAccessorFieldNameExtractorForArgumentSubstitutionTest {

    private static final String FOO = "foo", FOO_CAPITAL = "Foo";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodDescription methodDescription;

    @Test
    public void testGetterMethod() throws Exception {
        assertThat(new FieldAccessor.FieldNameExtractor.ForFixedValue(FOO).resolve(methodDescription), is(FOO));
    }
}
