Byte Buddy release notes
------------------------

### 24. June 2016: version 1.4.5

- Added `InstallationStrategy` to `AgentBuilder` that allows customization of error handling.
- Added *chunked* redefinition and retransformation strategies.

### 23. June 2016: version 1.4.4

- Added `net.bytebuddy` qualifier when logging.
- Added `net.bytebuddy.dump` system property for specifing a location for writing all created class files.

### 17. June 2016: version 1.4.3

- Fixed bug in `MultipleParentClassLoader` where class loaders were no longer filtered properly.
- Added support for major.minor version 53 (Java 9).
- Made `DescriptionStrategy` customizable.

### 16. June 2016: version 1.4.2

- Changed storage order of return values in `Advice` methods to avoid polluting the local variable array when dealing with nested exception handlers.
- Added caching `ElementMatcher` as a wrapping matcher.
- Exclude Byte Buddy types by default from an `AgentBuilder`.
- Added `DescriptionStrategy` that allows not using reflection in case that a class references non-available classes.

### 7. June 2016: version 1.4.1

- Fixed validation by `MethodCall` instrumentation for number of arguments provided to a method.
- Added further support for module system.
- Allow automatic adding of read-edges to specified classes/modules when instrumenting module classes.
- Implicitly skip methods without byte code from advice component.

### 2. June 2016: version 1.4.0

- Added initial support for Jigsaw modules.
- Adjusted agent builder API to expose modules of instrumented classes.
- Added additional matchers.
- Simplified `BinaryLocator` and changed its name to `TypeLocator`.

### 30. May 2016: version 1.3.20

- Fixed `MultipleParentClassLoader` to support usage as being a parent itself.
- Fixed default ignore matcher for `AgentBuilder` to ignore synthetic types.

### 29. April 2016: version 1.3.19

- Added convenience method to `MethodCall` to add all arguments of the instrumented method.
- Added `optional` attribute to `Advice.This`.

### 25. April 2016: version 1.3.18

- The Owner type of a parameterized type created by a `TypePool` is no longer parameterized for a static inner type.
- The receiver type of a executingTransformer is no longer considered parameterized for a static inner type.

### 23. April 2016: version 1.3.17

- Removed overvalidation of default values for non-static fields.

### 21. April 2016: version 1.3.16

- Better support for Java 1 to Java 4 by automatically resolving type references from a type pool to `forName` lookups.
- Better support for dealing with package-private types by doing the same for invisible types.
- Simplified `MethodHandle` and `MethodType` handling as `JavaInstance`.

### 19. April 2016: version 1.3.15

- Extended the `AgentBuilder` to allow for transformations that apply fall-through semantics, i.e. work as a decorator.
- Added map-based `BinaryLocator`.

### 19. April 2016: version 1.3.14

- Only add frames in `Advice` components if class file version is above 1.5.
- Allow to specify exception type for exit advice to be named. **This implies a default change** where exit advice is no longer invoked by default when an exception is thrown from the instrumented method.
- Added possibility to assign a value to a `@BoxedReturn` value to change the returned value.

### 16. April 2016: version 1.3.13

- Extended the `Advice` component storing serializable values that cannot be represented in the constant pool as encodings in the constant pool.
- Added support for non-inlined `Advice` method.
- Mask modifiers of ASM to not longer leak internal flags beyond the second byte.
- Added support for suppressing an exception of the instrumented method from within exit advice.

### 13. April 2016: version 1.3.12

- Fixed error during computation of frames for the `Advice` computation.
- Avoid reusing labels during the computations of exception tables of the `Advice` component.

### 12. April 2016: version 1.3.11

- Byte Buddy `Advice` now appends handlers to an existing exception handler instead of prepending them. Before, existing exception handlers were shadowed when applying suppression or exit advice on an exception.
- Added additional annotations for `Advice` such as `@Advice.BoxedReturn` and `@Advice.BoxedArguments` for more generic advice. Added possibility to write to fields from advice.
- Added mechanism for adding custom annotations to `Advice` that map compile-time constants.
- Implemented a canonical binder for adding custom compile-time constants to a `MethodDelegation` mapping.

### 8. April 2016: version 1.3.10

- Fixed another bug during frame translation of the `Advice` component when suppression were not catched for an exit advice.
- Improved unit tests to automatically build Byte Buddy with Java 7 and Java 8 byte code targets in integration.

### 7. April 2016: version 1.3.9

- Optimized method size for `Advice` when exception is not catched.
- Improved convenience method `disableClassFormatChanges` for `AgentBuilder`.

### 6. April 2016: version 1.3.8

