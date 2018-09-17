package net.bytebuddy.description.method;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

public class MethodListForLoadedTypesTest extends AbstractMethodListTest<Method, MethodDescription.InDefinedShape> {

    protected Method getFirst() throws Exception {
        return Foo.class.getDeclaredMethod("foo");
    }

    protected Method getSecond() throws Exception {
        return Foo.class.getDeclaredMethod("bar");
    }

    protected MethodList<MethodDescription.InDefinedShape> asList(List<Method> elements) {
        return new MethodList.ForLoadedMethods(new Constructor<?>[0], elements.toArray(new Method[elements.size()]));
    }

    protected MethodDescription.InDefinedShape asElement(Method element) {
        return new MethodDescription.ForLoadedMethod(element);
    }
}
