package no.nav.security.mock.oauth2.e2e

import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.jarOf
import no.nav.security.mock.oauth2.testutils.withClasspath
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class StaticAssetsIntegrationTest {
    private val client = client()

    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `request to static asset should return file from static asset directory`() {
        val dir = File("./src/test/resources/static")
        val server = MockOAuth2Server(OAuth2Config(staticAssetsPath = dir.canonicalPath)).apply { start() }
        client.get(server.url("/static/test.txt")).asClue {
            it.code shouldBe 200
            it.headers["content-type"] shouldBe "text/plain"
        }
        client.get(server.url("/static/test.css")).asClue {
            it.code shouldBe 200
            it.headers["content-type"] shouldBe "text/css"
        }
        client.get(server.url("/static/test.js")).asClue {
            it.code shouldBe 200
            it.headers["content-type"] shouldBeIn listOf("text/javascript", "application/javascript")
        }
        client.get(server.url("/static//test.txt")).asClue {
            it.code shouldBe 200
        }
    }

    @Test
    fun `static assets must not include files in sibling directories with the same prefix`() {
        val staticDir = Files.createDirectory(temporaryDirectory.resolve("static"))
        val siblingDir = Files.createDirectory(temporaryDirectory.resolve("static-private"))
        Files.writeString(siblingDir.resolve("secret.txt"), "outside")
        val server = MockOAuth2Server(OAuth2Config(staticAssetsPath = staticDir.toString())).apply { start() }

        try {
            client.get(server.url("/static/..%2Fstatic-private%2Fsecret.txt")).use {
                it.code shouldBe 404
            }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `static assets must not serve directories`() {
        val staticDir = Files.createDirectory(temporaryDirectory.resolve("static"))
        Files.writeString(Files.createDirectory(staticDir.resolve("nested")).resolve("file.txt"), "nested")
        val server = MockOAuth2Server(OAuth2Config(staticAssetsPath = staticDir.toString())).apply { start() }

        try {
            client.get(server.url("/static/nested/file.txt")).use { it.code shouldBe 200 }
            client.get(server.url("/static/nested")).use { it.code shouldBe 404 }
            client.get(server.url("/static/")).use { it.code shouldBe 404 }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `static assets must not follow symlinks outside their directory`() {
        val staticDir = Files.createDirectory(temporaryDirectory.resolve("static"))
        val outside = Files.writeString(temporaryDirectory.resolve("secret.txt"), "outside")
        Files.createSymbolicLink(staticDir.resolve("link.txt"), outside)
        val server = MockOAuth2Server(OAuth2Config(staticAssetsPath = staticDir.toString())).apply { start() }

        try {
            client.get(server.url("/static/link.txt")).use {
                it.code shouldBe 404
            }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `static assets follow symlinks inside their directory`() {
        val staticDir = Files.createDirectory(temporaryDirectory.resolve("static"))
        val dataDir = Files.createDirectory(staticDir.resolve("..2026_01_01"))
        Files.writeString(dataDir.resolve("test.txt"), "inside")
        Files.createSymbolicLink(staticDir.resolve("..data"), staticDir.relativize(dataDir))
        Files.createSymbolicLink(staticDir.resolve("test.txt"), staticDir.relativize(staticDir.resolve("..data/test.txt")))
        val server = MockOAuth2Server(OAuth2Config(staticAssetsPath = staticDir.toString())).apply { start() }

        try {
            client.get(server.url("/static/test.txt")).use {
                it.code shouldBe 200
                it.body.string() shouldBe "inside"
            }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `static assets use the requested file name for the content type of a symlink`() {
        val staticDir = Files.createDirectory(temporaryDirectory.resolve("static"))
        Files.writeString(staticDir.resolve("stored.txt"), "body { color: red; }")
        Files.createSymbolicLink(staticDir.resolve("style.css"), staticDir.relativize(staticDir.resolve("stored.txt")))
        val server = MockOAuth2Server(OAuth2Config(staticAssetsPath = staticDir.toString())).apply { start() }

        try {
            client.get(server.url("/static/style.css")).use {
                it.code shouldBe 200
                it.header("Content-Type") shouldBe "text/css"
                it.body.string() shouldBe "body { color: red; }"
            }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `request to static asset should return file from classpath directory`() {
        val server = MockOAuth2Server(OAuth2Config(staticAssetsPath = "classpath:static")).apply { start() }

        try {
            client.get(server.url("/static/test.css")).use {
                it.code shouldBe 200
                it.header("Content-Type") shouldBe "text/css"
                it.body.string() shouldBe File("./src/test/resources/static/test.css").readText()
            }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `classpath static assets must not serve directories or files outside their directory`() {
        val server = MockOAuth2Server(OAuth2Config(staticAssetsPath = "classpath:static")).apply { start() }

        try {
            client.get(server.url("/static/..%2Flogin.example.html")).use { it.code shouldBe 404 }
            client.get(server.url("/static/%2Flogin.example.html")).use { it.code shouldBe 404 }
            client.get(server.url("/static/does-not-exist.txt")).use { it.code shouldBe 404 }
            client.get(server.url("/static/")).use { it.code shouldBe 404 }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `request to static asset should return file from a jar on the classpath`() {
        val jar =
            jarOf(
                temporaryDirectory.resolve("assets.jar"),
                mapOf(
                    "web/static/style.css" to "body { color: red; }",
                    "web/static/css/nested/app.css" to "p { color: blue; }",
                ),
            )

        withClasspath(jar) {
            val server = MockOAuth2Server(OAuth2Config(staticAssetsPath = "classpath:web/static")).apply { start() }
            try {
                client.get(server.url("/static/style.css")).use {
                    it.code shouldBe 200
                    it.header("Content-Type") shouldBe "text/css"
                    it.body.string() shouldBe "body { color: red; }"
                }
                client.get(server.url("/static/css/nested/app.css")).use {
                    it.code shouldBe 200
                    it.header("Content-Type") shouldBe "text/css"
                    it.body.string() shouldBe "p { color: blue; }"
                }
                client.get(server.url("/static/css/nested")).use { it.code shouldBe 404 }
                client.get(server.url("/static/")).use { it.code shouldBe 404 }
                client.get(server.url("/static/..%2Fstatic")).use { it.code shouldBe 404 }
            } finally {
                server.shutdown()
            }
        }
    }

    @Test
    fun `classpath static assets must not follow symlinks`() {
        val classpathRoot = Files.createDirectory(temporaryDirectory.resolve("classes"))
        val staticDir = Files.createDirectory(classpathRoot.resolve("static"))
        Files.writeString(staticDir.resolve("test.txt"), "inside")
        val outside = Files.writeString(temporaryDirectory.resolve("secret.txt"), "outside")
        Files.createSymbolicLink(staticDir.resolve("link.txt"), outside)

        withClasspath(classpathRoot) {
            val server = MockOAuth2Server(OAuth2Config(staticAssetsPath = "classpath:static")).apply { start() }
            try {
                client.get(server.url("/static/test.txt")).use { it.code shouldBe 200 }
                client.get(server.url("/static/link.txt")).use { it.code shouldBe 404 }
            } finally {
                server.shutdown()
            }
        }
    }

    @Test
    fun `classpath static assets must not follow a symlinked directory with the same name`() {
        val classpathRoot = Files.createDirectory(temporaryDirectory.resolve("classes"))
        val outsideStatic = Files.createDirectories(temporaryDirectory.resolve("outside/static"))
        Files.writeString(outsideStatic.resolve("secret.txt"), "outside")
        Files.createSymbolicLink(classpathRoot.resolve("static"), outsideStatic)

        withClasspath(classpathRoot) {
            val server = MockOAuth2Server(OAuth2Config(staticAssetsPath = "classpath:static")).apply { start() }
            try {
                client.get(server.url("/static/secret.txt")).use { it.code shouldBe 404 }
            } finally {
                server.shutdown()
            }
        }
    }

    @Test
    fun `classpath static assets must not follow a symlinked subdirectory`() {
        val classpathRoot = Files.createDirectory(temporaryDirectory.resolve("classes"))
        val staticDir = Files.createDirectory(classpathRoot.resolve("static"))
        val outsideCss = Files.createDirectories(temporaryDirectory.resolve("outside/css"))
        Files.writeString(outsideCss.resolve("app.css"), "outside")
        Files.createSymbolicLink(staticDir.resolve("css"), outsideCss)

        withClasspath(classpathRoot) {
            val server = MockOAuth2Server(OAuth2Config(staticAssetsPath = "classpath:static")).apply { start() }
            try {
                client.get(server.url("/static/css/app.css")).use { it.code shouldBe 404 }
            } finally {
                server.shutdown()
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["classpath:", "classpath:/", "classpath: "])
    fun `classpath static assets path without a directory is rejected`(path: String) {
        shouldThrow<IllegalArgumentException> {
            MockOAuth2Server(OAuth2Config(staticAssetsPath = path))
        }
    }

    @Test
    fun `blank static assets path is rejected`() {
        shouldThrow<IllegalArgumentException> {
            MockOAuth2Server(OAuth2Config(staticAssetsPath = ""))
        }
    }
}
