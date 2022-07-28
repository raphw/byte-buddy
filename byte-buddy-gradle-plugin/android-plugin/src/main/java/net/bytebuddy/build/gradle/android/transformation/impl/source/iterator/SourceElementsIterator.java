package net.bytebuddy.build.gradle.android.transformation.impl.source.iterator;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.build.gradle.android.utils.Many;

import java.util.ArrayList;
import java.util.Iterator;

public class SourceElementsIterator implements Iterator<Plugin.Engine.Source.Element> {

    private final ArrayList<? extends Iterator<Plugin.Engine.Source.Element>> iterators;

    public SourceElementsIterator(ArrayList<? extends Iterator<Plugin.Engine.Source.Element>> iterators) {
        this.iterators = iterators;
    }

    @Override
    public boolean hasNext() {
        while (Many.isNotEmpty(iterators) && !iterators.get(0).hasNext()) {
            Iterator<Plugin.Engine.Source.Element> first = iterators.get(0);
            iterators.remove(first);
        }

        if (iterators.isEmpty()) {
            return false;
        }

        return iterators.get(0).hasNext();
    }

    @Override
    public Plugin.Engine.Source.Element next() {
        Iterator<Plugin.Engine.Source.Element> first = iterators.get(0);
        Plugin.Engine.Source.Element next = first.next();
        if (!first.hasNext()) {
            iterators.remove(first);
        }
        return next;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
