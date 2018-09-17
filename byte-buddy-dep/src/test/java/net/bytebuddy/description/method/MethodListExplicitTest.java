package net.bytebuddy.description.method;

import java.util.List;

public class MethodListExplicitTest extends AbstractMethodListTest<MethodDescription, MethodDescription> {

    protected MethodDescription getFirst() throws Exception {
        return new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod("foo"));
    }

    protected MethodDescription getSecond() throws Exception {
        return new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod("bar"));
    }

    protected MethodList<MethodDescription> asList(List<MethodDescription> elements) {
        return new MethodList.Explicit<MethodDescription>(elements);
    }

    protected MethodDescription asElement(MethodDescription element) {
        return element;
    }
}
