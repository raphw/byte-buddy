Byte Buddy release notes
------------------------

### 1. November 2020: version 1.10.18

- Fixes descriptor used for method handle constant of field.
- Fixes assignability check for varargs.
- Allow using static interface methods for bootstraping.
- Allow providing null to setsValue for field assignment.
- Cleans up providing of constant arguments as type description, enum or constant.
- Support getPackage in legacy class loaders.
- Allow method call by matcher on private method of instrumented type.

### 7. October 2020: version 1.10.17

- Adjust Gradle plugin to properly consider registration order with the Java plugin.
- Correct task adjustment when using Byte Buddy in a multimodule project.

### 23. September 2020: version 1.10.16

- Update to ASM 9 for full support of sealed types. 

### 19. September 2020: version 1.10.15

- Rewrite of Gradle plugin, including support for incremental builds.
- Fix `MethodCall` bug when loading arguments from array.
- Mark rebased methods `private final` as required by the JVM when using a native method preifx.
- Fix stack excess monitoring when using advice to discover excess stack values correctly to avoid verifier error.

### 31. July 2020: version 1.10.14

- Fix build config to include Eclipse e2e file.
- Allow for not printing a warning if no file is transformed in build plugin.
- Fix invokability check in `MethodCall` validation.
- Avoid premature validation of `InstrumentType`'s method modifiers.
- Use type cache by default when using loaded type class pool since class lookup showed to be rather expensive.

### 27. June 2020: version 1.10.13

- Add possibility to filter class loaders before attempting to load a class using the `AgentBuilder`'s resubmission feature.
- Add `nameOf` matcher for more efficient string matching based on a hash set.

### 18. June 2020: version 1.10.12

- Experimental support for Java 16.
- Support all constant pool constant types in all APIs.
- Adjust methods for bootstrap arguments to allow types of *constantdynamic* constants.
- Correctly resolve handle type for method handles on private constructors.
- Fix stack size computation for minimal methods in `Advice`.

### 4. June 2020: version 1.10.11

- Emit full frame after super or auxiliary constructor call in constructors if no full frame was already issued within this constructor.
- Support methods that start with a stack map frame before any code.
- Pop array after `@AllArguments` parameter.
- Fix source inclusion for ASM commons.
- Avoid resolution of detached types when replacing target types in generic arrays on members of instrumented types.
- Fix validation of member substitution.
- Include post processor for `Advice`.

### 29. April 2020: version 1.10.10

- Update ASM to 8.0.1
- Close Dex files in Android class loader.
- Add abstraction for advice dispatcher to allow for use of invokedynamic.
- Properly handle incompatible type changes in parsed annotations.
- Add support for Java records.

### 29. March 2020: version 1.10.9

- Add validation for interface method modifiers.
- Correct discovery of MacOs temp directory for Byte Buddy Agent `VirtualMachine`.
- Add parallel processor for Byte Buddy build engine.
- Add preprocessor for Byte Buddy build engine.
- Explicitly load Java's `Module` from boot loader to avoid loading pseudo compiler target bundled with NetBeans.
- Add convenience method for creating lookup-based class loading strategy with fallback to Unsafe for Java 8 and older.
- Add caching for method, field and parameter description hashCode methods.

### 16. February 2020: version 1.10.8

- Adjust use of types of the `java.instrument` module to avoid errors if the module is not present on a JVM.

### 21. January 2020: version 1.10.7

- Correct discovery of old J9 VMs.
- Correct invocation of `AgentBuilder.Listener` during retransformation.
- Allow forbidding self-attachment using own artifact.
- Add possibility to patch class file transformers.
- Fix equality check for float and double primitives.
- Add guards for annotation API to handle buggy reflection API with mandated parameters.
- Update ASM.

### 19. December 2019: version 1.10.6

- Add experimental support for Java 15.
- Allow `AndroidClassLoadingStrategy` to work with newer API level.

### 11. December 2019: version 1.10.5

- Fixes Gradle plugin release to include correct dependency.
- Fixes source jar release for shaded *byte-buddy* artifact.

### 28. November 2019: version 1.10.4

- Throw exception upon illegal creation of entry-only advice with local parameters to avoid verify error.
- Remove escaping for execution path on Windows with spaces for Byte Buddy agent.
- Fix J9 detection for older IBM-released versions of J9 in Byte Buddy agent.

### 8. November 2019: version 1.10.3

