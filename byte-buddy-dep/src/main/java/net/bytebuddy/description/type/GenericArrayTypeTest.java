package net.bytebuddy.description.type;

import java.lang.reflect.*;
import java.util.List;

public class GenericArrayTypeTest<T> {
    // 这里面有各种各样的数组：各有不同 方便看测试效果
    // 含有泛型数组的才是GenericArrayType
    public void testGenericArrayType(List<String>[] pTypeArray, T[] vTypeArray, List<String> list,
                                     List<? extends Number> wildcardList, String[] strings, GenericArrayTypeTest[] test) {
    }

    // GenericArrayType有两个都是：List<String>[] pTypeArray, T[] vTypeArray它哥俩都是泛型数组。但是这两String[] strings, Main[] test可不是，他俩属于Class普通类型
    // genericArrayType.getGenericComponentType() 返回的类型为 ParameterizedTypeImpl 类型，能够获取到数组的实际参数类型
    public static void main(String[] args) {
        Method[] declaredMethods = GenericArrayTypeTest.class.getDeclaredMethods();
        for (Method method : declaredMethods) {
            // main方法不用处理
            if (method.getName().startsWith("main")) {
                continue;
            }

            // 开始处理该方法 === 打印出此方法签名
            System.out.println("declare Method:" + method); //declare Method:public void com.fsx.maintest.GenericArrayTypeTest.testGenericArrayType(java.util.List[],java.lang.Object[],java.util.List,java.lang.String[],com.fsx.maintest.GenericArrayTypeTest[])

            // 该方法能获取到该方法所有的实际的参数化类型，比如本例中有五个参数，那数组长度就是5
            Type[] types = method.getGenericParameterTypes();

            // 分组打印出来
            for (Type type : types) {
                if (type instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    System.out.println("ParameterizedType type :" + parameterizedType);
                }
                else if (type instanceof GenericArrayType) {
                    // 从结果

                    GenericArrayType genericArrayType = (GenericArrayType) type;
                    System.out.println("GenericArrayType type :" + genericArrayType);

                    Type genericComponentType = genericArrayType.getGenericComponentType();
                    System.out.println("genericComponentType:" + genericComponentType);
                }
                else if (type instanceof WildcardType) {
                    WildcardType wildcardType = (WildcardType) type;
                    System.out.println("WildcardType type :" + wildcardType);
                }
                else if (type instanceof TypeVariable) {
                    TypeVariable typeVariable = (TypeVariable) type;
                    System.out.println("TypeVariable type :" + typeVariable);
                }
                else {
                    Class clazz = (Class) type;
                    System.out.println("type :" + clazz);
                }
            }
        }
    }
}
