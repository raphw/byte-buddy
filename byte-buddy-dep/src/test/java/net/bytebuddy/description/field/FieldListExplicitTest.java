package net.bytebuddy.description.field;

import java.util.List;

public class FieldListExplicitTest extends AbstractFieldListTest<FieldDescription, FieldDescription> {

    @Override
    protected FieldDescription getFirst() throws Exception {
        return new FieldDescription.ForLoadedField(Foo.class.getDeclaredField("foo"));
    }

    @Override
    protected FieldDescription getSecond() throws Exception {
        return new FieldDescription.ForLoadedField(Foo.class.getDeclaredField("bar"));
    }

    @Override
    protected FieldList<FieldDescription> asList(List<FieldDescription> elements) {
        return new FieldList.Explicit<FieldDescription>(elements);
    }

    @Override
    protected FieldDescription asElement(FieldDescription element) {
        return element;
    }
}
