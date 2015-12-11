package net.bytebuddy.agent;

import net.bytebuddy.test.utility.MockitoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class InstallerTest {

    private static final String FOO = "foo";

    private static final String INSTRUMENTATION = "instrumentation";

    private static final Object STATIC_FIELD = null;

    @Rule
    public final TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Instrumentation instrumentation;

    private Instrumentation actualInstrumentation;

    @Before
    public void setUp() throws Exception {
        Field field = Installer.class.getDeclaredField(INSTRUMENTATION);
        field.setAccessible(true);
        actualInstrumentation = (Instrumentation) field.get(STATIC_FIELD);
    }

    @After
    public void tearDown() throws Exception {
        Field field = Installer.class.getDeclaredField(INSTRUMENTATION);
        field.setAccessible(true);
        field.set(STATIC_FIELD, actualInstrumentation);
    }

    @Test
    public void testPreMain() throws Exception {
        Installer.premain(FOO, instrumentation);
        assertThat(ByteBuddyAgent.getInstrumentation(), is(instrumentation));
    }

    @Test
    public void testAgentMain() throws Exception {
        Installer.agentmain(FOO, instrumentation);
        assertThat(ByteBuddyAgent.getInstrumentation(), is(instrumentation));
    }

    @Test
    public void testAgentInstallerIsPublic() throws Exception {
        Class<?> type = Installer.class;
        assertThat(Modifier.isPublic(type.getModifiers()), is(true));
        assertThat(type.getDeclaringClass(), nullValue(Class.class));
        assertThat(type.getDeclaredClasses().length, is(0));
    }

    @Test
    public void testAgentInstallerStoreIsPublic() throws Exception {
        Field field = Installer.class.getDeclaredField("instrumentation");
        assertThat(Modifier.isPublic(field.getModifiers()), is(true));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConstructorThrowsException() throws Exception {
        Constructor<?> constructor = Installer.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException exception) {
            throw (Exception) exception.getCause();
        }
    }
}
