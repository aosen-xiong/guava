#!/usr/bin/env bash
# Typecheck one tier of TypecheckedList.txt on its own, not the whole module.
#
# This is a quick check, not the claim. javac loads the rest of Guava from source on
# demand (-sourcepath -implicit:none), the checker sees some of those classes with raw
# supertypes, and listed files can report errors that the whole-module run does not.
# The claim is measured by check-module.sh.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
TIER="${1:-T2}"          # T1, T2, T3 = one tier of the claim, ALL = all three
case "$TIER" in
  T1) sel='^T1 ' ;;
  T2) sel='^T2 ' ;;
  T3) sel='^T3 ' ;;
  ALL) sel='^T[123] ' ;;
  *) echo "usage: $0 [T1|T2|T3|ALL]" >&2; exit 2 ;;
esac
mapfile -t FILES < <(grep -E "$sel" "${ROOT}/TypecheckedList.txt" \
  | sed -E 's/^T[123] //; s/[[:space:]]*#.*$//; s#^#src/com/google/common/#')
echo "target ${TIER}: ${#FILES[@]} files"
"${ROOT}/typecheck-mutability.sh" "${FILES[@]}"
