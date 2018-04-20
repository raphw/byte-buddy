package net.bytebuddy.agent.builder;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaModule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class RawMatcherForResolvableTypesTest {

    @Test
    public void testUnloadedMatches() throws Exception {
        assertThat(AgentBuilder.RawMatcher.ForResolvableTypes.INSTANCE.matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                null,
                Foo.class.getProtectionDomain()), is(true));
    }

    @Test
    public void testResolvableMatches() throws Exception {
        assertThat(AgentBuilder.RawMatcher.ForResolvableTypes.INSTANCE.matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain()), is(true));
    }

    @Test
    public void testUnresolvableDoesNotMatch() throws Exception {
        assertThat(AgentBuilder.RawMatcher.ForResolvableTypes.INSTANCE.matches(TypeDescription.ForLoadedType.of(Bar.class),
                Bar.class.getClassLoader(),
                JavaModule.ofType(Bar.class),
                Bar.class,
                Bar.class.getProtectionDomain()), is(false));
    }

    private static class Foo {
        /* empty */
    }

    private static class Bar {

        static {
            if (true) {
                throw new AssertionError();
            }
        }
    }
}