- Fixed frame computation for the `Advice`.
- Optimized frame computation to emitt frames of the minimal, possible size when using `Advice`.
- Only add exit `Advice` once to reduce amound of added bytes to avoid size explosion when a method supplied several exits.
- Optimized `Advice` injection to only add advice infrastucture if entry/exit advice is supplied.
- Optimized exception handling infrastructure for exit `Advice` to only be applied when exceptions are catched.
- Added mapping for the *IINC* instruction which was missing from before.
- Added possibility to propagate AMS reader and writer flags for `AsmVisitorWrapper`.
- Made `Advice` method parser respect ASM reader flags for expanding frames.

### 3. April 2016: version 1.3.7

- Fixed bug when returning from an `Advice` exit method without return value and accessing `@Advice.Thrown`.
- Added additional annotations for advice `@Advice.Ignored` and `@Advice.Origin`.
- Implemented frame translator for `Advice` method to reuse existing frame information instead of recomputing it.

### 1. April 2016: version 1.3.6

- Implemented universal `FieldLocator`.
- Extended `AgentBuilder` API to allow for more flexible matching and ignoring types.

### 18. Match 2016: version 1.3.5

- Added `Advice.FieldValue` annotation for reading fields from advice.

### 13. March 2016: version 1.3.4

- Added support for new Java 9 version scheme.

### 10. March 2016: version 1.3.3

- Added hierarchical notation to default `TypePool`.

### 10. March 2016: version 1.3.2

- Added possibility to suppress `Throwable` from advice methods when using the `Advice` instrumentation.
 
### 9. March 2016: version 1.3.1

- Added possibility to use contravariant parameters within the `Advice` adapter for ASM.

### 8. March 2016: version 1.3.0

- Added `Advice` adapter for ASM.
- Fixed `AsmVisitorWrapper` registration to be stacked instead of replacing a previous value.
- Added validation for setting field default values what can only be done for `static` fields. Clarified javadoc.
- Fixed attach functionality to work properly on IBM's J9.

### 22. February 2016: version 1.2.3

- Fixed return type resolution for overloaded bridge method.

### 16. February 2016: version 1.2.2

- Fixed redefinition strategy for `AgentBuilder` where transformations were applied twice.
- Added `ClassLoader` as a third argument for the `AgentBuilder.Transformer`.

### 6. February 2016: version 1.2.1

- Added validation for receiver types.
- Set receiver types to be implicit when extracting constructors of a super type.

### 5. February 2016: version 1.2.0

- Added support for receiver type retention during type redefinition and rebasement.
- Added support for receiver type definitions.

### 5. February 2016: version 1.1.1

- Fixed interface assertion of the custom binder types to accept default methods.
- Improved documentation.
 
### 26. January 2016: version 1.1.0

- Refactored `AgentBuilder` API to be more streamlined with the general API and improved documentation.
- Added possibility to instrument classes that implement lambda expressions.
- Added possibility to explicitly ignore types from an `AgentBuilder`. By default, synthetic types are ignored.
- Proper treatment of deprecation which is now written into the class file as part of the resolved modifier and filtered on reading.
- Added support for Java 9 APIs for process id retrieval.

### 21. January 2016: version 1.0.3

- Added support for Java 9 owner type annotations.
- Fixed bug in type builder validation that prohibited annotations on owner types for non-generic types.
- Added additional element matchers for matching an index parameter type.

### 20. January 2016: version 1.0.2

- Fixed resolution of type paths for inner classes.
- Added preliminary support for receiver types.
- Fixed resolution of type variables from a static context.

### 18. January 2016: version 1.0.1

- Refactored type variable bindings for generic super types: Always retain variables that are defined by methods.
- Retain type annotations that are defined on a `TargetType`.

### 15. January 2016: version 1.0.0

- Added support for type annotations.
- Refactored public API to support type annotations and parameter meta information.
- Several renamings in preparation of the 1.0.0 release.
- Refactored type representation to represent raw types as `TypeDescription`s. This allows for resolution of variables on these types as erasures rather than their unresolved form. Refactored naming of generic types to the common naming scheme with nested classes.
- Replaced generalized token representation to define tokens, type tokens and signature tokens.
- General API improvements and minor bug fixes.

### 4. January 2016: version 0.7.8

- Implemented all type lists of class file-rooted files to fallback to type erasures in case that the length of generic types and raw types does not match. This makes Byte Buddy more robust when dealing with illegally defined class files.
- Fixed rule on a default method's invokeability.
- Extended `MethodCall` implementation to include shortcuts for executing `Runnable` and `Callable` instances.
- Added `failSafe` matcher that returns `false` for types that throw exceptions during navigation.

