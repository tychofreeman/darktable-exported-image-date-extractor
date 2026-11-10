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
