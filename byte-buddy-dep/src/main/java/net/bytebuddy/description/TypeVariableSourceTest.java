package net.bytebuddy.description;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

import java.lang.reflect.Method;

public class TypeVariableSourceTest {
    // 食物
    public static class Food {
    }

    // makeFood 方法定义在类内，他的包裹类就是 TypeVariableSourceTest
    public <T extends Food> T makeFood(T food) {
        return food;
    }

    // makeSauce 方法产生了一个匿名类，对这个匿名类而言，他的包裹方法就是 makeSauce 方法
    public Object makeSauce() {
        class Sauce {
            public void print() {
            }
        }

        return new Sauce();
    }

    public static void main(String[] args) throws Exception {
        Method makeFood = TypeVariableSourceTest.class.getDeclaredMethod("makeFood", Food.class);

        MethodDescription.ForLoadedMethod makeFoodBD = new MethodDescription.ForLoadedMethod(makeFood);
        System.out.println("Enclosing 外围的包裹类" + makeFoodBD.getEnclosingSource().toString());

        TypeDescription.ForLoadedType sauceBD = new TypeDescription.ForLoadedType(new TypeVariableSourceTest().makeSauce().getClass());
        System.out.println("Enclosing 外围的包裹方法" + sauceBD.getEnclosingSource());
    }
}
