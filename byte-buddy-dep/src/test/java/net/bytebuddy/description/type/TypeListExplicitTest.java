package net.bytebuddy.description.type;

import java.util.List;

public class TypeListExplicitTest extends AbstractTypeListTest<TypeDescription> {

    @Override
    protected TypeDescription getFirst() throws Exception {
        return TypeDescription.ForLoadedType.of(Foo.class);
    }

    @Override
    protected TypeDescription getSecond() throws Exception {
        return TypeDescription.ForLoadedType.of(Bar.class);
    }

    @Override
    protected TypeList asList(List<TypeDescription> elements) {
        return new TypeList.Explicit(elements);
    }

    @Override
    protected TypeDescription asElement(TypeDescription element) {
        return element;
    }
}
