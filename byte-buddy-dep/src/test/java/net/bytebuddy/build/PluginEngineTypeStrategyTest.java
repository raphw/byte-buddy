package net.bytebuddy.build;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class PluginEngineTypeStrategyTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private ByteBuddy byteBuddy;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private ClassFileLocator classFileLocator;

    @Mock
    private EntryPoint entryPoint;

    @Mock
    private MethodNameTransformer methodNameTransformer;

    @Test
    public void testRebase() {
        assertThat(Plugin.Engine.TypeStrategy.Default.REBASE.builder(byteBuddy, typeDescription, classFileLocator), nullValue());
        verify(byteBuddy).rebase(typeDescription, classFileLocator);
    }

    @Test
    public void testRedefine() {
        assertThat(Plugin.Engine.TypeStrategy.Default.REDEFINE.builder(byteBuddy, typeDescription, classFileLocator), nullValue());
        verify(byteBuddy).redefine(typeDescription, classFileLocator);
    }

    @Test
    public void testDecorate() {
        assertThat(Plugin.Engine.TypeStrategy.Default.DECORATE.builder(byteBuddy, typeDescription, classFileLocator), nullValue());
        verify(byteBuddy).decorate(typeDescription, classFileLocator);
    }

    @Test
    public void testForEntryPorint() {
        assertThat(new Plugin.Engine.TypeStrategy.ForEntryPoint(entryPoint, methodNameTransformer)
                .builder(byteBuddy, typeDescription, classFileLocator), nullValue());
        verify(entryPoint).transform(typeDescription, byteBuddy, classFileLocator, methodNameTransformer);
        verifyNoMoreInteractions(entryPoint);
    }
}
