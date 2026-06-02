# Byte Buddy — coverage-guided fuzzing

This module contains [Jazzer](https://github.com/CodeIntelligenceTesting/jazzer) fuzzing harnesses that probe Byte
Buddy's two security-relevant surfaces:

| Harness | Class | What it tests | Oracle |
| --- | --- | --- | --- |
| Generation | `ClassGenerationFuzzer` | Byte Buddy emitting class files | The class always loads, i.e. passes the JVM byte code verifier — a `VerifyError`/`ClassFormatError` is a defect |
| Parsing | `ClassFileParsingFuzzer` | `TypePool` parsing untrusted class files | Only a bounded set of declared exceptions may escape — a `StackOverflowError`/`OutOfMemoryError`/hang is a defect |

The generation harness is the higher-value target: it exercises Byte Buddy's *own* code and uses the JVM verifier as a
free oracle. Raw class-file parsing largely delegates to ASM, which is already continuously fuzzed upstream, so the
parsing harness focuses on the `TypePool` layer (notably generic-signature parsing).

The module requires **Java 8 or later** (Jazzer's minimum). Its *source* is deliberately kept Java 5 compatible to match
the rest of the project, but it always compiles to Java 8 byte code. It is excluded from the default reactor and is only
built when the `fuzz` profile is active (`-Pfuzz`).

## Replay as a regression test (CI)

```bash
mvn -Pfuzz -pl byte-buddy-fuzz -am test
```

`FuzzRegressionTest` replays, via the Jazzer JUnit 5 integration, every input found under
`src/test/resources/<package>/FuzzRegressionTestInputs/<method>/` — any saved crashers plus, if present, a locally
generated seed corpus (see below). This is fast and deterministic; no campaign is run.

## Seed corpus (generated, never committed)

Coverage-guided fuzzing converges far faster from real `.class` files than from random bytes. Rather than commit binary
class files to the repository, the seed corpus is **generated**:

- **In CI / OSS-Fuzz**, `.clusterfuzzlite/build.sh` runs `SeedCorpusGenerator` at build time and packages the result as
  `ClassFileParsingFuzzer_seed_corpus.zip` next to the fuzzer — no checked-in artifacts.
- **Locally**, you can optionally populate the parsing harness's replay directory to enrich the regression run. The
  generated files are git-ignored, so they will not be committed:

  ```bash
  mvn -Pfuzz -pl byte-buddy-fuzz compile \
    org.codehaus.mojo:exec-maven-plugin:3.1.0:java \
    -Dexec.mainClass=net.bytebuddy.fuzz.SeedCorpusGenerator \
    -Dexec.classpathScope=compile \
    -Dexec.args="$(pwd)/byte-buddy-fuzz/src/test/resources/net/bytebuddy/fuzz/FuzzRegressionTestInputs/classFileParsing"
  ```

  Pass a different directory as the argument to seed the standalone Jazzer driver's corpus instead.

## Run an open-ended campaign locally

Drive the JUnit harness in fuzzing mode (it writes any new finding back into the `...Inputs/<method>` directory):

```bash
JAZZER_FUZZ=1 mvn -Pfuzz -pl byte-buddy-fuzz -am test
```

…or invoke the standalone Jazzer driver against a harness class, persisting coverage in a corpus directory:

```bash
java -cp "byte-buddy-fuzz/target/classes:$(cat cp.txt):jazzer_standalone.jar" \
  com.code_intelligence.jazzer.Jazzer \
  --target_class=net.bytebuddy.fuzz.ClassFileParsingFuzzer \
  /path/to/corpus \
  -max_total_time=600
```

## Continuous fuzzing in CI (ClusterFuzzLite)

The repository is wired for [ClusterFuzzLite](https://google.github.io/clusterfuzzlite/) — the GitHub-Action form of
CIFuzz that runs without prior OSS-Fuzz onboarding:

- `.clusterfuzzlite/Dockerfile` — builds on the OSS-Fuzz `base-builder-jvm` image (which supplies the JDK, Maven and the
  Jazzer engine).
- `.clusterfuzzlite/build.sh` — compiles `byte-buddy-fuzz`, lays the harness jar and its runtime dependencies into
  `$OUT`, emits one launcher per harness, and generates the seed corpus at build time.
- `.github/workflows/cflite_pr.yml` — on each pull request, builds the fuzzers and runs them for five minutes against
  the change, failing the PR if a harness crashes.

The same build is what OSS-Fuzz consumes, so onboarding there later reuses these files unchanged.

> The ClusterFuzzLite actions in `cflite_pr.yml` are pinned to the `@v1` tag. To satisfy this repository's
> pinned-action policy (enforced by the Scorecard workflow), pin them to the corresponding commit SHA, and verify the
> container build on the first CI run.

## Next step: OSS-Fuzz

These `fuzzerTestOneInput` entry points are exactly what [OSS-Fuzz](https://github.com/google/oss-fuzz) consumes. Given
Byte Buddy's dependency footprint it is a strong candidate; continuous fuzzing there runs the harnesses 24/7, files
issues, and verifies fixes automatically.
