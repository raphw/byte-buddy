package net.bytebuddy.description.type;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

public class TypeVariableSample<T extends CharSequence> {

    public static void main(String[] args) {
        TypeVariable<Class<TypeVariableSample>>[] t = TypeVariableSample.class.getTypeParameters();
        for(TypeVariable<Class<TypeVariableSample>> m : t) {
            /**
             * 获得类型变量在声明的时候的名称，此例中为T
             */
            System.out.println(m.getName());
            /**
             * 获得类型变量的上边界，若无显式的定义（extends）,默认为Object;类型变量的上边界可能不止一个，
             * 因为可以用&符号限定多个（这其中有且只能有一个为类或抽象类，且必须放在extends后的第一个，
             * 即若有多个上边界，则第一个&后必须为接口）
             *
             */
            Type[] bounds = m.getBounds();
            for(Type t1 : bounds) {
                System.out.println(t1);
            }
            /**
             * 获得声明这个类型变量的类型及名称
             * 类中：class reflect.ConstructorTest
             */
            System.out.println(m.getGenericDeclaration());
        }
    }
}
