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
package net.bytebuddy.build.gradle.android;

import com.android.build.api.instrumentation.AsmClassVisitorFactory;
import com.android.build.api.instrumentation.ClassContext;
import com.android.build.api.instrumentation.ClassData;
import org.objectweb.asm.ClassVisitor;

/**
 * A class visitor factory for applying Byte Buddy plugins.
 */
public abstract class ByteBuddyAsmClassVisitorFactory implements AsmClassVisitorFactory<ByteBuddyInstrumentationParameters> {

    /**
     * {@inheritDoc}
     */
    public boolean isInstrumentable(ClassData classData) {
        ByteBuddyInstrumentationParameters parameters = getParameters().get();
        ByteBuddyAndroidService service = parameters.getByteBuddyService().get();
        service.initialize(parameters);
        return service.matches(classData.getClassName());
    }

    /**
     * {@inheritDoc}
     */
    public ClassVisitor createClassVisitor(ClassContext classContext, ClassVisitor classVisitor) {
        return getParameters()
                .get()
                .getByteBuddyService()
                .get()
                .apply(classContext.getCurrentClassData().getClassName(), classVisitor);
    }
}
