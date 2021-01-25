package net.bytebuddy.description;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

import java.lang.reflect.Method;

public class ByteCodeElementTest {
    private void notSee() {

    }

    public void canSee() {

    }

    public void test() throws Exception {
        Method notsee = ByteCodeElementTest.class.getDeclaredMethod("notSee");
        Method cansee = ByteCodeElementTest.class.getDeclaredMethod("canSee");

        MethodDescription.ForLoadedMethod notSeeBD = new MethodDescription.ForLoadedMethod(notsee);
        MethodDescription.ForLoadedMethod canSeeBD = new MethodDescription.ForLoadedMethod(cansee);

        // 同包下的另外一个类
        TypeDescription samePkgAnotherClass = TypeDescription.ForLoadedType.of(DemoModifierContributor.class);
        System.out.println("samePkgAnotherClass can not see private : " + notSeeBD.isVisibleTo(samePkgAnotherClass));
        System.out.println("samePkgAnotherClass can not use private : " + notSeeBD.isAccessibleTo(samePkgAnotherClass));

        System.out.println("samePkgAnotherClass can see public : " + canSeeBD.isVisibleTo(samePkgAnotherClass));
        System.out.println("samePkgAnotherClass can use public : " + canSeeBD.isAccessibleTo(samePkgAnotherClass));
    }

    public static void main(String[] args) throws Exception{
        new ByteCodeElementTest().test();
    }
}
