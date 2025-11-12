# Occurrence Search (Kotlin + Coroutines + Flow)

A lightweight, concurrent text search utility that parses files under a directory
and emits text match occurrences as a Kotlin `Flow`.

### ✨ Features

- ✅ Concurrent file scanning (bounded thread count)
- ✅ Coroutine-based (`channelFlow`)
- ✅ Streaming I/O — no loading whole files into memory
- ✅ Overlapping match support (`"aa"` in `"aaa"` → 0,1,2)
- ✅ UTF-8 reading with graceful error handling
- ✅ Skips unreadable files (logged)
- ✅ Optional `maxFileSizeBytes` to skip huge files

---

### Function signature

```kotlin
fun searchForTextOccurrences(
    stringToSearch: String,
    directory: Path,
    maxConcurrency: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(2),
    maxFileSizeBytes: Long? = null
): Flow<Occurrence>