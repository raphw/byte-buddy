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
package net.bytebuddy.dynamic;

import net.bytebuddy.description.method.MethodDescription;

/**
 * Implements a strategy for deciding if a visibility bridge should be generated. A visibility bridge is necessary
 * if a public type inherits a public method from a package-private type to allow for invoking that method without
 * specific privileges as the reflection API only considers the method's declaring type.
 */
public interface VisibilityBridgeStrategy {

    /**
     * Determines if a visibility bridge should be generated for a method that is eligable.
     *
     * @param methodDescription The method that would require a visibility bridge.
     * @return {@code true} if a visibility bridge should be generated.
     */
    boolean generateVisibilityBridge(MethodDescription methodDescription);

    /**
     * Default implementations of visibility bridge strategies.
     */
    enum Default implements VisibilityBridgeStrategy {

        /**
         * Always generates a visibility bridge.
         */
        ALWAYS {
            /**
             * {@inheritDoc}
             */
            public boolean generateVisibilityBridge(MethodDescription methodDescription) {
                return true;
            }
        },

        /**
         * Only generates visibility bridges for non-generified methods what was the behavior of <i>javac</i> until Java 11.
         */
        ON_NON_GENERIC_METHOD {
            /**
             * {@inheritDoc}
             */
            public boolean generateVisibilityBridge(MethodDescription methodDescription) {
                return !methodDescription.isGenerified();
            }
        },

        /**
         * Never generates a visibility bridge.
         */
        NEVER {
            /**
             * {@inheritDoc}
             */
            public boolean generateVisibilityBridge(MethodDescription methodDescription) {
                return false;
            }
        }
    }
}
