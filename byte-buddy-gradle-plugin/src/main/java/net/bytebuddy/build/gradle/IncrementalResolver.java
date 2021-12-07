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

import net.bytebuddy.build.gradle.api.ChangeType;
import net.bytebuddy.build.gradle.api.FileChange;
import org.gradle.api.Project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * An incremental resolver is responsible to determine the file set to transform after a change.
 */
public interface IncrementalResolver {

    /**
     * Returns a list of files to transform after an incremental change.
     *
     * @param project    The current project.
     * @param changes    An iterable of all changes that were found.
     * @param sourceRoot The source directory.
     * @param targetRoot The target directory.
     * @param classPath  The class path available.
     * @return A list of files to include in the transformation.
     */
    List<File> apply(Project project, Iterable<FileChange> changes, File sourceRoot, File targetRoot, Iterable<File> classPath);

    /**
     * An incremental resolver that retransforms any file that has changed but no other files.
     */
    enum ForChangedFiles implements IncrementalResolver {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public List<File> apply(Project project, Iterable<FileChange> changes, File sourceRoot, File targetRoot, Iterable<File> classPath) {
            List<File> files = new ArrayList<File>();
            for (FileChange change : changes) {
                if (change.getChangeType() == ChangeType.REMOVED) {
                    File target = new File(targetRoot, sourceRoot.toURI().relativize(change.getFile().toURI()).getPath());
                    if (project.delete(target)) {
                        project.getLogger().debug("Deleted removed file {} to prepare incremental build", target);
                    }
                } else {
                    files.add(change.getFile());
                }
            }
            return files;
        }
    }
}
