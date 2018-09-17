package net.bytebuddy.description.type;

import java.util.List;

public class TypeListExplicitTest extends AbstractTypeListTest<TypeDescription> {

    protected TypeDescription getFirst() throws Exception {
        return TypeDescription.ForLoadedType.of(Foo.class);
    }

    protected TypeDescription getSecond() throws Exception {
        return TypeDescription.ForLoadedType.of(Bar.class);
    }

    protected TypeList asList(List<TypeDescription> elements) {
        return new TypeList.Explicit(elements);
    }

    protected TypeDescription asElement(TypeDescription element) {
        return element;
    }
}