### 14. December 2015: version 0.7.7

- Fixed type resolution for anonymously loaded classes by the `ClassReloadingStrategy`.
- Added additional `InitiailizationStrategy`s for self-injection where the new default strategy loads types that are independent of the instrumented type before completing the instrumentation. This way, the resolution does not fail for types that are accessed via reflection before initializing the types if a executingTransformer is rebased.

### 11. December 2015: version 0.7.6

- Fixed resolution of `@Origin` for constructors and added possibility to use the `Executable` type.
- Fixed name resolution of types loaded by anonymous class loading.
- Allowed alternative lookup for redefinitions to support types loaded by anonymous class loading.

### 7. December 2015: version 0.7.5

- Fixed generic type resolution optimization for proxies for `@Super`.

### 2. December 2015: version 0.7.4

- Added `TypePool` that returns precomputed `TypeDescription`s for given types.
- Fixed agent and nexus attachment and the corresponding value access.

### 30. November 2015: version 0.7.3

- Added visibility substitution for `@Super` when the instrumented type is instrumented to see changed state on a redefinition.
- Added patch for modifier information of inner classes on a redefinition.
- Added fallback for `Nexus` injection to attempt lookup of already loaded class if resource cannot be located.

### 26. November 2015: version 0.7.2

- Added `TypePool` that falls back to class loading if a class cannot be located.
- Added binary locator for agent builder that uses the above class pool and only parses the class file of the instrumented type.
- Added methods for reading inner classes of a `TypeDescription`.
- Fixed random naming based on random numbers to avoid signed numbers.
- Moved `Nexus` and `Installer` types to a package-level to avoid illegal outer and inner class references which could be resolved eagerly.
- Added validation for illegal constant pool entries.
- Added a `Premature` initialization strategy for optimistically loading auxiliary types.
- Added a `ClassVisitorWrapper` for translating Java class files prior to Java 5 to use explicit class loading rather than class pool constants.

### 16. November 2015: version 0.7.1

- Fixed injection order for types to avoid premature loading by dependent auxiliary types.
- Added additional `ClassFileLocator`s and refactored class file lookup to always use these locators.

### 11. November 2015: version 0.7

- Refactored injection strategy to always inject and load the instrumented type first to avoid premature loading by reference from auxiliary types.
- Refactored `AgentBuilder.Default` to delay auxiliary type injection until load time to avoid premature loading by reference from auxiliary types.
- Added API to add additional code to type initializers while building a type.
- Refactored agent `Nexus` to allow for multiple registrations of self initializers if multiple agents are registered via Byte Buddy.
- Fixed resolution of interface methods that were represented in the type hierarchy multiple times.
- Implemented custom ASM class writer to allow for frame computation via Byte Buddy's type pool when this is required by a user.
- Fallback to no allowing for instrumenting type initializers for rebased or redefined interfaces before Java 8.

### 28. October 2015: version 0.7 (release candidate 6)

- Refactored `AgentBuilder.Default` to delegate exceptions during redefinitions to listener instead of throwing them.
- Fixed bug where instrumented type would count to auxiliary types and trigger injection strategy.
- Fixed bug where interface types would resolve to a non-generic type signature.
- Added strategy to use redefinition or retransformation of the `Instrumentation` API when building agents.
- Added lazy facade to be used by agent builder to improve performance for name-based matchers.

### 15. October 2015: version 0.7 (release candidate 5)

- Fixed parser to suppress exceptions from generic signatures which are not supposed to be included in the class file if no array type is generic.
- Fixed class validator which did not allow `<clinit>` blocks in interface types.
- Added restriction to retransformation to not attempt a retransformation at all if no class should be retransformed.
- Added a factory for creating an `Implementation.Context` that is configurable. This way, it is possible to avoid a rebase of a type initializer which is not always possible.
- Added a possibility to specify for an `AgentBuilder` how it should redefine or rebase a class that is intercepted.

### 13. October 2015: version 0.7 (release candidate 4)

- Fixed naming strategy for fields that cache values which chose duplicate names.
- Fixed resolution of raw types within the type hierarchy which were represented as non-generic `TypeDescription` instances where type variables of members were not resolved.
- Added possibility to specify hints for `ClassReader` and `ClassWriter` instances.
- Fixed resolution for modifiers of members that are defined by Byte Buddy. Previously, Byte Buddy would sometimes attempt to define private synthetic methods on generated interfaces.
- Fixed assignability resolution for arrays.
- Fixed class file parser which would not recognize outer classes for version 1.3 byte code.

### 6. October 2015: version 0.7 (release candidate 3)

