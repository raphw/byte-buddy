package net.bytebuddy.matcher;

import net.bytebuddy.description.method.MethodDescription;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class MethodSortMatcherTest extends AbstractElementMatcherTest<MethodSortMatcher<?>> {

    private static final String FOO = "foo", BAR = "bar";

    private final MethodSortMatcher.Sort sort;

    private final MockImplementation mockImplementation;

    @Mock
    private MethodDescription methodDescription;

    @SuppressWarnings("unchecked")
    public MethodSortMatcherTest(MethodSortMatcher.Sort sort, MockImplementation mockImplementation) {
        super((Class<MethodSortMatcher<?>>) (Object) MethodSortMatcher.class, sort.getDescription());
        this.sort = sort;
        this.mockImplementation = mockImplementation;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {MethodSortMatcher.Sort.CONSTRUCTOR, MockImplementation.CONSTRUCTOR},
                {MethodSortMatcher.Sort.DEFAULT_METHOD, MockImplementation.DEFAULT_METHOD},
                {MethodSortMatcher.Sort.METHOD, MockImplementation.METHOD},
                {MethodSortMatcher.Sort.VIRTUAL, MockImplementation.VIRTUAL},
                {MethodSortMatcher.Sort.TYPE_INITIALIZER, MockImplementation.TYPE_INITIALIZER},
        });
    }

    @Test
    public void testMatch() throws Exception {
        mockImplementation.prepare(methodDescription);
        assertThat(new MethodSortMatcher<MethodDescription>(sort).matches(methodDescription), is(true));
    }

    @Test
    public void testNoMatch() throws Exception {
        assertThat(new MethodSortMatcher<MethodDescription>(sort).matches(methodDescription), is(false));
    }

    @Test
    @Override
    public void testStringRepresentation() throws Exception {
        assertThat(new MethodSortMatcher<MethodDescription>(sort).toString(), is(sort.getDescription()));
    }

    private enum MockImplementation {

        CONSTRUCTOR {
            @Override
            protected void prepare(MethodDescription target) {
                when(target.isConstructor()).thenReturn(true);
            }
        },

        DEFAULT_METHOD {
            @Override
            protected void prepare(MethodDescription target) {
                when(target.isDefaultMethod()).thenReturn(true);
            }
        },

        METHOD {
            @Override
            protected void prepare(MethodDescription target) {
                when(target.isMethod()).thenReturn(true);
            }
        },

        VIRTUAL {
            @Override
            protected void prepare(MethodDescription target) {
                when(target.isVirtual()).thenReturn(true);
            }
        },

        TYPE_INITIALIZER {
            @Override
            protected void prepare(MethodDescription target) {
                when(target.isTypeInitializer()).thenReturn(true);
            }
        };

        protected abstract void prepare(MethodDescription target);
    }
}
