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

## 2026-11-09: test files for classes that do not exist yet

Needed: tests written first for new classes `RawProfile` and `DateTaken`.

- `create_file` with the whole test content: the files were created, but the
  build then failed with "cannot find symbol" for both classes, so the first red
  was a compile error, not a failing assertion. Not tried: `create_file` with
  `test_for`, then `add_declaration` of each test, which may declare the stubs.

## 2026-11-09: imports left after replacing a whole class

`replace_declaration` of `Main` with a new body reported the imports it added
and removed, but left `import java.io.*;` and an unused
`import java.util.Optional;`. Removed by hand with `replace_text`.

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

## 2026-11-10: batch line numbers after a step that adds an import

A `batch` with `add_declaration` (which imported `java.util.stream.Stream`)
first and `replace_statements` by line after it was refused: "line 36 ... was
rewritten by step 1". Reordered with the declaration last. The import moved
every line down by one, but the lines themselves were not rewritten.

## 2026-11-10: `extract_function` indents the new method's body by 5

The body of the `carryOut` it extracted was indented 5 spaces, not 8. Fixed
with a `replace_declaration` of the same code.
