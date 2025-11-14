# Occurrence Search (IntelliJ Plugin)

A simple IntelliJ IDEA toolwindow that searches for a text string inside all files under a directory.
The search runs concurrently using Kotlin coroutines and streams results into the UI as soon as they are found.

### ✨ Features

- ✅ Concurrent directory scan (bounded coroutines)
- ✅ Responsive UI — search runs fully in background
- ✅ Live results streaming
- ✅ Overlapping match support (`"aa"` in `"aaa"` → 0,1)
- ✅ Skips unreadable files (logged)
- ✅ Optional file size limit
- ✅ Pretty results list (`relative/path.kt   line:offset`)

---

### ▶️ How to Run

Fork the project and then clone your version:

```bash
git clone https://github.com/yourGithub/OccurrenceSearchPlugin.git
cd OccurrenceSearchPlugin
```

Run the plugin in a sandbox IntelliJ IDEA:
```bash
./gradlew runIde
```

(Windows: gradlew.bat runIde; Or you can simply run it with gradle toolkit inside IntelliJ Idea)

A new IntelliJ instance will start with the plugin installed.

Open the toolwindow:
```sql
View → Tool Windows → Occurrence Search
```

Enter:

Directory — absolute path to search in

Search for — text to find

Click Start search to begin or Cancel search to stop it.

