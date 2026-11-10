# Friction log

Each time a java-refactor-mcp tool got in the way, or a change to Java was made
by hand: what was needed, the tool sequences tried, and why each failed. Append
new entries at the end.

## 2026-11-09: moving `Main` out of the default package

Needed: `Main` in `src/main/java` (default package) moved to
`com.cwfreeman.darktable`, so tests and new classes could live in a package.

- `move` name=Main package=com.cwfreeman.darktable: refused, "Main is in the
  default package, which no other package can import. Give it a package first."
- `replace_text` adding `package com.cwfreeman.darktable;` worked, but left the
  file in a directory that does not match its package; `move` without a name
  moves only files that are not Java under a source root.

Done by hand: `git mv` into `src/main/java/com/cwfreeman/darktable/`. A `move`
of a default-package type (adding the package line and moving the file) would
have done it in one step.
- Follow-up: tychofreeman/java-refactor-mcp#292.

## 2026-11-09: test files for classes that do not exist yet

Needed: tests written first for new classes `RawProfile` and `DateTaken`.

- `create_file` with the whole test content: the files were created, but the
  build then failed with "cannot find symbol" for both classes, so the first red
  was a compile error, not a failing assertion. Not tried: `create_file` with
  `test_for`, then `add_declaration` of each test, which may declare the stubs.
- Follow-up: tychofreeman/java-refactor-mcp#293.

## 2026-11-09: imports left after replacing a whole class

`replace_declaration` of `Main` with a new body reported the imports it added
and removed, but left `import java.io.*;` and an unused
`import java.util.Optional;`. Removed by hand with `replace_text`.
- Follow-up: not a tool problem. `Optional` was unused before the edit, and
  the new code still used `java.io.*` (`File`, `PrintStream`); the tool removes
  the imports its edit leaves unused. Not filed upstream.

## 2026-11-10: loops in `Main.run` to streams

Needed: three `for` loops in `Main.run` as stream pipelines.

- No tool turns a loop into a stream. Considered `extract_function` then
  `fold`, but `fold` matches a method's statements, not a loop's shape, so no
  sequence composes to it. The two simple loops (expanding paths, printing the
  skipped files) were replaced with `replace_statements`, after an
  `add_declaration` of the `filesFor` method the first one maps with.
- The third loop went through tools: `extract_function` of its body (which
  passed and returned `status`), two one-line `replace_statements` to return
  a success flag, `replace_statements` of the loop, `change_type` of the
  return to boolean, `remove_parameter status`, and two `move_parameter`.
  A loop-to-stream tool (`for` + `add` to `map`/`toList`, `for` + effect to
  `forEach`) would have done the first two in one step each.
- Follow-up: tychofreeman/java-refactor-mcp#294.

## 2026-11-10: batch line numbers after a step that adds an import

A `batch` with `add_declaration` (which imported `java.util.stream.Stream`)
first and `replace_statements` by line after it was refused: "line 36 ... was
rewritten by step 1". Reordered with the declaration last. The import moved
every line down by one, but the lines themselves were not rewritten.
- Follow-up: tychofreeman/java-refactor-mcp#295.

## 2026-11-10: `extract_function` indents the new method's body by 5

The body of the `carryOut` it extracted was indented 5 spaces, not 8. Fixed
with a `replace_declaration` of the same code.
- Follow-up: tychofreeman/java-refactor-mcp#296.

## 2026-11-10: naming a lambda as a closure

Needed: `.map(r -> carryOut(r, rename, out, err))` as `.map(carryOut)`, with
the lambda in a local `carryOut` that closes over `rename`, `out` and `err`.
One `extract_variable` should do it; it took four steps and four retries.

- `extract_variable` of the lambda refused: it "calls, assigns or changes a
  variable", and declaring it before the statement "moves it past
  plan.renames().stream()". Creating a lambda runs nothing, so nothing moves.
  Worked around by extracting `plan.renames().stream()` as `renames` first
  (the lambda then moves past no call), and inlining `renames` after.
- The extracted local's type was `Function<? super Rename, ? extends Boolean>`,
  and `.reduce(true, Boolean::logicalAnd)` then did not compile. Fixed in the
  same batch (check "end") with `replace_statements` of the declaration as
  `Function<RenamePlan.Rename, Boolean>`. The lambda's target type,
  `Function<Rename, Boolean>`, would have compiled.
- Naming the new declaration in the batch: `$2.start` was the
  `java.util.function.Function` import step 2 added (line 12), not the
  declaration, and `$2.end` the `.map` line. It took `$2.end-2`, and
  `$2.end-3` for `renames`, since `$1.start` then counted as rewritten by
  step 2's import (as in the batch entry above).
- Follow-up: tychofreeman/java-refactor-mcp#297 (the lambda refused), #298
  (the wildcard type), and a comment on #295 (the `$N` references).

## 2026-11-10: extracting the Renamer classes

Needed: `Main`'s `renameAll` and `carryOut` as an abstract `Renamer` (fields
`out`, `err`; `run`) with `DryRunRenamer` and `FileRenamer` each implementing
`carryOut`. Done through tools: `extract_function`, `move` (x2),
`add_parameter` + `function_to_method` (x2), `remove_parameter` (x6),
`push_down`, `fold`, `rename`, `convert_lambda`, `inline`,
`extract_variable`. Friction:

- **Parameter to field.** No tool turns a parameter into a read of a field of
  the same name and value. `remove_parameter out` refused while the body read
  it; done as `replace_expression` `out` to `this.out` at each read, then
  `remove_parameter`, then `replace_expression` back to `out` once the
  parameter was gone.
- **`move` indentation.** Both moved methods landed with their Javadoc and
  signature at 2 spaces and the extracted body at 5, in a class indented by 4.
  `replace_declaration` of one later kept the 2, so it took a `replace_text`
  of the whole method.
- **A batch's `move` order.** `move Main.renameAll` before `move Main.carryOut`
  refused (`carryOut` is private in `Main`); moving the callee first worked.
  The refusal could say so.
- **`$N` after an import change, again** (java-refactor-mcp#295): `rename` by
  line 18 after an `inline` that removed the `Function` import was refused as
  "rewritten by step 5"; reordered.
- **`batch` retry keeps each step's tool,** so reordering steps of different
  tools needs the whole batch resent; the refusal suggested `retry`.
- **`function_to_method`** warned that a null receiver now fails at the call;
  the receivers were `new Renamer(...)` and `this`, which cannot be null.
