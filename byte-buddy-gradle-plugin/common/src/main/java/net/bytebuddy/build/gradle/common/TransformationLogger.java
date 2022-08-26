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
package net.bytebuddy.build.gradle.common;

import java.util.List;
import java.util.Map;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import org.slf4j.Logger;

/**
 * A {@link net.bytebuddy.build.Plugin.Engine.Listener} that logs several relevant events during the build.
 */
public final class TransformationLogger extends Plugin.Engine.Listener.Adapter {

    /**
     * The logger to delegate to.
     */
    private final Logger logger;

    /**
     * Creates a new transformation logger.
     *
     * @param logger The logger to delegate to.
     */
    public TransformationLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void onTransformation(TypeDescription typeDescription, List<Plugin> plugins) {
        logger.debug("Transformed {} using {}", typeDescription, plugins);
    }

    @Override
    public void onError(TypeDescription typeDescription, Plugin plugin, Throwable throwable) {
        logger.warn("Failed to transform {} using {}", typeDescription, plugin, throwable);
    }

    @Override
    public void onError(Map<TypeDescription, List<Throwable>> throwables) {
        logger.warn("Failed to transform {} types", throwables.size());
    }

    @Override
    public void onError(Plugin plugin, Throwable throwable) {
        logger.error("Failed to close {}", plugin, throwable);
    }

    @Override
    public void onLiveInitializer(TypeDescription typeDescription, TypeDescription definingType) {
        logger.debug("Discovered live initializer for {} as a result of transforming {}", definingType, typeDescription);
    }
}
