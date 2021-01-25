package net.bytebuddy.pool;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.ClassFileLocator;

public class MyApplication {
    // 从 ClassPath 生成一个 Type
    public static void main(String[] args) {
        TypePool typePool = TypePool.Default.ofClassPath();
        new ByteBuddy()
                .redefine(typePool.describe("foo.Bar").resolve(), // do not use 'Bar.class'
                        ClassFileLocator.ForClassLoader.ofClassPath())
                .defineField("qux", String.class) // we learn more about defining fields later
                .make()
                .load(ClassLoader.getSystemClassLoader());
    }
}
