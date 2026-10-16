#!/usr/bin/env bash
# Typecheck Guava with the Mutability (PICO) Checker.
#
#   ./typecheck-mutability.sh                                          # whole module (~34s)
#   ./typecheck-mutability.sh src/com/google/common/collect/Maps.java  # one file (seconds)
#
# Do NOT use Maven for this. maven-compiler-plugin offers no way to set the
# thread stack size (`maxmem` maps to -Xmx only) and silently drops -J-Xss from
# compilerArgs, so it cannot check the files that need a deeper stack. Maven is
# needed exactly once, to produce guava-cp.txt:
#
#   CHECKERFRAMEWORK=... mvn -B -P checkerframework-local \
#     dependency:build-classpath -Dmdep.outputFile=../guava-cp.txt
#
# The JDK annotations come from the annotated JDK bundled in checker.jar, so
# checker.jar must be built against aosen-xiong/jdk#4 (-PannotatedJdkHome) and
# rebuilt after every JDK change. java.base is not patched from source: doing
# so makes the checker see AbstractCollection's Collection supertype as raw,
# which gave order-dependent errors and an AsSuperVisitor crash.
set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
: "${CHECKERFRAMEWORK:=/Users/aosenxiong/cf-pico-move}"
: "${JAVA_HOME:=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home}"
export JAVA_HOME

CP="$(cat "${ROOT}/guava-cp.txt")"
LOG="${ROOT}/compile.log"
SRC="${ROOT}/.sources.txt"

if [ "$#" -gt 0 ]; then
  printf '%s\n' "$@" > "${SRC}"
  # A single file cannot resolve the rest of Guava on its own. -sourcepath lets
  # javac find the other classes; -implicit:none stops it from also CHECKING
  # them, which would turn a one-file run back into a whole-module run.
  SCOPE=(-sourcepath src -implicit:none)
else
  (cd "${ROOT}/guava" && find src -name '*.java') > "${SRC}"
  SCOPE=()
fi
echo "typechecking $(wc -l < "${SRC}" | tr -d ' ') file(s)"

# Run from guava/, whose root holds no stray .java files: javac's implicit
# sourcepath would otherwise pick them up and report "bad source file".
cd "${ROOT}/guava"
set +e
"${CHECKERFRAMEWORK}/checker/bin/javac" \
  -J-Xss256m -J-Xmx8g \
  -processor "${PROCESSOR:-org.checkerframework.checker.mutability.MutabilityChecker}" \
  ${ASSUME_INITIALIZED:--AassumeInitialized} \
  ${EXTRA_ARGS:-} \
  "${SCOPE[@]}" \
  -Xmaxerrs 100000 -Xmaxwarns 100000 \
  -Alint=-all -XDignore.symbol.file ${WARN_FLAGS:--nowarn} \
  -cp "${CP}" -d /tmp/pico-out \
  "@${SRC}" > "${LOG}" 2>&1
status=$?
set -e

echo "exit=${status}  log=${LOG}"
tail -3 "${LOG}"
echo "checker crashes: $(grep -c 'The Checker Framework crashed' "${LOG}" || true)"
if grep -q "error: \[" "${LOG}"; then
  python3 /Users/aosenxiong/pico-artifact/guava-summarize-errors.py "${LOG}" | head -30
else
  echo "No checker diagnostics — if there are plain javac errors above, the"
  echo "classpath or sourcepath is wrong and the checker never ran."
fi