- Allow overriding the name of the native library for Windows attach emulation.
- Use correct type pool in build plugin engine for decorators.
- Fix attach emulation for OpenJ9 on MacOS.

### 16. October 2019: version 1.10.2

- Upgrade ASM to version 7.2.
- Improve class file version detection for class files.
- Check argument length of Windows attach emulation.

### 9. August 2019: version 1.10.1

- Extend `VirtualMachine` API emulation.
- Reopen socket for POSIX-HotSpot connections after each command being sent to avoid broken pipe errors.
- Only use JNA API that is available in JNA versions 4 and 5 for better compatibility.
- Include correct license information in artifacts.
- Add injection API based on `jdk.internal.misc.Unsafe` to support agents on platforms that do not include *jdk.unsupported*.
- Add `AgentBuilder.InjectionStrategy` to allow usage of internal injection API.
- Open package in `AgentBuilder` if from and to edges are added.

### 3. August 2019: version 1.10.0

- Add API for loading native agents from Byte Buddy agent.
- Extend `VirtualMachine` API to include other methods.
- Fix error handling in `VirtualMachine` API.
- Fix temporary folder resolution for `VirtualMachine` API.
- Add API for `MemberAttributeExtension`.
- Rework of `AnnotationDescription` API to emulate JVM error handling for incorrect or inconsistent values.
- Add generic type-aware `Assigner`.
- Fix method handle-based injector for Java 14.

### 27. July 2019: version 1.9.16

- Add support for attach emulation on Solaris.
- Fix JNA signatures for attach emulation on POSIX.
- Add standard call conventions for Windows JNA calls.

### 21. July 2019: version 1.9.15

- Add emulated attach mechanism for HotSpot on Windows and for OpenJ9/J9 on POSIX and Windows (if JNA is present).
- Reimplement POSIX attach mechanism for HotSpot to use JNA (if present).

### 8. July 2019: version 1.9.14

- Add Java 14 compatibility.
- Refactor emulated attach mechanism and use JNA in order to prepare supporting other mechanisms in the future.
- Reinterrupt threads if interruption exceptions are catched in threads not owned by Byte Buddy.
- Refactor class file dumping. 
- Publish Gradle plugin to Gradle plugin repository.

### 24. May 2019: version 1.9.13

- Added matcher for super class hierarchy that ignores interfaces.
- Extend API for member substitution.
- Minor API extensions.

### 26. March 2019: version 1.9.12

- Fixed stack map frame generation during constructor advice.
- Improves frame generation for cropping-capable frames.

### 21. March 2019: version 1.9.11

- Remove field reference in injected class due to possibility of loading Byte Buddy on the boot loader.
- Updated to ASM 7.1.
- Fix unsafe injection on Java 12/13.

### 11. February 2019: version 1.9.10

- Fixed `ByteArrayClassLoader` when used from boot class loader.
- Fixed shading to include ASM class required during renaming.

### 4. February 2019: version 1.9.9

- Properly interrupt resubmission process in agent builder.
- Fix visibility checks for nest mates.

### 24. January 2019: version 1.9.8

- Extend `MethodCall` to allow for loading target from `StackManipulation`.
- Allow for injection into `MultipleParentClassLoader`.
- Performance improvement on array creation.
- Allow for custom strategy for visibility bridge creation.

### 10. January 2019: version 1.9.7

- Retain native modifier when defining a method without method body.
- Allow appending class loader to multiple parent class loader with hierarchy check.
- Add support for Java 13.
- Extend experimental property to allow for detection of unknown versions.

### 13. December 2018: version 1.9.6

- Add the JVM extension / platform class loaders to the default excludes for the `AgentBuilder`.
- Refactor `MethodCall` to better reuse intermediates. This implies some API changes in the customization API.
- Add hook to `AgentBuilder` to customize class file transformer.

### 22. November 2018: version 1.9.5

- Fixed lookup injection for classes in the default package in Java 9.

### 13. November 2018: version 1.9.4

- Add API for explicit field access from `FieldAccessor`.
- Fix stack size adjustment for custom `MemberSubstitution`s.
- Performance improvement for classes with many methods.

### 28. October 2018: version 1.9.3

- Update to ASM 7.0 final
- Improve field setting capabilities of `FieldAccessor` and `MethodCall`.

### 15. October 2018: version 1.9.2

