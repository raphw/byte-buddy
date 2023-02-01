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
package net.bytebuddy.build;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;

/**
 * A plugin that replaces names that are discovered in class files.
 */
@HashCodeAndEqualsPlugin.Enhance
public class RenamingPlugin extends AsmVisitorWrapper.AbstractBase implements Plugin {

    /**
     * The renaming to apply.
     */
    private final Renaming renaming;

    /**
     * A matcher that determines what types to consider for renaming.
     */
    private final ElementMatcher<? super TypeDescription> matcher;

    /**
     * Creates a renaming plugin for a given regular expression and replacement that applies to all types.
     *
     * @param pattern     The pattern to consider.
     * @param replacement The replacement to apply if the supplied pattern is matched.
     */
    public RenamingPlugin(String pattern, String replacement) {
        this(new Renaming.ForPattern(Pattern.compile(pattern), replacement));
    }

    /**
     * Creates a renaming plugin for a given regular expression and replacement that applies to all types that start with a given prefix.
     *
     * @param pattern     The pattern to consider.
     * @param replacement The replacement to apply if the supplied pattern is matched.
     * @param prefix      The prefix for types to consider for renaming.
     */
    public RenamingPlugin(String pattern, String replacement, String prefix) {
        this(new Renaming.ForPattern(Pattern.compile(pattern), replacement), nameStartsWith(prefix));
    }

    /**
     * Creates a renaming plugin for the given renaming that applies to all types.
     *
     * @param renaming The renaming to apply.
     */
    public RenamingPlugin(Renaming renaming) {
        this(renaming, any());
    }

    /**
     * Creates a renaming plugin for the given renaming and type matcher.
     *
     * @param renaming The renaming to apply.
     * @param matcher  A matcher that determines what types to consider for renaming.
     */
    public RenamingPlugin(Renaming renaming, ElementMatcher<? super TypeDescription> matcher) {
        this.renaming = renaming;
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        return builder.visit(this);
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(TypeDescription target) {
        return matcher.matches(target);
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        /* do nothing */
    }

    /**
     * {@inheritDoc}
     */
    public ClassVisitor wrap(TypeDescription instrumentedType,
                             ClassVisitor classVisitor,
                             Implementation.Context implementationContext,
                             TypePool typePool,
                             FieldList<FieldDescription.InDefinedShape> fields,
                             MethodList<?> methods,
                             int writerFlags,
                             int readerFlags) {
        return new ClassRemapper(classVisitor, new RenamingRemapper(renaming));
    }

    /**
     * A renaming function tho transform a type's binary name.
     */
    public interface Renaming {

        /**
         * Applies a renaming.
         *
         * @param name The previous name.
         * @return The former name.
         */
        String apply(String name);

        /**
         * A non-operational renaming.
         */
        enum NoOp implements Renaming {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public String apply(String name) {
                return name;
            }
        }

        /**
         * A renaming that renames types by a given regular expression.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForPattern implements Renaming {

            /**
             * The regular expression to use.
             */
            private final Pattern pattern;

            /**
             * The replacement to apply.
             */
            private final String replacement;

            /**
             * Creates a new renaming for a regular expression.
             *
             * @param pattern     The regular expression to use.
             * @param replacement The replacement to apply.
             */
            public ForPattern(Pattern pattern, String replacement) {
                this.pattern = pattern;
                this.replacement = replacement;
            }

            /**
             * {@inheritDoc}
             */
            public String apply(String name) {
                Matcher matcher = pattern.matcher(name);
                if (matcher.find()) {
                    StringBuffer buffer = new StringBuffer();
                    do {
                        matcher.appendReplacement(buffer, replacement);
                    } while (matcher.find());
                    return matcher.appendTail(buffer).toString();
                } else {
                    return name;
                }
            }
        }

        /**
         * A compound renaming.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Compound implements Renaming {

            /**
             * The renamings to apply.
             */
            private final List<Renaming> renamings;

            /**
             * Creates a new compound renaming.
             *
             * @param renaming The renaming to apply.
             */
            public Compound(Renaming... renaming) {
                this(Arrays.asList(renaming));
            }

            /**
             * Creates a new compound renaming.
             *
             * @param renamings The renamings to apply.
             */
            public Compound(List<? extends Renaming> renamings) {
                this.renamings = new ArrayList<Renaming>(renamings.size());
                for (Renaming remapping : renamings) {
                    if (remapping instanceof Compound) {
                        this.renamings.addAll(((Compound) remapping).renamings);
                    } else if (!(remapping instanceof NoOp)) {
                        this.renamings.add(remapping);
                    }
                }
            }

            /**
             * {@inheritDoc}
             */
            public String apply(String name) {
                for (Renaming remapping : renamings) {
                    name = remapping.apply(name);
                }
                return name;
            }
        }
    }

    /**
     * An ASM {@link Remapper} to apply renamings.
     */
    protected static class RenamingRemapper extends Remapper {

        /**
         * The renaming to apply.
         */
        private final Renaming renaming;

        /**
         * A cache of previously applied renamings.
         */
        private final Map<String, String> cache = new HashMap<String, String>();

        /**
         * Creates a new renaming remapper.
         * @param renaming The renaming to apply.
         */
        protected RenamingRemapper(Renaming renaming) {
            this.renaming = renaming;
        }

        @Override
        public String map(String internalName) {
            String renamed = cache.get(internalName);
            if (renamed != null) {
                return renamed;
            }
            renamed = renaming.apply(internalName.replace('/', '.')).replace('.', '/');
            cache.put(internalName, renamed);
            return renamed;
        }
    }
}
