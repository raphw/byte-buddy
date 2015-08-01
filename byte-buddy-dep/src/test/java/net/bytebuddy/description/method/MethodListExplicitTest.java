package net.bytebuddy.description.method;

import java.util.List;

public class MethodListExplicitTest extends AbstractMethodListTest<MethodDescription, MethodDescription> {

    @Override
    protected MethodDescription getFirst() throws Exception {
        return new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod("foo"));
    }

    @Override
    protected MethodDescription getSecond() throws Exception {
        return new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod("bar"));
    }

    @Override
    protected MethodList<MethodDescription> asList(List<MethodDescription> elements) {
        return new MethodList.Explicit<MethodDescription>(elements);
    }

    @Override
    protected MethodDescription asElement(MethodDescription element) {
        return element;
    }
}
