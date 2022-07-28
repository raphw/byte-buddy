package net.bytebuddy.build.gradle.android.transformation.impl.source;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.build.gradle.android.transformation.impl.source.iterator.SourceElementsIterator;
import net.bytebuddy.build.gradle.android.utils.Many;
import net.bytebuddy.dynamic.ClassFileLocator;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
            List<ClassFileLocator> locators = Many.map(origins, Origin::getClassFileLocator);
            locator = new ClassFileLocator.Compound(locators);
        }

        return locator;
    }

    @Override
    public Iterator<Element> iterator() {
        return new SourceElementsIterator(Many.map(origins, Origin::iterator));
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
}