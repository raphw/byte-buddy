package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodAttributeAppenderForMethodTest extends AbstractMethodAttributeAppenderTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final int PARAMETER_INDEX = 0;

    private Method method;

    private Constructor<?> constructor;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        method = Foo.class.getDeclaredMethod(BAR, Object.class);
        ParameterList<?> parameters = mock(ParameterList.class);
        when(methodDescription.getParameters()).thenReturn((ParameterList) parameters);
        when(parameters.size()).thenReturn(PARAMETER_INDEX + 1);
        constructor = Bar.class.getDeclaredConstructor(Object.class);
        when(methodDescription.getParameters()).thenReturn((ParameterList) parameters);
        when(parameters.size()).thenReturn(PARAMETER_INDEX + 1);
    }

    @Test
    public void testMethodMakeReturnsSameInstance() throws Exception {
        assertThat(new MethodAttributeAppender.ForMethod(method, valueFilter).make(mock(TypeDescription.class)),
                is((MethodAttributeAppender) new MethodAttributeAppender.ForMethod(method, valueFilter)));
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void testMethodIllegalApplicationThrowsException() throws Exception {
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty());
        new MethodAttributeAppender.ForMethod(method, valueFilter).apply(methodVisitor, methodDescription);
    }

    @Test
    public void testLoadedMethodAttributeAppender() throws Exception {
        MethodAttributeAppender methodAttributeAppender = new MethodAttributeAppender.ForMethod(method, valueFilter);
        methodAttributeAppender.apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verify(methodVisitor).visitParameterAnnotation(PARAMETER_INDEX, Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getParameters();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testConstructorMakeReturnsSameInstance() throws Exception {
        assertThat(new MethodAttributeAppender.ForMethod(constructor, valueFilter).make(mock(TypeDescription.class)),
                is((MethodAttributeAppender) new MethodAttributeAppender.ForMethod(constructor, valueFilter)));
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void testIllegalConstructorApplicationThrowsException() throws Exception {
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty());
        new MethodAttributeAppender.ForMethod(constructor, valueFilter).apply(methodVisitor, methodDescription);
    }

    @Test
    public void testLoadedFieldAttributeAppender() throws Exception {
        MethodAttributeAppender methodAttributeAppender = new MethodAttributeAppender.ForMethod(constructor, valueFilter);
        methodAttributeAppender.apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verify(methodVisitor).visitParameterAnnotation(PARAMETER_INDEX, Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getParameters();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testObjectProperties() throws Exception {
        final Iterator<Method> methods = Arrays.asList(MethodSample.class.getDeclaredMethod(FOO),
                MethodSample.class.getDeclaredMethod(FOO, Void.class),
                MethodSample.class.getDeclaredMethod(FOO),
                MethodSample.class.getDeclaredMethod(FOO, Void.class)).iterator();
        Constructor<?> first = ConstructorSample.class.getDeclaredConstructor(), second = ConstructorSample.class.getDeclaredConstructor(Void.class);
        final Iterator<Constructor<?>> constructors = Arrays.asList(first, second, first, second).iterator();
        ObjectPropertyAssertion.of(MethodAttributeAppender.ForMethod.class)
                .create(new ObjectPropertyAssertion.Creator<Method>() {
                    @Override
                    public Method create() {
                        return methods.next();
                    }
                })
                .create(new ObjectPropertyAssertion.Creator<Constructor<?>>() {
                    @Override
                    public Constructor<?> create() {
                        return constructors.next();
                    }
                }).apply();
    }

    private static abstract class Foo {

        @Qux
        @Baz
        @QuxBaz
        protected abstract void bar(@Qux @Baz @QuxBaz Object o);
    }

    private static class MethodSample {

        private void foo() {
        }

        private void foo(Void v) {
        }
    }

    private static abstract class Bar {

        @Qux
        @Baz
        @QuxBaz
        protected Bar(@Qux @Baz @QuxBaz Object o) {
            /* empty */
        }
    }

    private static class ConstructorSample {

        private ConstructorSample() {
        }

        private ConstructorSample(Void v) {
        }
    }
}
