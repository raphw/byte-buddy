package net.bytebuddy.description.type.generic;

import java.util.List;

public class GenericTypeListExplicitTest extends AbstractGenericTypeListTest<GenericTypeDescription> {

    @Override
    protected GenericTypeDescription getFirst() throws Exception {
        return GenericTypeDescription.Sort.describe(Holder.class.getGenericInterfaces()[0]);
    }

    @Override
    protected GenericTypeDescription getSecond() throws Exception {
        return GenericTypeDescription.Sort.describe(Holder.class.getGenericInterfaces()[1]);
    }

    @Override
    protected GenericTypeList asList(List<GenericTypeDescription> elements) {
        return new GenericTypeList.Explicit(elements);
    }

    @Override
    protected GenericTypeDescription asElement(GenericTypeDescription element) {
        return element;
    }
}
