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
package net.bytebuddy.utility.visitor;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.utility.OpenedClassReader;
import org.objectweb.asm.ClassVisitor;

import java.util.List;

/**
 * A {@link ClassVisitor} that supplies contextual information, similar to a {@link DynamicType}.
 * Reading auxiliary types or the loaded type initializer of this type
 */
public abstract class ContextClassVisitor extends ClassVisitor {

    /**
     * If {@code true}, this class visitor permits auxiliary types or an active type initializer.
     */
    private boolean active;

    /**
     * Creates a new context class visitor.
     *
     * @param classVisitor The class visitor to delegate to.
     */
    protected ContextClassVisitor(ClassVisitor classVisitor) {
        super(OpenedClassReader.ASM_API, classVisitor);
    }

    /**
     * Allows this class visitor to result in auxiliary types or an active type initializer.
     *
     * @return This instance marked as active.
     */
    public ContextClassVisitor active() {
        active = true;
        return this;
    }

    /**
     * Returns the auxiliary types that this class visitor currently supplies.
     *
     * @return The auxiliary types that this class visitor currently supplies.
     */
    public abstract List<DynamicType> getAuxiliaryTypes();

    /**
     * Returns the loaded type initializer that this class visitor currently implies.
     *
     * @return The loaded type initializer that this class visitor currently implies.
     */
    public abstract LoadedTypeInitializer getLoadedTypeInitializer();

    @Override
    public void visitEnd() {
        super.visitEnd();
        if (!active && (!getAuxiliaryTypes().isEmpty() || getLoadedTypeInitializer().isAlive())) {
            throw new IllegalStateException(this + " is not defined 'active' but defines auxiliary types or an alive type initializer");
        }
    }
}
