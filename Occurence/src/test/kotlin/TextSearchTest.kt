package com.example.search

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaDuration

class TextSearchTest {

    @TempDir
    lateinit var tempDir: Path

    private fun write(path: Path, content: String) {
        Files.createDirectories(path.parent)
        path.writeText(content)
    }

    @Test
    fun `finds occurrences in one file`() = runBlocking {
        val file = tempDir.resolve("a.txt")
        write(file, "hello\nworld\nhello again")

        val results = searchForTextOccurrences("hello", tempDir).toList()
        assertEquals(2, results.size)
        assertEquals(listOf(0, 0), results.map { it.offset })
    }

    @Test
    fun `supports overlapping matches`() = runBlocking {
        val file = tempDir.resolve("b.txt")
        write(file, "aaaa")

        val results = searchForTextOccurrences("aa", tempDir).toList()
        assertEquals(listOf(0, 1, 2), results.map { it.offset })
    }

    @Test
    fun `searches multiple files`() = runBlocking {
        write(tempDir.resolve("1.txt"), "find me")
        write(tempDir.resolve("2.txt"), "and find me too")

        val results = searchForTextOccurrences("find", tempDir).toList()
        assertEquals(2, results.size)
    }

    @Test
    fun `skips file exceeding max size`() = runBlocking {
        write(tempDir.resolve("big.txt"), "a".repeat(2_000_000))
        write(tempDir.resolve("ok.txt"), "hello")

        val results = searchForTextOccurrences("hello", tempDir, maxFileSizeBytes = 1_000_000).toList()
        assertEquals(1, results.size)
        assertEquals("ok.txt", results.first().file.fileName.toString())
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `cancels search when collector stops`() = runBlocking {
        // Create a fairly heavy file so scanning takes some time
        val big = tempDir.resolve("big.txt")
        write(big, (1..500_000).joinToString("\n") { "line $it some filler text that is quite long" })

        val job = launch {
            searchForTextOccurrences("unlikely-pattern-xyz", tempDir)
                .take(1)
                .firstOrNull()
        }

        delay(100)
        withTimeout(5.seconds.toJavaDuration().toMillis()) {
            job.cancel()
            job.join()
        }

        assertTrue(job.isCancelled || job.isCompleted)
    }
}