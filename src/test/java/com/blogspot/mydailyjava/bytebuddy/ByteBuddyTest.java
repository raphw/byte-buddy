package com.blogspot.mydailyjava.bytebuddy;

import org.junit.Test;

public class ByteBuddyTest {

    @Test
    public void testSubclass() throws Exception{
        System.out.println(new ByteBuddy().subclass(Object.class).name("my.Test").make().load(getClass().getClassLoader()).newInstance().toString());
    }
}
