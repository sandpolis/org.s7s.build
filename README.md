## Sandpolis Build Module
This module contains common build logic for all Sandpolis modules. It should be consumed as a git submodule named `buildSrc` in the individual module repositories or in the root [repository](https://github.com/sandpolis/sandpolis).

Gradle will automatically compile the build plugins and provide them via the standard plugins block:
```
plugins {
	id("sandpolis-module")
}
```
