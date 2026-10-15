#!/usr/bin/env bash
# Typecheck the targeted file set (TypecheckedList.txt), not the whole module.
#
# The evaluation is defined by a file set that reaches ZERO errors — the same
# contract jdk/.../java/util/TypecheckedList.txt uses in the artifact. Driving
# the whole 622-file module to zero is not a realistic target and was never the
# plan; see guava-annotation-strategy.md section 10.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
TIER="${1:-T2}"          # T1 = regression baseline, T2 = the deliverable, ALL = both
case "$TIER" in
  T1) sel='^T1 ' ;;
  T2) sel='^T2 ' ;;
  ALL) sel='^T[12] ' ;;
  *) echo "usage: $0 [T1|T2|ALL]" >&2; exit 2 ;;
esac
mapfile -t FILES < <(grep -E "$sel" "${ROOT}/TypecheckedList.txt" \
  | sed -E 's/^T[12] //; s/[[:space:]]*#.*$//; s#^#src/com/google/common/#')
echo "target ${TIER}: ${#FILES[@]} files"
"${ROOT}/typecheck-mutability.sh" "${FILES[@]}"
