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
package net.bytebuddy.dynamic.loading;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * A class file transformer that does not apply a transformation.
 */
public enum NoOpClassFileTransformer implements ClassFileTransformer {

    /**
     * The singleton instance.
     */
    INSTANCE;

    /**
     * Indicates that no transformation is to applied.
     */
    private static final byte[] NO_TRANSFORMATION = null;

    /**
     * {@inheritDoc}
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Array is guaranteed to be null")
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {
        return NO_TRANSFORMATION;
    }
}
