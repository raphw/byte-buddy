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
package net.bytebuddy.build.gradle.android.connector.adapter.current.asm;

import com.android.build.api.instrumentation.AsmClassVisitorFactory;
import com.android.build.api.instrumentation.ClassContext;
import com.android.build.api.instrumentation.ClassData;
import com.android.build.api.instrumentation.InstrumentationParameters;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.CompileClasspath;
import org.objectweb.asm.ClassVisitor;

public abstract class ByteBuddyAsmClassVisitorFactory implements AsmClassVisitorFactory<ByteBuddyAsmClassVisitorFactory.Params> {

    @Override
    public ClassVisitor createClassVisitor(ClassContext classContext, ClassVisitor classVisitor) {
        return BytebuddyManager.apply(classContext.getCurrentClassData().getClassName(), classVisitor);
    }

    @Override
    public boolean isInstrumentable(ClassData classData) {
        Params params = getParameters().get();
        BytebuddyManager.initialize(params.getClasspath(), params.getAndroidBootClasspath(), params.getByteBuddyClasspath());
        return BytebuddyManager.matches(classData.getClassName());
    }

    public interface Params extends InstrumentationParameters {

        @CompileClasspath
        ConfigurableFileCollection getAndroidBootClasspath();

        @CompileClasspath
        ConfigurableFileCollection getByteBuddyClasspath();

        @CompileClasspath
        ConfigurableFileCollection getClasspath();
    }
}