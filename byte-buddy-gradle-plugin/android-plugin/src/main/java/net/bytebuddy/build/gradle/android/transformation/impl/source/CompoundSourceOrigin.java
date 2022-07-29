/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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