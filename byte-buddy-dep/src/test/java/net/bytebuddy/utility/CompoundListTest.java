package net.bytebuddy.utility;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CompoundListTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private Object first, second, third, forth;

    @Test(expected = UnsupportedOperationException.class)
    public void testConstruction() throws Throwable {
        Constructor<?> constructor = CompoundList.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException exception) {
            throw exception.getTargetException();
        }
    }

    @Test
    public void testElementAndList() throws Exception {
        List<Object> list = CompoundList.of(first, Arrays.asList(second, third, forth));
        assertThat(list.size(), is(4));
        assertThat(list.get(0), is(first));
        assertThat(list.get(1), is(second));
        assertThat(list.get(2), is(third));
        assertThat(list.get(3), is(forth));
    }

    @Test
    public void testListAndElement() throws Exception {
        List<Object> list = CompoundList.of(Arrays.asList(first, second, third), forth);
        assertThat(list.size(), is(4));
        assertThat(list.get(0), is(first));
        assertThat(list.get(1), is(second));
        assertThat(list.get(2), is(third));
        assertThat(list.get(3), is(forth));
    }

    @Test
    public void testListAndList() throws Exception {
        List<Object> list = CompoundList.of(Arrays.asList(first, second), Arrays.asList(third, forth));
        assertThat(list.size(), is(4));
        assertThat(list.get(0), is(first));
        assertThat(list.get(1), is(second));
        assertThat(list.get(2), is(third));
        assertThat(list.get(3), is(forth));
    }

    @Test
    public void testListAndListAndList() throws Exception {
        List<Object> list = CompoundList.of(Collections.singletonList(first), Collections.singletonList(second), Collections.singletonList(third));
        assertThat(list.size(), is(3));
        assertThat(list.get(0), is(first));
        assertThat(list.get(1), is(second));
        assertThat(list.get(2), is(third));
    }
}
