package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class PluginCompoundTest {

    @Rule
    public TestRule mockutoRule = new MockitoRule(this);

    @Mock
    private Plugin first, second;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private ClassFileLocator classFileLocator;

    @Mock
    private DynamicType.Builder<?> origin, firstBuilder, secondBuilder;

    @Test
    @SuppressWarnings("unchecked")
    public void testNone() {
        Plugin compound = new Plugin.Compound(first, second);
        assertThat(compound.matches(typeDescription), is(false));
        verify(first).matches(typeDescription);
        verify(second).matches(typeDescription);
        assertThat(compound.apply(origin, typeDescription, classFileLocator), is((DynamicType.Builder) origin));
        verify(first, never()).apply(origin, typeDescription, classFileLocator);
        verify(second, never()).apply(origin, typeDescription, classFileLocator);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFirstOnly() {
        when(first.matches(typeDescription)).thenReturn(true);
        when(first.apply(origin, typeDescription, classFileLocator)).thenReturn((DynamicType.Builder) firstBuilder);
        Plugin compound = new Plugin.Compound(first, second);
        assertThat(compound.matches(typeDescription), is(true));
        verify(first).matches(typeDescription);
        verify(second, never()).matches(typeDescription);
        assertThat(compound.apply(origin, typeDescription, classFileLocator), is((DynamicType.Builder) firstBuilder));
        verify(first).apply(origin, typeDescription, classFileLocator);
        verify(second, never()).apply(origin, typeDescription, classFileLocator);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSecondOnly() {
        when(second.matches(typeDescription)).thenReturn(true);
        when(second.apply(origin, typeDescription, classFileLocator)).thenReturn((DynamicType.Builder) firstBuilder);
        Plugin compound = new Plugin.Compound(first, second);
        assertThat(compound.matches(typeDescription), is(true));
        verify(first).matches(typeDescription);
        verify(second).matches(typeDescription);
        assertThat(compound.apply(origin, typeDescription, classFileLocator), is((DynamicType.Builder) firstBuilder));
        verify(first, never()).apply(origin, typeDescription, classFileLocator);
        verify(second).apply(origin, typeDescription, classFileLocator);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFirstAndSecond() {
        when(first.matches(typeDescription)).thenReturn(true);
        when(second.matches(typeDescription)).thenReturn(true);
        when(first.apply(origin, typeDescription, classFileLocator)).thenReturn((DynamicType.Builder) firstBuilder);
        when(second.apply(firstBuilder, typeDescription, classFileLocator)).thenReturn((DynamicType.Builder) secondBuilder);
        Plugin compound = new Plugin.Compound(first, second);
        assertThat(compound.matches(typeDescription), is(true));
        verify(first).matches(typeDescription);
        verify(second, never()).matches(typeDescription);
        assertThat(compound.apply(origin, typeDescription, classFileLocator), is((DynamicType.Builder) secondBuilder));
        verify(first).apply(origin, typeDescription, classFileLocator);
        verify(second).apply(firstBuilder, typeDescription, classFileLocator);
    }
}