- Allow for delegation to method result for `MethodDelegation`.
- Extend `MemberSubstitution` to allow for delegating to matched member.
- Create multi-release jar for module-info carrying artifacts.
- Properly handle directory elements in plugin engine with in-memory or folder target.

### 5. October 2018: version 1.9.1

- Minor API change of `Plugin.Engine.Source` to allow for closing resources that need to be opened.
- Reinstantiate class injection on Java 12 with new Unsafe use.
- Allow for disabling use of Unsafe alltogether.
- Adjust Gradle build plugin to use closure for argument instantiation.
- Prepare method arguments on `MethodCall`.

### 29. September 2018: version 1.9.0

- Update to ASM 7 for non-experimental Java 11 support.
- Reduce byte code level to Java 5.
- Add *module-info.class* for *byte-buddy* and *byte-buddy-agent* artifacts.
- Extend `ClassInjector` API to allow supplying string to byte array mappings.
- Add visitor to allow adjustment of inner class attribute.
- Refactor agent builder API to use decoration by default and rather require explicit termination.
- Add `Plugin.Engine` to allow simple static enhancements and rework build plugins for Maven and Gradle to use it.
- Refactor `AsmVisitorWrapper.ForDeclaredMethods` to only instrument methods on `.method` but offer `.invokable` for anthing.

### 8. September 2018: version 1.8.22

- Add guard to `EnclosedMethod` property upon redefinition to avoid error with Groovy which often gets the propery wrong.
- Add possibility to sort fields in plugin-generated equals method.
- Add class file locator for URL instances.

### 3. September 2018: version 1.8.21

- Added caching for expensive methods of reflection API.
- Fix treatment of inner class attributes for redefinition and rebasement.
- Extend build plugin API to achieve better Java agent compatibility.
- Add convenience API for creating lambda expressions.

### 29. August 2018: version 1.8.20

- Fix decoration to include non-virtual methods.
- Add build plugin for caching the return value of a method.

### 28. August 2018: version 1.8.19

- Fix annotation handling in decorator strategy.
- Several minor bug fixes for `MethodCall`.
- Fix shading for signature remapper.

### 27. August 2018: version 1.8.18

- Add API for defining inner types and nest mate groups.
- Add decoration transformer for more efficient application of ASM visitors.
- Allow chaining of `MethodCall`s.
- Prohibit illegal constructor invocation from `MethodCall`s.

### 6. August 2018: version 1.8.17

- Fix class loader injection using `putBoolean`.
- Do not set timeout for Unix attach simulation if value is `0`.
- Avoid incorrect lookup of `getDefinedPackage` on Java 8 IBM VMs.
- Fix type checks on constantdynamic support.

### 3. August 2018: version 1.8.16

- Add support for dynamic class file constants for Java 11+.
- Suppress lazy resolution errors within `MemberSubstitution::relaxed`.
- Generalize method matcher for `clone` method.
- Add `toString` method to `ClassFileVersion`.
- Reenable `ClassFileInjection.Default.INJECTION` in Java 11+ via fallback onto `Unsafe::putBoolean`.

### 26. July 2018: version 1.8.15

- Add preliminary support for Java 12.

### 24. July 2018: version 1.8.14

- Query explicitly added class loaders before the instrumented class's class loader in advice transformer for an agent builder.
- Add nullcheck for `Instrumentation::getAllLoadedClasses`.
- Allow for access controller-based lookups for `Method` constants.
- Use `getMethod` instead of `getDeclaredMethod` for method lookup if possible.

### 5. July 2018: version 1.8.13

- Update to ASM 6.2
- Reinstate support for latest Java 11 EA if `net.bytebuddy.experimental` is set.
- Fix edge completion for `AgentBuilder`.
- Dump input class file if the `net.bytebuddy.dump` is set.
- Add convenience chaining methods to `Implementation.Compound`.
- Fix nestmate changes in method invocation.

### 25. May 2018: version 1.8.12

- Fix misrepresentation of default package as `null`.
- Add `Advice.Exit` annotation and allow for method repetition based on exit advice value.
- Add `Advice.Local` annotation to allow for stack allocation of additional variables.
- Improve advice's method size handler.

### 4. May 2018: version 1.8.11

- Avoid shading unused ASM classes with incomplete links what breaks lint on Android and JPMS module generation.

### 28. April 2018: version 1.8.10

- Extended support for self-attachment by using current jar file for Java 9+.
- Minor performance improvements.

### 27. April 2018: version 1.8.9

