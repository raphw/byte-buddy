package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.ToolsJarRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.InputStream;
import java.lang.instrument.Instrumentation;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassFileLocatorAgentBasedTest {

    @Rule
    public MethodRule toolsJarRule = new ToolsJarRule();

    @Before
    public void setUp() throws Exception {
        assertThat(ByteBuddyAgent.installOnOpenJDK(), instanceOf(Instrumentation.class));
    }

    @Test
    @ToolsJarRule.Enforce
    public void testStrategyCreation() throws Exception {
        assertThat(ClassReloadingStrategy.fromInstalledAgent(), notNullValue());
    }

    @Test
    @ToolsJarRule.Enforce
    public void testExtraction() throws Exception {
        ClassFileLocator classFileLocator = ClassFileLocator.AgentBased.fromInstalledAgent(getClass().getClassLoader());
        InputStream inputStream = classFileLocator.classFileFor(new TypeDescription.ForLoadedType(Foo.class));
        try {
            assertThat(inputStream, notNullValue());
        } finally {
            inputStream.close();
        }
    }

    private static class Foo {

    }
}
