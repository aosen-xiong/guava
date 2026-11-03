# PICO qualifier counts for a file list, telling PICO's @Immutable from Error Prone's.
#   python3 qualcount.py LIST [REV]
import re, subprocess, sys, collections
lst, rev = sys.argv[1], (sys.argv[2] if len(sys.argv) > 2 else "HEAD")
PICO = "org.checkerframework.checker.mutability.qual."
names = ["Assignable", "Immutable", "Readonly", "ReceiverDependentMutable", "Mutable", "PolyMutable", "MutabilityLost"]
tot = collections.Counter(); ep = 0; files = 0
for f in open(lst).read().split():
    s = subprocess.run(["git", "show", f"{rev}:guava/src/com/google/common/{f}"], capture_output=True, text=True).stdout
    files += 1
    body = "\n".join(x for x in s.split("\n") if not x.lstrip().startswith("import"))
    # Strings and comments in one pass, so that a "//" inside a string does not start a
    # comment and a qualifier named in a @CFComment string is not counted.
    STR_OR_COMMENT = r"\"(?:\\.|[^\"\\\n])*\"|'(?:\\.|[^'\\\n])+'|//[^\n]*|/\*.*?\*/"
    code = re.sub(STR_OR_COMMENT, lambda m: m.group(0) if m.group(0)[0] == '"' else "", body, flags=re.S)
    body = re.sub(STR_OR_COMMENT, "", body, flags=re.S)
    for n in names:
        fq = len(re.findall(r"@" + re.escape(PICO + n) + r"\b", body))
        simple = len(re.findall(r"(?<!\w)@" + n + r"\b", body))
        if n == "Immutable" and f"import {PICO}Immutable;" not in s:
            ep += simple; simple = 0
        tot[n] += fq + simple
    tot["SuppressWarnings:mutability"] += len(re.findall(r'"mutability(?::[^"]*)?"', re.sub(r"@AnnotatedFor\([^)]*\)", "", code)))
print(f"files {files}", " ".join(f"{n}={tot[n]}" for n in names + ["SuppressWarnings:mutability"]),
      f"total={sum(tot[n] for n in names)}", f"(Error Prone @Immutable excluded: {ep})")