- Several performance improvements.
- Adjust `toString` implementation for parameterized types to the changed OpenJDK 8+ behavior.
- Attempt self-attachment using the current jar file.

### 20. April 2018: version 1.8.8

- Use cache for loaded `TypeDescription` to avoid overallocation.
- Generalize exception handler API for `Advice`.

### 19. April 2018: version 1.8.7

- Added `ClassWriterStrategy` that allows controlling how the constant pool is copied.

### 18. April 2018: version 1.8.6

- Introduced concept of sealing the `InjectionClassLoader` to avoid abuse.
- Avoid class loader leak by not storing exceptions thrown in class initializers which can keep references to their first loading class in their backtrace.
- Add `ClassFileBufferStrategy` to agent builder.
- Retain deprecation modifier on intercepted methods and fields on class files prior to Java 5.

### 15. April 2018: version 1.8.5

- Release with `equals` and `hashCode` methods being generated based on the fixes in the previous version.

### 15. April 2018: version 1.8.4

- Only open ASM if this is specified via the boolean property `net.bytebuddy.experimental`.
- Fix resolution of invoking methods of `Object` on interfaces to not specialize on the interface type. The latter is also failing verification on Android.
- Several performance improvements.
- Do no longer use unsafe injection as a default class loading strategy.

### 31. March 2018: version 1.8.3

- Allow Java 11 classes by opening ASM.
- Remove Lombok and add methods using Byte Buddy plugin.

### 31. March 2018: version 1.8.2

- Reduce log output for Gradle and Maven plugin.
- Fix class check in `EqualsMethod`.

### 28. March 2018: version 1.8.1

- Add implementations for `HashCodeMethod`, `EqualsMethod` and `ToStringMethod` including build tool plugins.
- Refactor handling of stack map frame translation within `Advice` to allow for handling of methods with inconsistent stack map frames if the method arguments are copied.
- Make argument copying the default choice if exit advice is enabled. 
- Fix a bug in parameter annotation computation within `Advice`.
- Update to ASM 6.1.1.  

### 13. March 2018: version 1.8.0

- Refactored `Advice` argument handling to be controlled by a dedicated structure.
- Added basic logic for argument copying in `Advice`.
- Fix performance degradation for cached fields.
- Add support for Java 10 and preliminary support for Java 11.

### 2. March 2018: version 1.7.11

- Fix Maven and Gradle plugins to resolve correct class file version.
- Add method to `ClassReloadingStrategy` to allow specification of explicit redefinition strategy. Change default redefinition strategy.
- Improve stack map frame validation in `Advice`.
- Fix type resolution for virtual calls in `MemberSubstitution`.

### 2. February 2018: version 1.7.10

- Fixes self-attachment on Java 9+ on Windows.
- Check for non-accessibility on `MethodCall`.
- Change static proxy fields to be `volatile`.
- Do not copy security-related meta-data on jar file copying.
- Guard resolution of annotations for methods with synthetic parameters.
- Forbid skipping code in constructors for `Advice`.
- Added constructor strategy for defining a default constructor that invokes a non-default constructor.
- Improve performance of accessor methods and cache fields by reducing use of `String::format`.

### 6. November 2017: version 1.7.9

- Fixes `RAW_TYPES` mode for loaded types where properties were resolved incorrectly.
- Adds support for Java 10 version number.

### 24. October 2017: version 1.7.8

- Added property `net.bytebuddy.raw` to allow for suppress generic type navigation.

### 22. October 2017: version 1.7.7

- Make self-attachment more robust on Windows.
- Add M2E instructions for Maven plugin.
- Improve hash function for members and avoid collision field names.
- Convenience for custom target binders.

### 8. October 2017: version 1.7.6

- Update ASM to version 6 final.
- Accept `null` in custom bound `Advice`.
- Fix fail fast in build plugins.
- Permit repeated exception in method signature.

### 30. August 2017: version 1.7.5

- Prevents premature termination of reattempting retransformation.

### 30. August 2017: version 1.7.4

- Add convenience methods for defining bean properties.
- Minor fixes to support Java 9 and allow building on JDK9.

### 11. August 2017: version 1.7.3

- Allow configuring `TypePool` to use within `Advice`.

### 05. August 2017: version 1.7.2

- Fixes possibility to customize binding in `MethodDelegation`.
- Update to ASM 6 beta to support Java 9 fully.

### 15. June 2017: version 1.7.1

