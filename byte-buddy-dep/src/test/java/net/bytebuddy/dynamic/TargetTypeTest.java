package net.bytebuddy.dynamic;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

public class TargetTypeTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription, targetType, componentType;

    @Test
    public void testIsNotTargetType() throws Exception {
        when(typeDescription.represents(TargetType.class)).thenReturn(false);
        assertThat(TargetType.resolve(typeDescription, targetType), is(typeDescription));
    }

    @Test
    public void testIsTargetType() throws Exception {
        when(typeDescription.represents(TargetType.class)).thenReturn(true);
        assertThat(TargetType.resolve(typeDescription, targetType), is(targetType));
    }

    @Test
    public void testIsNotTargetTypeArray() throws Exception {
        when(typeDescription.isArray()).thenReturn(true);
        when(typeDescription.getComponentType()).thenReturn(componentType);
        when(componentType.represents(TargetType.class)).thenReturn(false);
        assertThat(TargetType.resolve(typeDescription, targetType), is(typeDescription));
    }

    @Test
    public void testIsTargetTypeArray() throws Exception {
        when(typeDescription.isArray()).thenReturn(true);
        when(typeDescription.getComponentType()).thenReturn(componentType);
        when(componentType.represents(TargetType.class)).thenReturn(true);
        TypeDescription resolvedType = TargetType.resolve(typeDescription, targetType);
        assertThat(resolvedType.isArray(), is(true));
        assertThat(resolvedType.getComponentType(), is(targetType));
    }

    @Test
    public void testConstructorIsHidden() throws Exception {
        assertThat(TargetType.class.getDeclaredConstructors().length, is(1));
        Constructor<?> constructor = TargetType.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers()), is(true));
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause().getClass(), CoreMatchers.<Class<?>>is(UnsupportedOperationException.class));
        }
    }

    @Test
    public void testTypeIsFinal() throws Exception {
        assertThat(Modifier.isFinal(TargetType.class.getModifiers()), is(true));
    }
}