- Read `Nexus` instances of the Byte Buddy agents from the enclosing class loader rather than from the system class loader. This allows for their usage from OSGi environments and for user with other custom class loaders.
- Changed modifiers for accessor methods and rebased methods to be public when rebasing or accessing methods of a Java 8 interface. For interfaces, all modifiers must be public, even for such synthetic members.
- Support absolute path names for accessing class file resources of the `ByteArrayClassLoader`.
- Added random suffix to the names of rebased methods to avoid naming conflicts.

### 16. September 2015: version 0.7 (release candidate 2)

- Refactored runtime attachment of Java agents to support Java 9 and additional legacy VM (version 8-).
- Refactored `MethodGraph` to only represent virtual methods.
- Changed notion of visibility to not longer consider the declaring type as part of the visibility.
- Increased flexibility of defining proxy types for `@Super` and `@Default` annotations.
- Added directional `AmbigouityResolver`.
- Fixed detection of methods that can be rebased to not include methods that did not previously exist.

### 11. August 2015: version 0.7 (release candidate 1)

- Added support for generic types.
- Replaced `MethodLookupEngine` with `MethodGraph.Compiler` to provide a richer data structure.
- Added support for bridge methods (type and visibility bridges).
- Refactored the predefined `ElementMatcher`s to allow for matching generic types.
- Replaced the `ModifierResolver` with a more general `MethodTransformer`.

### 11. August 2015: version 0.6.15

- Added support for discovery and handling of Java 9 VMs.
- Fixed class loading for Android 5 (Lollipop) API.

### 20. July 2015: version 0.6.14

- Fixed resolution of ignored methods. Previously, additional ignored methods were not appended but added as an additional criteria for ignoring a method.

### 17. July 2015: version 0.6.13

- Fixed resolution of field accessors to not attempt reading of non-static fields from static methods.
- Fixed renaming strategy for type redefinitions to work around a constraint of ASM where stack map frames required to be expanded even though this was not strictly necessary.

### 10. July 2015: version 0.6.12

- Added API for altering a method's modifiers when intercepting it.
- Added API for allowing to filter default values when writing annotations.

### 22. June 2015: version 0.6.11

- Added additional `ClassFileLocator`s for locating jar files in folders and jar files.
- Added explicit check for invalid access of instance fields from static methods in field accessing interceptors.
- Added the `@StubValue` and `@FieldValue` annotations.

### 18. June 2015: version 0.6.10 (and 0.6.9)

- Corrected the resolution of a type's visibility to another type to determine if a method can be legally overridden.
- Previous version 0.6.9 contained another bug when attempting to fix this problem.

Corrected incorrect deployment of version 0.6.7 which does not use a dependency reduced POM for the *byte-buddy* module.

### 1. June 2015: version 0.6.8 (and 0.6.7)

- Upgraded ASM dependency to 5.0.4.
- Fixed OSGi headers in all relevant artifacts.

*Warning*: The *byte-buddy* artifact of version 0.6.7 is accidentally deployed with a defect POM file which does not exclude the shaded resources.

### 28. May 2015: version 0.6.6

- Fixed error in resolution of the `TargetType` pseudo-variable when used as component type of an array.

### 7. May 2015: version 0.6.5

- Extended public API with convenience methods.

### 6. May 2015: version 0.6.4

- Extended public API to accept more general argument types when appropriate.
- Extended `@Origin` annotation to allow for accepting modifiers.

### 29. April 2015: version 0.6.3

- Made the `TypeDescription.ForLoadedType` class loader agnostic. Before, a class that was loaded by multiple class
  loaders would have been considered inequal what is not true for the byte code level.
  
### 23. April 2015: version 0.6.2

- Added additional class validation such that it becomes impossible to define members on classes that do not fit
  the class's structure, i.e. default methods on Java interfaces in version seven.
- Added default `Assigner` singleton.

### 21. April 2015: version 0.6.1

- Added `AnnotationDescription.Builder` to allow easy definition of annotation values without loading any values.
- Added possibility to define enumerations at runtime.
- Added possibility to dynamically read enumerations for the `MethodCall` and `InvokeDynamic` implementations.
- Further API clean-up.

### 15. April 2015: version 0.6

- Renamed the `Instrumentation` interface to `Implementation` to avoid naming conflicts with Java types.
- Renamed the `Field` annotation to `FieldProxy` to avoid naming conflicts with Java types.
- Refactored package structure to make the implementation more readable.
- Added possibility to define annotation default values.
- Avoid creation of an auxiliary placeholder type for method rebasements if it is not required.
- Avoid rebasing of methods if they are not instrumented.
- Reimplemented `TypeWriter`, `MethodRegistry` and other supporting infrastructure to make  the code simpler.
- Refactored testing that is related to the previous infrastructure.

