package net.bytebuddy.description.type;

import java.lang.reflect.*;
import java.util.List;
import java.util.Map;

public class WildcardTypeTest {
    private List<? extends Number> listUpper;
    private List<? super String> listLower;
    private List<String> list;

    private Map<? extends String, ? super Number> map1;
    private Map<? extends String, ?> map2;

    private Class<?> clazz;
    // 不写泛型的list
    private List objList;


    private static void printWildcardType(WildcardType wildcardType) {
        for (Type type : wildcardType.getUpperBounds()) {
            System.out.println("\t\t上界：" + type);
        }
        for (Type type : wildcardType.getLowerBounds()) {
            System.out.println("\t\t下界：" + type);
        }
    }

    public static void main(String[] args) {
        Field f = null;
        try {
            Field[] fields = WildcardTypeTest.class.getDeclaredFields();

            for (int i = 0; i < fields.length; i++) {
                f = fields[i];
                System.out.println("begin ******当前field:" + f.getName() + " *************************");
                Type genericType = f.getGenericType(); // 获取字段的泛型参数
                if (genericType instanceof ParameterizedType) {
                    System.out.println("\tParameterizedType type :" + genericType);
                    ParameterizedType parameterizedType = (ParameterizedType) genericType;

                    for (Type type : parameterizedType.getActualTypeArguments()) {
                        //参数化类型可能有多个
                        System.out.println("\t  获取到getActualTypeArguments为:" + type);
                        if (type instanceof WildcardType) {
                            printWildcardType((WildcardType) type);
                        }
                    }
                }
                else if (genericType instanceof GenericArrayType) {
                    GenericArrayType genericArrayType = (GenericArrayType) genericType;
                    System.out.println("\tGenericArrayType type :" + genericArrayType);
                    Type genericComponentType = genericArrayType.getGenericComponentType();
                    if (genericComponentType instanceof WildcardType) {
                        printWildcardType((WildcardType) genericComponentType);
                    }
                }
                else if (genericType instanceof TypeVariable) {
                    TypeVariable typeVariable = (TypeVariable) genericType;
                    System.out.println("\ttypeVariable:" + typeVariable);

                }
                else {
                    System.out.println("\ttype :" + genericType);
                    if (genericType instanceof WildcardType) {
                        printWildcardType((WildcardType) genericType);
                    }
                }
                System.out.println("end ******当前field:" + f.getName() + " *************************");
                System.out.println();
            }
        }
        catch (Exception e) {
        }
    }
}

