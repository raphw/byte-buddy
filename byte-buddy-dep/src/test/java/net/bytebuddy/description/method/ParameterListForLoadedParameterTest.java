package net.bytebuddy.description.method;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

public class ParameterListForLoadedParameterTest extends AbstractParameterListTest<ParameterDescription.InDefinedShape, Method> {

    @Override
    protected Method getFirst() throws Exception {
        return Foo.class.getDeclaredMethod("foo", Void.class);
    }

    @Override
    protected Method getSecond() throws Exception {
        return Foo.class.getDeclaredMethod("bar", Void.class);
    }

    @Override
    protected ParameterList<ParameterDescription.InDefinedShape> asList(List<Method> elements) {
        List<ParameterDescription.InDefinedShape> parameters = new LinkedList<ParameterDescription.InDefinedShape>();
        for (Method method : elements) {
            parameters.add(new MethodDescription.ForLoadedMethod(method).getParameters().getOnly());
        }
        return new ParameterList.Explicit<ParameterDescription.InDefinedShape>(parameters);
    }

    @Override
    protected ParameterDescription.InDefinedShape asElement(Method element) {
        return new MethodDescription.ForLoadedMethod(element).getParameters().getOnly();
    }
}