### 21. March 2015: version 0.5.6

- Added possibility to write parameter meta information to created classes if it is fully available for a method.

### 20. March 2015: version 0.5.5

- Retrofitted method parameters to be represented by `ParameterDescription`s and added possibility to extract names
  and modifiers for these parameters, either by using the Java 8 API (if available) or by reading this information
  from the underlying class file.
- Fixed a `NullPointerException` being thrown due to accidental return of a `null` value from a method.

### 15. March 2015: version 0.5.4

- Fixed missing retention of method annotations of instrumented types.
- Allowed dynamic lookup of methods for the `MethodCall` instrumentation.

### 24. February 2015: version 0.5.3

- Changed the `SuperMethodCall` instrumentation to fall back to a default method call if required. A different
  behavior was found to surprise users and would introduce subtle bugs in user code as the super method instrumentation
  would always work with subclassing due to Java super method call semantics.
- Added a `MethodCall` instrumentation that allows hard-coding a method call.
- Added an `InvokeDynamic` instrumentation that allows runtime dispatching by bootstrap methods.
- Fixed the default `TypePool` to retain generic signatures in order to avoid that agents delete such signatures.
- Fixed a bug in all of the the default `ConstructorStrategy` that effectively prevented intercepting of constructors.

### 18. January 2015: version 0.5.2

- Fixed a bug where interface generation would result in a `NullPointerException`.
- Added additional `ElementMatcher`s that allow to identify class loaders.

### 5. December 2014: version 0.5.1

Added the `andThen` method to the `SuperMethodCall` instrumentation in order to allow for a more convenient 
executingTransformer interception where a hard-coded super method call is required by the Java verifier.

### 3. December 2014: version 0.5

- Added the `DeclaringTypeResolver` as a component in the default chain which selects the most specific method out
  of two. This is mainly meant to avoid the accidental matching of the methods that are declared by the `Object` type.
- Added `TypeInitializer`s in order to allow `Instrumentation`s to define type initializer blocks.
- Replaced the `MethodMatcher` API with the `ElementMatcher` API which allows for a more sophisticated matching DSL.
- Added a `ClassLoadingStrategy` for Android in its own module.
- Introduced an `AgentBuilder` API and implementation.

### 26. November 2014: version 0.4.1

- Refactored the implementation of the `VoidAwareAssigner` which would otherwise cause unexpected behavior in its 
  default state.
- Added a missing boxing instruction to the `InvocationHandlerAdapter`.

### 18. November 2014: version 0.4

- Extended `Instrumentation.Context` to support field accessors.
- Added the `TypePool` abstraction and added a default implementation.
- Refactored annotations to have an intermediate form as `AnnotationDescription` which does not need to 
  represent loaded values.
- Refactored several built-in `Instrumentation`, among others, all implementations now support `TypeDescription` 
  in addition to loaded `Class` as their arguments
- Added several annotations that apply to the `MethodDelegation`.

### 19. September 2014: version 0.3.1

- Added support for optionally specifying a `ProtectionDomain` for the built-in `ClassLoadingStrategy` implementations.
- Fixed a bug in the resolution of resources of the `ByteArrayClassLoader` and its child-first implementation.

### 15. September 2014: version 0.3

- Added basic support for Java 7 types `MethodHandle` and `MethodType` which are available from Java 7 for injection.
- Added support for type redefinition and type rebasing.
- Added support for accessing a JVM's HotSwap features and a Java agent.
- Added latent a child-first `ClassLoadingStrategy` and manifest versions of the `WRAPPER` and `CHILD_FIRST` default
  class loading strategies.
  
### 20. June 2014: version 0.2.1

- Added proper support for defining class initializers. Added support for field caching from method instrumentations,
mainly for allowing the reuse of `Method` instances for the `@Origin` annotation and the `InvocationHandlerAdapter`.
  
### 16. June 2014: version 0.2

 - Changed the semantics of the `@SuperCall` to be only bindable, if a super method can be invoked. Before, an
   exception was thrown if only a non-existent or abstract super method was found.
 - Added features for the interaction with Java 8 default methods. Refactored method lookup to extract invokable
   default methods.
 - Refactored the invocation of super methods to be created by an `Instrumentation.Target`. For a future release,
   this hopefully allows for class redefinitions using today's API for creating subclasses.
 - Upgraded to ASM 5.0.3.
 
### 02. May 2014: version 0.1

- First general release.
