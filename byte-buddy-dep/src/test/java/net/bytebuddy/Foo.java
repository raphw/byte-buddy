package net.bytebuddy;

public class Foo {

    static Class<?> type = new Runnable() {
        @Override
        public void run() {

        }
    }.getClass();

    public static void main(String[] args) {
//        Class<?> type = new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        }.getClass();

        System.out.println(type.isLocalClass());
        System.out.println(type.isMemberClass());
        System.out.println(type.isAnonymousClass());
    }

}
