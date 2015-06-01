Byte Buddy release notes
------------------------

### 02. Mai 2014: version 0.1

First general release.

### 16. June 2014: version 0.2

Added several bug fixes for existing features. Beyond that the following features were added or changed:

 - Changed the semantics of the `@SuperCall` to be only bindable, if a super method can be invoked. Before, an
   exception was thrown if only a non-existent or abstract super method was found.
 - Added features for the interaction with Java 8 default methods. Refactored method lookup to extract invokable
   default methods.
 - Refactored the invocation of super methods to be created by an `Instrumentation.Target`. For a future release,
   this hopefully allows for class redefinitions using today's API for creating subclasses.
 - Upgraded to ASM 5.0.3.

### 20. June 2014: version 0.2.1

Added proper support for defining class initializers. Added support for field caching from method instrumentations,
mainly for allowing the reuse of `Method` instances for the `@Origin` annotation and the `InvocationHandlerAdapter`.

### 15. September 2014: version 0.3

- Added basic support for Java 7 types `MethodHandle` and `MethodType` which are available from Java 7 for injection.
- Added support for type redefinition and type rebasing.
- Added support for accessing a JVM's HotSwap features and a Java agent.
- Added latent a child-first `ClassLoadingStrategy` and manifest versions of the `WRAPPER` and `CHILD_FIRST` default
  class loading strategies.
  
### 19. September 2014: version 0.3.1

- Added support for optionally specifying a `ProtectionDomain` for the built-in `ClassLoadingStrategy` implementations.
- Fixed a bug in the resolution of resources of the `ByteArrayClassLoader` and its child-first implementation.

### 18. November 2014: version 0.4

- Extended `Instrumentation.Context` to support field accessors.
- Added the `TypePool` abstraction and added a default implementation.
- Refactored annotations to have an intermediate form as `AnnotationDescription` which does not need to 
  represent loaded values.
- Refactored several built-in `Instrumentation`, among others, all implementations now support `TypeDescription` 
  in addition to loaded `Class` as their arguments
- Added several annotations that apply to the `MethodDelegation`.

### 26. November 2014: version 0.4.1

- Refactored the implementation of the `VoidAwareAssigner` which would otherwise cause unexpected behavior in its 
  default state.
- Added a missing boxing instruction to the `InvocationHandlerAdapter`.

### 3. December 2014: version 0.5

- Added the `DeclaringTypeResolver` as a component in the default chain which selects the most specific method out
  of two. This is mainly meant to avoid the accidental matching of the methods that are declared by the `Object` type.
- Added `TypeInitializer`s in order to allow `Instrumentation`s to define type initializer blocks.
- Replaced the `MethodMatcher` API with the `ElementMatcher` API which allows for a more sophisticated matching DSL.
- Added a `ClassLoadingStrategy` for Android in its own module.
- Introduced an `AgentBuilder` API and implementation.

### 5. December 2014: version 0.5.1

Added the `andThen` method to the `SuperMethodCall` instrumentation in order to allow for a more convenient 
constructor interception where a hard-coded super method call is required by the Java verifier.

### 18. January 2015: version 0.5.2

- Fixed a bug where interface generation would result in a `NullPointerException`.
- Added additional `ElementMatcher`s that allow to identify class loaders.

### 24. February 2015: version 0.5.3

- Changed the `SuperMethodCall` instrumentation to fall back to a default method call if required. A different
  behavior was found to surprise users and would introduce subtle bugs in user code as the super method instrumentation
  would always work with subclassing due to Java super method call semantics.
- Added a `MethodCall` instrumentation that allows hard-coding a method call.
- Added an `InvokeDynamic` instrumentation that allows runtime dispatching by bootstrap methods.
- Fixed the default `TypePool` to retain generic signatures in order to avoid that agents delete such signatures.
- Fixed a bug in all of the the default `ConstructorStrategy` that effectively prevented intercepting of constructors.

### 15. March 2015: version 0.5.4

- Fixed missing retention of method annotations of instrumented types.
- Allowed dynamic lookup of methods for the `MethodCall` instrumentation.

### 20. March 2015: version 0.5.5

- Retrofitted method parameters to be represented by `ParameterDescription`s and added possibility to extract names
  and modifiers for these parameters, either by using the Java 8 API (if available) or by reading this information
  from the underlying class file.
- Fixed a `NullPointerException` being thrown due to accidental return of a `null` value from a method.

### 21. March 2015: version 0.5.6

- Added possibility to write parameter meta information to created classes if it is fully available for a method.

### 15. April 2015: version 0.6

- Renamed the `Instrumentation` interface to `Implementation` to avoid naming conflicts with Java types.
- Renamed the `Field` annotation to `FieldProxy` to avoid naming conflicts with Java types.
- Refactored package structure to make the implementation more readable.
- Added possibility to define annotation default values.
- Avoid creation of an auxiliary placeholder type for method rebasements if it is not required.
- Avoid rebasing of methods if they are not instrumented.
- Reimplemented `TypeWriter`, `MethodRegistry` and other supporting infrastructure to make  the code simpler.
- Refactored testing that is related to the previous infrastructure.

### 21. April 2015: version 0.6.1

- Added `AnnotationDescription.Builder` to allow easy definition of annotation values without loading any values.
- Added possibility to define enumerations at runtime.
- Added possibility to dynamically read enumerations for the `MethodCall` and `InvokeDynamic` implementations.
- Further API clean-up.

### 23. April 2015: version 0.6.2

- Added additional class validation such that it becomes impossible to define members on classes that do not fit
  the class's structure, i.e. default methods on Java interfaces in version seven.
- Added default `Assigner` singleton.

### 29. April 2015: version 0.6.3

- Made the `TypeDescription.ForLoadedType` class loader agnostic. Before, a class that was loaded by multiple class
  loaders would have been considered inequal what is not true for the byte code level.

### 6. Mai 2015: version 0.6.4

- Extended public API to accept more general argument types when appropriate.
- Extended `@Origin` annotation to allow for accepting modifiers.

### 7. Mai 2015: version 0.6.5

Extended public API with convenience methods.

### 28. Mai 2015: version 0.6.6

Fixed error in resolution of the `TargetType` pseudo-variable when used as component type of an array.

### 1. June 2015: version 0.6.7

- Upgraded ASM dependency to 0.5.4.
- Fixed OSGi headers in all relevant artifacts.

*Warning*: The *byte-buddy* artifact is accidentally deployed with a defect pom file which does not exclude the shaded resources.

### 1. June 2015: version 0.6.8

Corrected incorrect deployment of version 0.6.7 which does not use a dependency reduced POM for the *byte-buddy* module.

### Current snapshot

Currently, there are no changes.
