package net.bytebuddy.description;

public class ClassDemo {
    public Object c;

    public ClassDemo() {
        class ClassA{};
        c = new ClassA();
    }

    public Object classAObject() {
        class ClassA{};
        return new ClassA( );
    }

    public Runnable classWithAnonymousClass() {
        return new Runnable() {
            public void run() {
            }
        };
    }

    public static void main(String[] args) {
        ClassDemo classDemo = new ClassDemo();
        Class cls = classDemo.classAObject().getClass();

        System.out.print("Local class with Method = ");
        System.out.println(cls.getEnclosingMethod());
        //  Local class with Method = public java.lang.Object com.my.java.lang.ClassDemo.classAObject()

        System.out.print("Anonymous class with Method = ");
        System.out.println(classDemo.classWithAnonymousClass().getClass().getEnclosingMethod());
        //  Anonymous class with Method = public java.lang.Runnable com.my.java.lang.ClassDemo.classWithAnonymousClass()
    }
}
