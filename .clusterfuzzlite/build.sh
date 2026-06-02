#!/bin/bash -eu
#
# Copyright 2014 - Present Rafael Winterhalter
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Builds the byte-buddy-fuzz harnesses and assembles one runnable fuzzer per harness in $OUT. Invoked by the OSS-Fuzz /
# ClusterFuzzLite infrastructure inside the container defined by the adjacent Dockerfile.

cd "$SRC/byte-buddy"

# Build the fuzzing module and install its reactor dependency (byte-buddy-dep) into the local repository so that the
# subsequent dependency:copy-dependencies goal can resolve it. The "fuzz" profile adds the byte-buddy-fuzz module to the
# reactor; it is excluded from every other build.
$MVN -Pfuzz -pl byte-buddy-fuzz -am -DskipTests install

# Resolve the project version to locate the built artifact.
VERSION=$($MVN -Pfuzz -pl byte-buddy-fuzz -q -DforceStdout help:evaluate -Dexpression=project.version)

# Copy the harness jar and every runtime dependency next to the fuzzers. Jazzer's own API is provided at runtime by the
# agent that the base image places into $OUT, so it is scoped "provided" and intentionally not copied here.
cp "byte-buddy-fuzz/target/byte-buddy-fuzz-${VERSION}.jar" "$OUT/byte-buddy-fuzz.jar"
$MVN -Pfuzz -pl byte-buddy-fuzz -DskipTests dependency:copy-dependencies \
  -DincludeScope=runtime -DoutputDirectory="$OUT"

# Generate a launcher per harness. jazzer_driver and jazzer_agent_deploy.jar are seeded into $OUT by the base image, and
# the "$this_dir/*" class path entry picks up byte-buddy-fuzz.jar together with all copied runtime dependencies.
for class in ClassGenerationFuzzer ClassFileParsingFuzzer; do
  cat > "$OUT/${class}" <<EOF
#!/bin/bash
this_dir=\$(dirname "\$0")
LD_LIBRARY_PATH="\$JVM_LD_LIBRARY_PATH":\$this_dir \\
  \$this_dir/jazzer_driver --agent_path=\$this_dir/jazzer_agent_deploy.jar \\
  --cp="\$this_dir/*" \\
  --target_class=net.bytebuddy.fuzz.${class} \\
  --jvm_args="-Xmx2048m:-Xss1024k" \\
  "\$@"
EOF
  chmod +x "$OUT/${class}"
done

# Generate the parsing harness seed corpus at build time so that no class files need to be committed to the repository.
# SeedCorpusGenerator writes a set of real class files that fuzzing starts from instead of random bytes; OSS-Fuzz
# associates a "<fuzzer>_seed_corpus.zip" archive with the fuzzer of the same name. The class path uses the jars already
# laid into $OUT (byte-buddy-fuzz.jar and its runtime dependencies).
SEED_CORPUS_DIR="$(mktemp -d)"
java -cp "$OUT/*" net.bytebuddy.fuzz.SeedCorpusGenerator "$SEED_CORPUS_DIR"
if [ -n "$(find "$SEED_CORPUS_DIR" -maxdepth 1 -type f -print -quit)" ]; then
  find "$SEED_CORPUS_DIR" -maxdepth 1 -type f -print0 | xargs -0 zip -j "$OUT/ClassFileParsingFuzzer_seed_corpus.zip" > /dev/null
fi