- Added `DiscoveryStrategy` for redefinition to determine types to be redefined.
- Changed Maven plugin to only warn of missing output directory which might be missing.
- Added global circularity lock.
- Removed sporadic use of Java util logging API.

### 14. May 2017: version 1.7.0

- Define names for automatic modules in Java 9.
- Introduce property `net.bytebuddy.nexus.disabled` to allow disabling `Nexus` mechanism.
- Do not use context `ProtectionDomain` when using `Nexus` class.
- Normalize `Advice` class custom bindings via opening internally used `OffsetMapping` API. Remove `CustomValue` binding which is less powerful.
- Do not group `transient` with `volatile` modifier.
- Introduce `MemberRemoval` component for removing fields and/or methods.
- Introduce first version for `MemberSubstituion` class for replacing field/method access.

### 26. April 2017: version 1.6.14

- Extended `AgentBuilder` listener API.
- Added trivial `RawMatcher`.
- Check modules for modifiability.
- Adapt new Java 9 namespaces for modules.
- Start external process for self-attachment if self-attachment is not allowed.

### 17. April 2017: version 1.6.13

- Explicit consistency check for stack map frame information in `Advice`.
- Extended `InstallationListener` API.
- Fixed stack size information on variable storage.

### 18. March 2017: version 1.6.12

- Add `InstallationListener` in favor of `InstallationStrategy` and allow resubmission strategy to hook into it in order to cancel submitted jobs.

### 12. March 2017: version 1.6.11

- Fix modifier adjustment for visibility bridges (did not work last time)
- Added class injector for Java 9 handle class definition.

### 1. March 2017: version 1.6.10

- Allow installation of `ClassFileTransformer` in byte array class loader.
- Adjust visibility for bridge methods.

### 20. February 2017: version 1.6.9

- Properly add visibility bridges for default methods.
- Added matcher for unresolvable types.
- Improved `ByteBuddyAgent` API.
- Fixed Gradle and Maven plugin path resolution.

### 12. February 2017: version 1.6.8

- Avoid logging on empty resubmission.
- Retain actual modifiers on frozen instrumented type.

### 26. January 2017: version 1.6.7

- Refactored `Resubmitter` to a DSL-step within the redefinition configuration.
- Added additional element matchers.

### 24. January 2017: version 1.6.6

- Fixed computation of modifiers for rebased method in native state.

### 20. January 2017: version 1.6.5

- Improved lazy resolution of super types in matchers.
- Added frozen instrumented type and factory for such types when no class file format changes are desired.
- Improved lazy resolution for generic type signatures.

### 19. January 2017: version 1.6.4

- Refactored super type visitors to always be lazy until generic properties are required to be resolved.
- Apply proper raw type resolution. Made default method graph compiler reify generic types to compute correct bridges.

### 13. January 2017: version 1.6.3

- Improved `Resubmitter` configuration.
- Added `AgentBuilder.Transformation.ForAdvice` to allow for simple creation of `Advice` classes from Java agents.

### 11. January 2017: version 1.6.2

- Removed obsolete `toString` representations.
- Start using Lombok for equals/hashCode unless explicit.
- Add security manager check to Byte Buddy agent.
- Added asynchronous super type loading strategies.
- Added resubmitter.
- Added class injection strategy for Android.
- Fixed type initializer instrumentation for redefinitions.
- Added `loaded` property for listener on agent builder.

### 5. January 2017: version 1.6.1

- Added check to `@Pipe` for method invokability.
- Added unsafe `ClassInjector` and class loading strategy.
- Improved reflection-based class injector on Java 9.
- Removed unnecessary class file location using modules on Java 9.
- Improved fail-safety for type variable resolution to allow processing incorrectly declared type variables.

### 2. January 2017: version 1.6.0

- Added `InjectingClassLoader` with class loading strategy that allows for reflection-free loading.
- Added proper class loader locking to injection strategy.
- Fixed method lookup to not use *declared* accessors unless necessary to avoid security manager check.
- Added `@SuperMethod` and `@DefaultMethod` annotations for `MethodDelegation`.
- Refactored `AsmVisitorWrapper` to accept a list of fields and methods that are intercepted. This allows to use the wrapper also for methods that are overridden.
- Added a `MethodGraph.Compiler.ForDeclaredMethods` to avoid processing full type hierarchy if only type enhancement should be done without declaring new methods on a type. This should be used in combination with `Advice` instead of `MethodGraph.Empty` as those methods are supplied to the ASM visitor wrappers.
- Refactored `MethodDelegation` to precompile records for all candidates to avoid duplicate annotation processing.

