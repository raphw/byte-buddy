package net.bytebuddy.description.method;

import java.util.List;

public class ParameterListExplicitTest extends AbstractParameterListTest<ParameterDescription, ParameterDescription> {

    @Override
    protected ParameterDescription getFirst() throws Exception {
        return new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod("foo", Void.class)).getParameters().getOnly();
    }

    @Override
    protected ParameterDescription getSecond() throws Exception {
        return new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod("bar", Void.class)).getParameters().getOnly();
    }

    @Override
    protected ParameterList<ParameterDescription> asList(List<ParameterDescription> elements) {
        return new ParameterList.Explicit<ParameterDescription>(elements);
    }

    @Override
    protected ParameterDescription asElement(ParameterDescription element) {
        return element;
    }
}
