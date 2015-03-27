package net.bytebuddy.utility;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class JavaMethodTest {

    @Test
    public void testUnavailableMethodIsNotInvokable() throws Exception {
        assertThat(JavaMethod.ForUnavailableMethod.INSTANCE.isInvokable(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testUnavailableMethodThrowsException() throws Exception {
        JavaMethod.ForUnavailableMethod.INSTANCE.invoke(mock(Object.class));
    }

    @Test
    public void testLoadedMethod() throws Exception {
        Method method = Object.class.getDeclaredMethod("toString");
        JavaMethod javaMethod = new JavaMethod.ForLoadedMethod(method);
        Object instance = new Object();
        assertThat(javaMethod.isInvokable(), is(true));
        assertThat(javaMethod.invoke(instance), is((Object) instance.toString()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        final Iterator<Method> iterator = Arrays.asList(Object.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(JavaMethod.ForLoadedMethod.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return iterator.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(JavaMethod.ForUnavailableMethod.class).apply();
    }
}
