package net.bytebuddy.description.type;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

public class ParameterizedTypeTest {
    private Map<String, ParameterizedTypeTest> map;
    private Set<String> set1;
    private Class<?> clz;
    private Holder<String> holder;
    private List<String> list;
    private ArrayList<String> arrayList;
    private Map.Entry<String, String> entry;

    private String str;
    private Integer i;
    private Set set;
    private List aList;

    static class Holder<V> {
    }

    public static void main(String[] args) {
        Field f = null;
        try {
            // 拿到所有的字段
            Field[] fields = ParameterizedTypeTest.class.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                f = fields[i];


                if (f.getGenericType() instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) f.getGenericType();
                    System.out.println(f.getName() + "：");

                    System.out.println("\t ParameterizedType:" + Arrays.asList(parameterizedType.getActualTypeArguments()));
                    System.out.println("\t getRawType:" + parameterizedType.getRawType());
                    System.out.println("\t getOwnerType:" + parameterizedType.getOwnerType());
                }
                // 输出不是ParameterizedType 参数化类型的
                else {
                    System.out.println(f.getName() + ":is not ParameterizedType ");
                }
            }
        }
        catch (Exception e) {
        }
    }
}
