package no.nav.security.mock.oauth2.http

import mu.KotlinLogging
import java.io.File
import java.io.FileNotFoundException
import java.net.JarURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private val log = KotlinLogging.logger {}

private const val CLASSPATH_PREFIX = "classpath:"

internal class StaticAsset(
    val bytes: ByteArray,
    val contentType: String,
)

internal sealed interface ResourceLocation {
    val isEmpty: Boolean

    fun readText(): String?

    fun readAsset(relativePath: String): StaticAsset?

    companion object {
        fun of(location: String): ResourceLocation =
            if (location.startsWith(CLASSPATH_PREFIX)) {
                ClasspathLocation(location.removePrefix(CLASSPATH_PREFIX).trim('/'))
            } else {
                FileLocation(location)
            }
    }
}

private class FileLocation(
    private val path: String,
) : ResourceLocation {
    override val isEmpty: Boolean = path.isBlank()

    override fun readText(): String? =
        try {
            File(path).readText()
        } catch (e: FileNotFoundException) {
            null
        }

    override fun readAsset(relativePath: String): StaticAsset? {
        if (!relativePath.isRelativeAssetPath()) return null
        val directory = File(path).canonicalFile.toPath()
        val requestedFile = File(path, relativePath)
        return requestedFile.canonicalFile
            .takeIf { it.toPath().startsWith(directory) && it.isFile }
            ?.let { StaticAsset(it.readBytes(), contentTypeOf(requestedFile.toPath())) }
    }
}

private class ClasspathLocation(
    private val path: String,
) : ResourceLocation {
    private val classLoader: ClassLoader = Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader()

    override val isEmpty: Boolean = path.isBlank()

    override fun readText(): String? = findRegularFileUrl(path)?.readText()

    override fun readAsset(relativePath: String): StaticAsset? {
        if (!relativePath.isRelativeAssetPath()) return null
        return findRegularFileUrl("$path/$relativePath")?.let { StaticAsset(it.readBytes(), contentTypeOf(Paths.get(relativePath))) }
    }

    private fun findRegularFileUrl(resourcePath: String): URL? =
        classLoader
            .getResource(resourcePath)
            ?.takeIf { it.isRegularFile() && it.isReachedWithoutSymlinks(resourcePath) }

    private fun URL.isRegularFile(): Boolean =
        when (val connection = openConnection()) {
            is JarURLConnection -> connection.jarEntry?.isDirectory == false
            else -> protocol == "file" && File(toURI()).isFile
        }.also { isRegularFile ->
            if (!isRegularFile && protocol !in setOf("file", "jar")) {
                log.debug { "classpath resource $this is not served, only file and jar resources are supported" }
            }
        }

    private fun URL.isReachedWithoutSymlinks(resourcePath: String): Boolean =
        protocol != "file" ||
            generateSequence(File(toURI()).toPath()) { it.parent }
                .take(Paths.get(resourcePath).nameCount)
                .none { Files.isSymbolicLink(it) }
}

private fun String.isRelativeAssetPath(): Boolean = Paths.get(this).let { isNotBlank() && !it.isAbsolute && !it.startsWith("..") }

private fun contentTypeOf(path: Path): String = Files.probeContentType(path) ?: "application/octet-stream"
