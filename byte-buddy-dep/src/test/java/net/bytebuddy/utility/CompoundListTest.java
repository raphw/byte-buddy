package net.bytebuddy.utility;

import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class CompoundListTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

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
            throw exception.getCause();
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
}
