package net.bytebuddy.instrumentation;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

public class MethodDelegationEqualsHashCodeTest {

    private static final String FOO = "foo", BAR = "bar";

    public static class Foo {

        public static void foo() {
            /* empty */
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj.getClass() == getClass();
        }

        @Override
        public int hashCode() {
            return 31;
        }
    }

    public static class Bar {

        public static void bar() {
            /* empty */
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj.getClass() == getClass();
        }

        @Override
        public int hashCode() {
            return 27;
        }
    }

    @Test
    public void testStaticMethodDelegation() throws Exception {
        assertThat(MethodDelegation.to(Foo.class).hashCode(), is(MethodDelegation.to(Foo.class).hashCode()));
        assertThat(MethodDelegation.to(Foo.class), is(MethodDelegation.to(Foo.class)));
        assertThat(MethodDelegation.to(Foo.class).hashCode(), not(is(MethodDelegation.to(Bar.class).hashCode())));
        assertThat(MethodDelegation.to(Foo.class), not(is(MethodDelegation.to(Bar.class))));
    }

    @Test
    public void testStaticInstanceDelegation() throws Exception {
        assertThat(MethodDelegation.to(new Foo()).hashCode(), is(MethodDelegation.to(new Foo()).hashCode()));
        assertThat(MethodDelegation.to(new Foo()), is(MethodDelegation.to(new Foo())));
        assertThat(MethodDelegation.to(new Foo()).hashCode(), not(is(MethodDelegation.to(new Bar()).hashCode())));
        assertThat(MethodDelegation.to(new Foo()), not(is(MethodDelegation.to(new Bar()))));
    }

    @Test
    public void testStaticInstanceDelegationWithFieldName() throws Exception {
        assertThat(MethodDelegation.to(new Foo(), FOO).hashCode(), is(MethodDelegation.to(new Foo(), FOO).hashCode()));
        assertThat(MethodDelegation.to(new Foo(), FOO), is(MethodDelegation.to(new Foo(), FOO)));
        assertThat(MethodDelegation.to(new Foo(), FOO).hashCode(), not(is(MethodDelegation.to(new Foo(), BAR).hashCode())));
        assertThat(MethodDelegation.to(new Foo()), not(is(MethodDelegation.to(new Foo(), BAR))));
    }

    @Test
    public void testInstanceFieldDelegation() throws Exception {
        assertThat(MethodDelegation.instanceField(Foo.class, FOO).hashCode(), is(MethodDelegation.instanceField(Foo.class, FOO).hashCode()));
        assertThat(MethodDelegation.instanceField(Foo.class, FOO), is(MethodDelegation.instanceField(Foo.class, FOO)));
        assertThat(MethodDelegation.instanceField(Foo.class, FOO).hashCode(), not(is(MethodDelegation.instanceField(Bar.class, FOO).hashCode())));
        assertThat(MethodDelegation.instanceField(Foo.class, FOO), not(is(MethodDelegation.instanceField(Bar.class, FOO))));
        assertThat(MethodDelegation.instanceField(Foo.class, FOO).hashCode(), not(is(MethodDelegation.instanceField(Foo.class, BAR).hashCode())));
        assertThat(MethodDelegation.instanceField(Foo.class, FOO), not(is(MethodDelegation.instanceField(Foo.class, BAR))));
    }

    @Test
    public void testConstructorDelegation() throws Exception {
        assertThat(MethodDelegation.construct(Foo.class).hashCode(), is(MethodDelegation.construct(Foo.class).hashCode()));
        assertThat(MethodDelegation.construct(Foo.class), is(MethodDelegation.construct(Foo.class)));
        assertThat(MethodDelegation.construct(Foo.class).hashCode(), not(is(MethodDelegation.construct(Bar.class).hashCode())));
        assertThat(MethodDelegation.construct(Foo.class), not(is(MethodDelegation.construct(Bar.class))));
    }
}
