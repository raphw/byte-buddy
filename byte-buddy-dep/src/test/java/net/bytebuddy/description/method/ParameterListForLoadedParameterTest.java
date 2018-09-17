package net.bytebuddy.description.method;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ParameterListForLoadedParameterTest extends AbstractParameterListTest<ParameterDescription.InDefinedShape, Method> {

    protected Method getFirst() throws Exception {
        return Foo.class.getDeclaredMethod("foo", Void.class);
    }

    protected Method getSecond() throws Exception {
        return Foo.class.getDeclaredMethod("bar", Void.class);
    }

    protected ParameterList<ParameterDescription.InDefinedShape> asList(List<Method> elements) {
        List<ParameterDescription.InDefinedShape> parameters = new ArrayList<ParameterDescription.InDefinedShape>(elements.size());
        for (Method method : elements) {
            parameters.add(new MethodDescription.ForLoadedMethod(method).getParameters().getOnly());
        }
        return new ParameterList.Explicit<ParameterDescription.InDefinedShape>(parameters);
    }

    protected ParameterDescription.InDefinedShape asElement(Method element) {
        return new MethodDescription.ForLoadedMethod(element).getParameters().getOnly();
    }
}
