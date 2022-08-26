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
package net.bytebuddy.build.gradle.android.asm;

import com.android.build.api.instrumentation.AsmClassVisitorFactory;
import com.android.build.api.instrumentation.ClassContext;
import com.android.build.api.instrumentation.ClassData;
import com.android.build.api.instrumentation.InstrumentationParameters;
import net.bytebuddy.build.gradle.android.service.BytebuddyService;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.objectweb.asm.ClassVisitor;

public abstract class ByteBuddyAsmClassVisitorFactory implements AsmClassVisitorFactory<ByteBuddyAsmClassVisitorFactory.Params> {

    @Override
    public ClassVisitor createClassVisitor(ClassContext classContext, ClassVisitor classVisitor) {
        return getService().apply(classContext.getCurrentClassData().getClassName(), classVisitor);
    }

    @Override
    public boolean isInstrumentable(ClassData classData) {
        Params parameters = getParameters().get();
        BytebuddyService service = parameters.getBytebuddyService().get();
        service.initialize(parameters.getRuntimeClasspath(), parameters.getAndroidBootClasspath(), parameters.getByteBuddyClasspath(), parameters.getLocalClassesDirs());
        return service.matches(classData.getClassName());
    }

    private BytebuddyService getService() {
        return getParameters().get().getBytebuddyService().get();
    }

    public interface Params extends InstrumentationParameters {

        @InputFiles
        @PathSensitive(PathSensitivity.RELATIVE)
        ConfigurableFileCollection getAndroidBootClasspath();

        @InputFiles
        @PathSensitive(PathSensitivity.RELATIVE)
        ConfigurableFileCollection getByteBuddyClasspath();

        @InputFiles
        @PathSensitive(PathSensitivity.RELATIVE)
        ConfigurableFileCollection getRuntimeClasspath();

        @Classpath
        ConfigurableFileCollection getLocalClassesDirs();

        @Internal
        Property<BytebuddyService> getBytebuddyService();
    }
}