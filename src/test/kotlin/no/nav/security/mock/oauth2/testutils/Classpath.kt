package no.nav.security.mock.oauth2.testutils

import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

fun jarOf(
    jar: Path,
    files: Map<String, String>,
): Path =
    jar.apply {
        JarOutputStream(Files.newOutputStream(this)).use { out ->
            files.keys
                .flatMap { name -> name.split("/").dropLast(1).runningReduce { parent, child -> "$parent/$child" } }
                .distinct()
                .forEach { directory -> out.putNextEntry(JarEntry("$directory/")).also { out.closeEntry() } }
            files.forEach { (name, content) ->
                out.putNextEntry(JarEntry(name))
                out.write(content.toByteArray())
                out.closeEntry()
            }
        }
    }

fun <T> withClasspath(
    entry: Path,
    block: () -> T,
): T {
    val thread = Thread.currentThread()
    val original = thread.contextClassLoader
    return URLClassLoader(arrayOf(entry.toUri().toURL()), null).use { classLoader ->
        thread.contextClassLoader = classLoader
        try {
            block()
        } finally {
            thread.contextClassLoader = original
        }
    }
}
