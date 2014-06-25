Byte Buddy release notes
------------------------

### 02. Mai 2014: Version 0.1

First general release.

### 16. June 2014: Version 0.2

Added several bug fixes for existing features. Beyond that the following features were added or changed:

 - Changed the semantics of the `@SuperCall` to be only bindable, if a super method can be invoked. Before, an
   exception was thrown if only a non-existent or abstract super method was found.
 - Added features for the interaction with Java 8 default methods. Refactored method lookup to extract invokable
   default methods.
 - Refactored the invocation of super methods to be created by an `Instrumentation.Target`. For a future release,
   this hopefully allows for class redefinitions using today's API for creating subclasses.
 - Upgraded to ASM 5.0.3.

### 20. June 2014: Version 0.2.1

Added proper support for defining class initializers. Added support for field caching from method instrumentations,
mainly for allowing the reuse of `Method` instances for the `@Origin` annotation and the `InvocationHandlerAdapter`.

### Current snapshot version

- Added basic support for Java 7 types `MethodHandle` and `MethodType` which are available from Java 7.
