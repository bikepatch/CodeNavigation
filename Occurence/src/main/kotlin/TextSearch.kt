package com.example.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile

interface Occurrence {
    val file: Path
    val line: Int
    val offset: Int
}

private data class OccurrenceImpl(
    override val file: Path,
    override val line: Int,
    override val offset: Int
) : Occurrence

private val LOG: Logger = Logger.getLogger("TextSearch")

/**
 * Searches for [stringToSearch] inside all regular files in [directory].
 *
 * @param maxFileSizeBytes Skip files larger than this size (null = no limit)
 */
fun searchForTextOccurrences(
    stringToSearch: String,
    directory: Path,
    maxConcurrency: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(2),
    maxFileSizeBytes: Long? = null
): Flow<Occurrence> {
    require(stringToSearch.isNotEmpty()) { "stringToSearch must not be empty" }
    require(Files.exists(directory) && Files.isDirectory(directory)) { "directory must exist and be a directory" }

    val semaphore = Semaphore(maxConcurrency)

    return channelFlow {
        Files.walk(directory).use { paths ->
            paths
                .filter { it.isRegularFile() }
                .forEach { file ->

                    if (maxFileSizeBytes != null) {
                        val size = try {
                            file.fileSize()
                        } catch (t: Throwable) {
                            LOG.log(Level.WARNING, "Cannot read file size: $file (${t.message}), skipping file")
                            return@forEach
                        }

                        if (size > maxFileSizeBytes) {
                            LOG.log(
                                Level.WARNING,
                                "Skipping file (size $size > limit $maxFileSizeBytes bytes): $file"
                            )
                            return@forEach
                        }
                    }

                    launch(Dispatchers.IO) {
                        semaphore.withPermit {
                            try {
                                Files.newBufferedReader(file, StandardCharsets.UTF_8).use { reader ->
                                    var lineNumber = 0
                                    var line = reader.readLine()
                                    while (line != null) {
                                        lineNumber++
                                        var idx = 0
                                        while (true) {
                                            val found = line.indexOf(stringToSearch, idx)
                                            if (found < 0) break
                                            send(OccurrenceImpl(file, lineNumber, found))
                                            idx = found + 1
                                        }
                                        line = reader.readLine()
                                    }
                                }
                            } catch (t: Throwable) {
                                LOG.log(
                                    Level.WARNING,
                                    "Skipping unreadable file: $file (${t.javaClass.simpleName}: ${t.message})"
                                )
                            }
                        }
                    }
                }
        }
    }
}