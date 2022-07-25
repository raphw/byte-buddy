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
package net.bytebuddy.build.gradle;

import java.util.List;

/**
 * Determines how and if plugins are discovered.
 */
public enum Discovery {

    /**
     * Attempts discovery of plugins even if they are explicitly registered.
     */
    ALL(false) {
        @Override
        protected boolean isDiscover(List<Transformation> transformations) {
            return true;
        }
    },

    /**
     * Attempts discovery of plugins but does not discover plugins that are explicitly registered.
     */
    UNIQUE(true) {
        @Override
        protected boolean isDiscover(List<Transformation> transformations) {
            return true;
        }
    },

    /**
     * Only discovers plugins if no plugin was explicitly registered. This is the default configuration.
     */
    EMPTY(true) {
        @Override
        protected boolean isDiscover(List<Transformation> transformations) {
            return transformations.isEmpty();
        }
    },

    /**
     * Does never discover plugins.
     */
    NONE(true) {
        @Override
        protected boolean isDiscover(List<Transformation> transformations) {
            return false;
        }
    };

    /**
     * {@code true} if explicit configurations should be recorded.
     */
    private final boolean recordConfiguration;

    /**
     * Creates a new discovery configuration.
     *
     * @param recordConfiguration {@code true} if explicit configurations should be recorded.
     */
    Discovery(boolean recordConfiguration) {
        this.recordConfiguration = recordConfiguration;
    }

    /**
     * Returns {@code true} if explicit configurations should be recorded.
     *
     * @return {@code true} if explicit configurations should be recorded.
     */
    protected boolean isRecordConfiguration() {
        return recordConfiguration;
    }

    /**
     * Determines if plugins should be discovered from the class path.
     *
     * @param transformations The configured transformers.
     * @return {@code true} if plugins should be discovered from the class path.
     */
    protected abstract boolean isDiscover(List<Transformation> transformations);
}