### 29. December 2016: version 1.5.13

- Updates to ASM 5.2
- Updates Android DX-maker.
- Adds API to `MultipleParentClassLoader` to use other loader than bootstrap loader as a parent which is not always legal, e.g. on Android.
- Make `ClassInjector` use official class loading lock if one is available.
- Make `ClassInjector` use `getDefinedPackage` instead of `getPackage` if available.
- Declare UNIX socket library as provided in Byte Buddy agent to only add the dependency on demand.

### 27. December 2016: version 1.5.12

- Refactored rebasing of type initializers. Do no longer rebase into static method to pass validation for final static field assignment on Java 9.
- Added fallback to `sun.misc.Unsafe` for class definition if reflective access to the protected `ClassLoader` methods is not available which are required for the injection strategy.
- Added super-type-loading `DescriptorStrategy` for agent builder.
- Added assignment checks for `MethodCall` for invocation target.

### 20. December 2016: version 1.5.11

- Resolved compound components to linearize nested collections for vastly improved performance with large structures.
- Added `TypeCache`.
- Added fallback to assign `null` to `SuperCall` and `DefaultCall` if assignment is impossible.
- Deprecated `Forwarding` in favor of `MethodCall`.
- Fixed matcher for interfaces in type builder DSL.
- Fixed resolution of field type in `MethodCall`.

### 16. December 2016: version 1.5.10

- Added possibility for readding types after a failed retransformation batch.
- Added partitioning batch allocator.

### 13. December 2016: version 1.5.9

- Allow specifying `TargetType` in `Advice.FieldValue`.
- Allow array value explosion in `MethodCall`.
- Extended `FieldAccessor` to allow reading `FieldDescription`s directly.
- Fixed class name resolution in Maven and Gradle plugins.

### 5. December 2016: version 1.5.8

- Added implementation for attachment on Linux and HotSpot using a non-JDK VM.
- Fixed argument resolution for `ByteBuddyAgent`.
- Fixed field resolution for `MethodCall` to allow custom definition of fields.
- Fixed visibility checks.
- Do not override default method for proxies for `Pipe`.

### 25. November 2016: version 1.5.7

- Fixed type discovery for custom advice annotation bindings.

### 18. November 2016: version 1.5.6

- Added possibility to configure suppression handler in `Advice` classes.

### 17. November 2016: version 1.5.5

- Refactored `Advice` to use stack manipulations and `Assigner`.
- Refactored `Advice` to use `Return` instead of `BoxedReturn` and added `AllArguments` instead of `BoxedArguments` in conjunction with allowing to use dynamic typing for assignments via the annotation.
- Added fixed value instrumentation for method parameters.
- Added weak class loader referencing for `Nexus` and allow registration of a `ReferenceQueue`.

### 11. November 2016: version 1.5.4

- Extended `MethodCall` API.
- Added additional element matchers.
- Extended `AsmVisitorWrapper` API.

### 3. November 2016: version 1.5.3

- Refactored `Advice` to allow usage as a wrapper for an `Implementation`. This allows chaining of such advices.
- Allow to dynamically locate a `FieldDescription` or `ParameterDescription` from a custom `Advice` annotation which binds the field or parameter value.
- Added `invokeSelf` option to `MethodCall` instrumentation.

### 31. October 2016: version 1.5.2

- Refactored `FieldAccessor` to allow more flexible creation of getters and setters of particular parameters.
- Create string-based hashes for random fields that depend on a value's hash value.

### 27. October 2016: version 1.5.1

- Fixed stack size computation when using `@Advice.Origin`.

### 25. October 2016: version 1.5.0

- Refactor `Instrumentation`s to only delegate to fields instead of requiring their definition. The `defineField` API should be generally preferred for defining fields as it is much richer and therefore easier to extend.
- Made type annotation reader more robust towards older versions of Java 8.
- Refactored lazy type resolution for generic types to no longer eagerly load generic types when navigating through a type hierarchy.
- Unified several implementation APIs and added better abstractions.
- Fixed some missing bits of validation of implementations.
- Do not replicate incompatible bridge methods.

### 17. October 2016: version 1.4.33

