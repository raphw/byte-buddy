package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;

import java.util.LinkedList;
import java.util.List;

public class CollectionRawTypeMatcher<T extends Iterable<? extends GenericTypeDescription>> extends ElementMatcher.Junction.AbstractBase<T> {

    private final ElementMatcher<? super Iterable<? extends TypeDescription>> matcher;

    public CollectionRawTypeMatcher(ElementMatcher<? super Iterable<? extends TypeDescription>> matcher) {
        this.matcher = matcher;
    }

    public boolean matches(T target) {
        List<TypeDescription> typeDescriptions = new LinkedList<TypeDescription>();
        for (GenericTypeDescription typeDescription : target) {
            typeDescriptions.add(typeDescription.asRawType());
        }
        return matcher.matches(typeDescriptions);
    }
}
