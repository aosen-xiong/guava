#!/usr/bin/env bash
# Typechecks the whole Guava module and reports errors per tier of TypecheckedList.txt.
#
# check-target.sh checks only one tier and lets javac load the rest of Guava from
# source on demand (-sourcepath -implicit:none). The checker then sees some of those
# lazily loaded classes with raw supertypes or wrong type-variable bounds, so the
# result depends on which files are checked and in what order. Checking the whole
# module loads nothing on demand. Slower, but the counts are stable.
#
#   ./check-module.sh            # T1 and T2 counts, per-file T2 errors
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
"${ROOT}/typecheck-mutability.sh" > /dev/null 2>&1 || true
python3 - "${ROOT}/compile.log" "${ROOT}/TypecheckedList.txt" <<'PY'
import re, sys
tiers = {}
for line in open(sys.argv[2]):
    m = re.match(r"^(T1|T2)\s+(\S+\.java)", line)
    if m:
        tiers[m.group(2)] = m.group(1)
log = open(sys.argv[1]).read()
counts = {"T1": 0, "T2": 0}
per = {}
for m in re.finditer(r"src/com/google/common/(\S+\.java):\d+: error: \[", log):
    f = m.group(1)
    if f in tiers:
        counts[tiers[f]] += 1
        per[(tiers[f], f)] = per.get((tiers[f], f), 0) + 1
crashes = log.count("The Checker Framework crashed")
print(f"module: errors={len(re.findall(r'error: \[', log))} crashes={crashes}")
print(f"T1 errors={counts['T1']}  T2 errors={counts['T2']}")
for (t, f), n in sorted(per.items()):
    print(f"  {t} {f} {n}")
PY
