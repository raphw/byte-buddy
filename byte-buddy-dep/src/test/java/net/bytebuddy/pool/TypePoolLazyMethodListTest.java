package net.bytebuddy.pool;

import net.bytebuddy.description.method.AbstractMethodListTest;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.anyOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypePoolLazyMethodListTest extends AbstractMethodListTest<Method> {

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    @Override
    protected Method getFirst() throws Exception {
        return Foo.class.getDeclaredMethod("foo");
    }

    @Override
    protected Method getSecond() throws Exception {
        return Foo.class.getDeclaredMethod("bar");
    }

    @Override
    protected MethodList asList(List<Method> elements) {
        return typePool.describe(Foo.class.getName()).resolve().getDeclaredMethods().filter(anyOf(elements.toArray(new Method[elements.size()])));
    }

    @Override
    protected MethodDescription asElement(Method element) {
        return new MethodDescription.ForLoadedMethod(element);
    }
}
