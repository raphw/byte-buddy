package net.bytebuddy.asm;

public class Foo {

    void foo() {
        try {
            System.out.println("a");
        } catch (RuntimeException ignored) {
            System.out.println("b");
        }
    }

}
