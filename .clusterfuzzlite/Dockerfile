# Builds the Byte Buddy fuzzing harnesses for ClusterFuzzLite and OSS-Fuzz.
#
# The base image provides a JDK, Maven (as $MVN), and the Jazzer driver and agent (seeded into $OUT). The image's Maven
# is used in build.sh rather than the project's Maven wrapper so that the wrapper's checksum-enforcement extension is not
# involved in the container build.
FROM gcr.io/oss-fuzz-base/base-builder-jvm

COPY . $SRC/byte-buddy
WORKDIR $SRC/byte-buddy
COPY .clusterfuzzlite/build.sh $SRC/
