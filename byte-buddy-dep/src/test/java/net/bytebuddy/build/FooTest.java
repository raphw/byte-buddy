package net.bytebuddy.build;

import org.junit.Test;

public class FooTest {

    @Test
    public void testFoo() {
        System.out.println(new Foo().hashCode());
    }
}