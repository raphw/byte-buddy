package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.StubMethodByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.method.matcher.MethodMatchers;
import org.junit.Test;

public class ByteBuddyTest {

    @Test
    public void testSubclass() throws Exception{
        String text = ByteBuddy.make()
                .subclass(Object.class)
                .name("my.Test")
                .intercept(MethodMatchers.returns(String.class), StubMethodByteCodeAppender.INSTANCE)
                .make()
                .load(getClass().getClassLoader())
                .newInstance()
                .toString();
        System.out.println(text);
    }
}