- Use `IMITATE_SUPER_CLASS_OPENING` as a default constructor strategy.
- Extract method visibility during method graph compilation.
- Apply a type variable's erasure if a type variable is out of scope instead of throwing an exception. This can happen when subclassing an inner type outside of its outer type or when a compiler such as *scalac* adds inconsistent generic type information.
- Optimize the application of the ignore matcher within an agent builder to only be applied once.

### 11. October 2016: version 1.4.32

- Added `ConstructorStrategy` for inheriting constructors but make them `public`.
- Do not instrument anonymously loaded types during redefinition unless the lambda strategy is enabled.

### 6. October 2016: version 1.4.31

- Reuse `CircularityLock` on all `AgentBuilder`s by default to avoid that Byte Buddy agents introduce circularities to different agents.
- Also allow using `Advice` as `Implementation`.
- Added `FixedValue.self()` and added `FieldPersistence` for describing `volatile` fields.

### 4. October 2016: version 1.4.30

- Also acquire circularity lock during class file retransformation.
- Added slicing `BatchAllocator`.

### 3. October 2016: version 1.4.29

- Explicitly check for recursive transformation of types used during a transformation causing a `ClassCircularityError` from an `AgentBuilder` by adding thread-local locking.

### 30. September 2016: version 1.4.28

- Additional refactoring of the `AgentBuilder` to fix a regression of 1.4.27.
- Unified the error listener and the regular listener that were added in the previous version.

### 29. September 2016: version 1.4.27

- Refactored `AgentBuilder` retransformation mechanism to allow for custom recovery and batch strategies.
- Fixed Gradle plugin build which did not contain files.
- Supply no argument to agent attachment by default instead of empty string argument.

*Note*: Currently, it seems like the new retransformation mechanism introduces a racing condition in class loading resulting in some classes not being instrumented

### 21. September 2016: version 1.4.26

- Refactored `skipOn` property of `Advice` component.
- Allow reading a `Method`/`Constructor` property from `Advice`.
- Fixed bug that duplicated added parameter annotations on a `DynamicType` builder.

### 20. September 2016: version 1.4.25

- Added overloaded versions for `byte-buddy-agent` to allow agent attachment with explicit argument.
- Made `Advice` more flexible to allow skipping of instrumented method for complex advice method return types.

### 15. September 2016: version 1.4.24

- Make `AgentBuilder` produce `ResettableClassFileTransformer`s which can undo their transformation.

### 14. September 2016: version 1.4.23

- Made `TypeDescription.ForLoadedType` serializable for better alignment with reflection API.
- Adapted changes in `Instrumentation` API for Java 9.
- Refactored `AnnotationValue` to apply Java 9 specific string rendering on Java 9 VMs.
- Adapted new `toString` representation of parameterized types on Java 9 VMs.

### 02. September 2016: version 1.4.22

- Added Byte Buddy plugin for Gradle.
- Improved Byte Buddy plugin for Maven.

### 28. August 2016: version 1.4.21

- Fixed modifier resolution for anonymous classes to preserve a shadowed `final` modifier.
- Added Byte Buddy plugin for Maven.

### 21. August 2016: version 1.4.20

- Fixed stack size adjustment for accessing double-sized primitive access array elements.
- Fixed `Advice` adjustment of local variable index for debugging information (improves Java agent compatibility).
- Renamed `TypeLocator` to `PoolStrategy` to avoid confusion with the names.
- Removed `DescriptionStrategy`s that rely on fallback-description as those do not properly fallback for meta data.
- Added `FallbackStrategy` as a replacement which allows to reattempt a transformation without using loaded type information.

### 14. August 2016: version 1.4.19

- Added `@StubValue` and `@Unused` annotations to `Advice` component.
- Added possibility to retain line number information for entry`Advice`
- Removed class loader dependency when querying loaded annotation values.
- Made annotation values more type-safe.

### 9. August 2016: version 1.4.18

- Added automatic support for Java 9 class file location for boot modules.
- Improved `FieldProxy.Binder` to allow for a single accessor interface.
- Fixed counting problem in `Advice` component.

### 1. August 2016: version 1.4.17

- Fixed annotation resolution for Java 9 to exclude the `jdk.internal` namespace by default.
- Do not copy annotations for default constructor strategies but allow configuring annotation strategy.
- Added file-system class file locators for modules in Java 9.
- Added convenience methods to default location strategies.
- Exclude `sun.reflect` namespace by default from `AgentBuilder` to avoid error messages.
- Fixed resolution of type variables for transformed methods and fields.
- Fixed stack-aware method visitor when encountering exchanging duplication instructions.

