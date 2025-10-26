package net.bytebuddy.agent;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class InstallerTest {

    private static final String FOO = "foo";

    @Rule
    public final MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private Instrumentation instrumentation;

    private Instrumentation actualInstrumentation;

    @Before
    public void setUp() throws Exception {
        Field field = Installer.class.getDeclaredField("instrumentation");
        field.setAccessible(true);
        actualInstrumentation = (Instrumentation) field.get(null);
        field.set(null, null);
    }

    @After
    public void tearDown() throws Exception {
        Field field = Installer.class.getDeclaredField("instrumentation");
        field.setAccessible(true);
        field.set(null, actualInstrumentation);
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
    public void testAgentInstallerStoreIsPrivate() throws Exception {
        Field field = Installer.class.getDeclaredField("instrumentation");
        assertThat(Modifier.isPrivate(field.getModifiers()), is(true));
    }

    @Test
    public void testAgentInstallerGetterIsPublic() throws Exception {
        Method method = Installer.class.getDeclaredMethod("getInstrumentation");
        assertThat(Modifier.isPublic(method.getModifiers()), is(true));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConstructorThrowsException() throws Exception {
        Constructor<?> constructor = Installer.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException exception) {
            throw (Exception) exception.getTargetException();
        }
    }

    @Test
    public void testInstallerObfuscatedNameMatches() throws Exception {
        assertThat(Installer.NAME, is(Installer.class.getName()));
    }
}
