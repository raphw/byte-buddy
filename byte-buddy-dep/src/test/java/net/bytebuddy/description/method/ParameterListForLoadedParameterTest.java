package net.bytebuddy.description.method;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

public class ParameterListForLoadedParameterTest extends AbstractParameterListTest<ParameterDescription.InDeclaredForm, Method> {

    @Override
    protected Method getFirst() throws Exception {
        return Foo.class.getDeclaredMethod("foo", Void.class);
    }

    @Override
    protected Method getSecond() throws Exception {
        return Foo.class.getDeclaredMethod("bar", Void.class);
    }

    @Override
    protected ParameterList<ParameterDescription.InDeclaredForm> asList(List<Method> elements) {
        List<ParameterDescription.InDeclaredForm> parameters = new LinkedList<ParameterDescription.InDeclaredForm>();
        for (Method method : elements) {
            parameters.add(new MethodDescription.ForLoadedMethod(method).getParameters().getOnly());
        }
        return new ParameterList.Explicit<ParameterDescription.InDeclaredForm>(parameters);
    }

    @Override
    protected ParameterDescription.InDeclaredForm asElement(Method element) {
        return new MethodDescription.ForLoadedMethod(element).getParameters().getOnly();
    }
}
