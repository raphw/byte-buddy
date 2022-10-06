package net.bytebuddy.build.gradle.android;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.dynamic.ClassFileLocator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.Manifest;

/**
 * Needed to go through sources from across multiple locations, either multiple folders or even jar files too.
 */
public class CompoundSourceOrigin implements Plugin.Engine.Source,
        Plugin.Engine.Source.Origin {
    private final Set<Origin> origins;

    private ClassFileLocator locator;

    public CompoundSourceOrigin(Set<Origin> origins) {
        this.origins = origins;
    }

    private ClassFileLocator getLocator() {
        if (locator == null) {
            List<ClassFileLocator> locators = map(origins, Origin::getClassFileLocator);
            locator = new ClassFileLocator.Compound(locators);
        }

        return locator;
    }

    @Override
    public Iterator<Element> iterator() {
        return new SourceElementsIterator(map(origins, Origin::iterator));
    }

    @Override
    public void close() {
        origins.forEach(it -> {
            try {
                it.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public ClassFileLocator getClassFileLocator() {
        return getLocator();
    }

    @Override
    public Manifest getManifest() {
        return Plugin.Engine.Source.Origin.NO_MANIFEST;
    }

    @Override
    public Plugin.Engine.Source.Origin read() {
        return this;
    }

    private <T, R> ArrayList<R> map(Collection<T> original, Function<T, R> transformation) {
        ArrayList<R> list = new ArrayList<>();
        original.forEach(item -> list.add(transformation.apply(item)));

        return list;
    }

    private static class SourceElementsIterator implements Iterator<Plugin.Engine.Source.Element> {

        private final ArrayList<? extends Iterator<Plugin.Engine.Source.Element>> iterators;

        public SourceElementsIterator(ArrayList<? extends Iterator<Plugin.Engine.Source.Element>> iterators) {
            this.iterators = iterators;
        }

        @Override
        public boolean hasNext() {
            while (!iterators.isEmpty() && !iterators.get(0).hasNext()) {
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
}