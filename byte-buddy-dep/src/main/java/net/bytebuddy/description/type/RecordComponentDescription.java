package net.bytebuddy.description.type;

import net.bytebuddy.description.DeclaredByType;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.method.MethodDescription;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;


public interface RecordComponentDescription extends DeclaredByType, NamedElement, AnnotationSource {

    TypeDescription.Generic getType();

    MethodDescription.InDefinedShape getAccessor();

    abstract class AbstractBase implements RecordComponentDescription {

        @Override
        public int hashCode() {
            return getActualName().hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (!(other instanceof RecordComponentDescription)) {
                return false;
            }
            RecordComponentDescription recordComponentDescription = (RecordComponentDescription) other;
            return getActualName().equals(recordComponentDescription.getActualName());
        }

        @Override
        public String toString() {
            return getType().getTypeName() + " " + getActualName();
        }
    }

    class ForLoadedRecordComponent extends AbstractBase {

        protected static final Dispatcher DISPATCHER = AccessController.doPrivileged(Dispatcher.CreationAction.INSTANCE);

        private final AnnotatedElement recordComponent;

        protected ForLoadedRecordComponent(AnnotatedElement recordComponent) {
            this.recordComponent = recordComponent;
        }

        public static RecordComponentDescription of(Object recordComponent) {
            if (!DISPATCHER.isInstance(recordComponent)) {
                throw new IllegalArgumentException("Not a record component: " + recordComponent);
            }
            return new ForLoadedRecordComponent((AnnotatedElement) recordComponent);
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription.Generic getType() {
            return new TypeDescription.Generic.LazyProjection.OfRecordComponent(recordComponent);
        }

        /**
         * {@inheritDoc}
         */
        public MethodDescription.InDefinedShape getAccessor() {
            return new MethodDescription.ForLoadedMethod(DISPATCHER.getAccessor(recordComponent));
        }

        /**
         * {@inheritDoc}
         */
        public TypeDefinition getDeclaringType() {
            return TypeDescription.ForLoadedType.of(DISPATCHER.getDeclaringType(recordComponent));
        }

        /**
         * {@inheritDoc}
         */
        public String getActualName() {
            return DISPATCHER.getName(recordComponent);
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.ForLoadedAnnotations(recordComponent.getDeclaredAnnotations());
        }

        protected interface Dispatcher {

            boolean isInstance(Object instance);

            Object[] getRecordComponents(Class<?> type);

            String getName(Object recordComponent);

            Class<?> getDeclaringType(Object recordComponent);

            Method getAccessor(Object recordComponent);

            Class<?> getType(Object recordComponent);

            Type getGenericType(Object recordComponent);

            AnnotatedElement getAnnotatedType(Object recordComponent);

            enum CreationAction implements PrivilegedAction<Dispatcher> {

                INSTANCE;

                @Override
                public Dispatcher run() {
                    try {
                        Class<?> recordComponent = Class.forName("java.lang.reflect.RecordComponent");
                        return new ForJava14CapableVm(recordComponent,
                                Class.class.getMethod("getRecordComponents"),
                                recordComponent.getMethod("getName"),
                                recordComponent.getMethod("getDeclaringRecord"),
                                recordComponent.getMethod("getAccessor"),
                                recordComponent.getMethod("getType"),
                                recordComponent.getMethod("getGenericType"),
                                recordComponent.getMethod("getAnnotatedType"));
                    } catch (ClassNotFoundException ignored) {
                        return ForLegacyVm.INSTANCE;
                    } catch (NoSuchMethodException ignored) {
                        return ForLegacyVm.INSTANCE;
                    }
                }
            }

            enum ForLegacyVm implements Dispatcher {

                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public boolean isInstance(Object instance) {
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                public Object[] getRecordComponents(Class<?> type) {
                    return null;
                }


                /**
                 * {@inheritDoc}
                 */
                public String getName(Object recordComponent) {
                    throw new IllegalStateException("The current VM does not support record components");
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> getDeclaringType(Object recordComponent) {
                    throw new IllegalStateException("The current VM does not support record components");
                }

                /**
                 * {@inheritDoc}
                 */
                public Method getAccessor(Object recordComponent) {
                    throw new IllegalStateException("The current VM does not support record components");
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> getType(Object recordComponent) {
                    throw new IllegalStateException("The current VM does not support record components");
                }

                /**
                 * {@inheritDoc}
                 */
                public Type getGenericType(Object recordComponent) {
                    throw new IllegalStateException("The current VM does not support record components");
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotatedElement getAnnotatedType(Object recordComponent) {
                    throw new IllegalStateException("The current VM does not support record components");
                }
            }

            class ForJava14CapableVm implements Dispatcher {

                private final Class<?> recordComponent;

                private final Method getRecordComponents;

                private final Method getName;

                private final Method getDeclaringType;

                private final Method getAccessor;

                private final Method getType;

                private final Method getGenericType;

                private final Method getAnnotatedType;

                protected ForJava14CapableVm(Class<?> recordComponent,
                                             Method getRecordComponents,
                                             Method getName,
                                             Method getDeclaringType,
                                             Method getAccessor,
                                             Method getType,
                                             Method getGenericType,
                                             Method getAnnotatedType) {
                    this.recordComponent = recordComponent;
                    this.getRecordComponents = getRecordComponents;
                    this.getName = getName;
                    this.getDeclaringType = getDeclaringType;
                    this.getAccessor = getAccessor;
                    this.getType = getType;
                    this.getGenericType = getGenericType;
                    this.getAnnotatedType = getAnnotatedType;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isInstance(Object instance) {
                    return recordComponent.isInstance(instance);
                }

                /**
                 * {@inheritDoc}
                 */
                public Object[] getRecordComponents(Class<?> type) {
                    try {
                        return (Object[]) getRecordComponents.invoke(type);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.Class#getRecordComponents", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.Class#getRecordComponents", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public String getName(Object recordComponent) {
                    try {
                        return (String) getName.invoke(recordComponent);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflection.RecordComponent#getName", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflection.RecordComponent#getName", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> getDeclaringType(Object recordComponent) {
                    try {
                        return (Class<?>) getDeclaringType.invoke(recordComponent);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflection.RecordComponent#getDeclaringType", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflection.RecordComponent#getDeclaringType", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Method getAccessor(Object recordComponent) {
                    try {
                        return (Method) getAccessor.invoke(recordComponent);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflection.RecordComponent#getAccessor", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflection.RecordComponent#getAccessor", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> getType(Object recordComponent) {
                    try {
                        return (Class<?>) getType.invoke(recordComponent);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflection.RecordComponent#getType", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflection.RecordComponent#getType", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Type getGenericType(Object recordComponent) {
                    try {
                        return (Type) getGenericType.invoke(recordComponent);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflection.RecordComponent#getGenericType", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflection.RecordComponent#getGenericType", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotatedElement getAnnotatedType(Object recordComponent) {
                    try {
                        return (AnnotatedElement) getAnnotatedType.invoke(recordComponent);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflection.RecordComponent#getAnnotatedType", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflection.RecordComponent#getAnnotatedType", exception.getCause());
                    }
                }
            }
        }
    }
}