### 28. July 2016: version 1.4.16

- Added `POOL_LAST_DEFERRED` and `POOL_LAST_FALLBACK` description strategy.
- Fixed resolution of bridge methods for diamond inheritance.
- Refactored modifier API to only expose named modifier checks for an element that apply to it.
- Fixed resolution for type variables for transformed methods.

### 25. July 2016: version 1.4.15

- Fixed frame generation for `void` methods without regular return in `Advice`.
- Fixed `TypeValidation` for Java 8 interfaces not allowing private methods.
- Simplified and documented `AccessController` usage.

### 21. July 2016: version 1.4.14

- Fixed bug with handling of legacy byte code instructions in `Advice` component.
- Cleaned up and refactored usage of `AccessController`. Added privileged handling to `AgentBuilder`.
- Added proper buffering to non-buffered interaction with file streams.
- Make `ByteBuddy` creation more robust by adding a default fallback for unknown VMs.
- Improved support for Java 9.

### 19. July 2016: version 1.4.13

- Lazily compute `Implementation.Target` and `Implementation.Context` in case of a type inlining to provide correct feature set. Added validation if this constraint is broken.
- Make `TypePool` using an eager `TypeDescription` more robust towards errors.

### 15. July 2016: version 1.4.12

- Monitor advice code for inconsistent stack heights at return statements to clean the stack during instrumentation to not trigger verifier errors if such atypical but legal code is encountered.
- Do not generate handlers for return values if an instrumented method or an advice method only throws exceptions but never returns regularly.

### 15. July 2016: version 1.4.11

- Added tracer for the state of the operand stack for the `Advice` component to clear the stack upon a return. Without this, if code would return without leaving the stack empty, a verifier error would be thrown. This typically is only a problem when processing code that was produced by other code generation libraries.

### 15. July 2016: version 1.4.10

- Fixed resolution of modifiers and local type properties from a default type pool.
- Improved key for caching `TypeLocator` to share a key for the system and bootstrap class loader.

### 11. July 2016: version 1.4.9

- Added additional implementations of a `DescriptionStrategy` for `POOL_LAST` and `POOL_FIRST` resolution.
 
### 6. July 2016: version 1.4.8

- Allow to skip execution of instrumented method from `Advice` via entry advice indicated by return value.
- Added API to transform predefined type variables on a dynamic type.
- Refactored `Transformer` API to be shared for methods, fields and type variables.
- Allow to spread `Advice` methods over multiple classes.
- Added convenience methods to `AsmVisitorWrapper`s for declared fields and methods.
- Performance improvements in `Advice` class for byte code parsing.

### 6. July 2016: version 1.4.7

- Added default `TypePool` that allows for lazy resolution of referenced types. This can both be a performance improvement and allows working with optional types as long as they are not directly required within a transformation. This type pool is now used by default.
- Make interfaces public by default when creating them via `ByteBuddy::makeInterface`.
- Added `TypeResolutionStrategy` to allow for active resolution via the `Nexus` also from outside the `AgentBuilder`.
- Make best effort from a `ClassLoadingStrategy` to not resolve types during loading.
- Added convenience method for loading a dynamic type with an implicit `ClassLoadingStrategy`.

### 30. June 2016: version 1.4.6

- Added a `ClassFileLocator` for a class loader that only references it weakly.
- Allow to supply `TypePool` and `ClassFileLocator` separately within an `AgentBuilder`.
- Made `MethodPool` sensitive to bridge methods which should only be added to classes of a version older than Java 4.
- Fixed creation of Java 9 aware `ClassFileTransformer` to only apply on Java 9 VMs.
- Added matcher for the type of a class loader.
- Fixed name resolution of anonymously-loaded types.

### 24. June 2016: version 1.4.5

- Added `InstallationStrategy` to `AgentBuilder` that allows customization of error handling.
- Added *chunked* redefinition and retransformation strategies.

### 23. June 2016: version 1.4.4

- Added `net.bytebuddy` qualifier when logging.
- Added `net.bytebuddy.dump` system property for specifying a location for writing all created class files.

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
- Optimized frame computation to emit frames of the minimal, possible size when using `Advice`.
- Only add exit `Advice` once to reduce amount of added bytes to avoid size explosion when a method supplied several exits.
- Optimized `Advice` injection to only add advice infrastructure if entry/exit advice is supplied.
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
