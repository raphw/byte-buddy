package net.bytebuddy.description.field;

import java.util.List;

public class FieldListExplicitTest extends AbstractFieldListTest<FieldDescription, FieldDescription> {

    protected FieldDescription getFirst() throws Exception {
        return new FieldDescription.ForLoadedField(Foo.class.getDeclaredField("foo"));
    }

    protected FieldDescription getSecond() throws Exception {
        return new FieldDescription.ForLoadedField(Foo.class.getDeclaredField("bar"));
    }

    protected FieldList<FieldDescription> asList(List<FieldDescription> elements) {
        return new FieldList.Explicit<FieldDescription>(elements);
    }

    protected FieldDescription asElement(FieldDescription element) {
        return element;
    }
}
