# Builds the Byte Buddy fuzzing harnesses for ClusterFuzzLite and OSS-Fuzz.
#
# The base image provides a JDK, Maven (as $MVN), and the Jazzer driver and agent (seeded into $OUT). The image's Maven
# is used in build.sh rather than the project's Maven wrapper so that the wrapper's checksum-enforcement extension is not
# involved in the container build.
FROM gcr.io/oss-fuzz-base/base-builder-jvm@sha256:b1d95ac60392fd9958b4da1d45e1b4337a743c7012853f95a529177ef0ec50c4

COPY . $SRC/byte-buddy
WORKDIR $SRC/byte-buddy
COPY .clusterfuzzlite/build.sh $SRC/
