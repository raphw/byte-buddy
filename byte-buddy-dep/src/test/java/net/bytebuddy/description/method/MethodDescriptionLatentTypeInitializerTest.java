package net.bytebuddy.description.method;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDescriptionLatentTypeInitializerTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;

    private MethodDescription.InDefinedShape typeInitializer;

    @Before
    public void setUp() throws Exception {
        typeInitializer = new MethodDescription.Latent.TypeInitializer(typeDescription);
    }

    @Test
    public void testDeclaringType() throws Exception {
        assertThat(typeInitializer.getDeclaringType(), is(typeDescription));
    }

    @Test
    public void testName() throws Exception {
        assertThat(typeInitializer.getInternalName(), is(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME));
    }

    @Test
    public void testModifiers() throws Exception {
        assertThat(typeInitializer.getModifiers(), is(MethodDescription.TYPE_INITIALIZER_MODIFIER));
    }

    @Test
    public void testAnnotations() throws Exception {
        assertThat(typeInitializer.getDeclaredAnnotations().size(), is(0));
    }

    @Test
    public void testExceptions() throws Exception {
        assertThat(typeInitializer.getExceptionTypes().size(), is(0));
    }

    @Test
    public void testParameters() throws Exception {
        assertThat(typeInitializer.getParameters().size(), is(0));
    }

    @Test
    public void testReturnType() throws Exception {
        assertThat(typeInitializer.getReturnType(), is(TypeDescription.Generic.VOID));
    }
}
