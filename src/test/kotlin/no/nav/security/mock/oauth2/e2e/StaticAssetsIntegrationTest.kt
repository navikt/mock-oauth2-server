package no.nav.security.mock.oauth2.e2e

import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
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
            it.headers["content-type"] shouldBe "text/javascript"
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
    fun `blank static assets path is rejected`() {
        shouldThrow<IllegalArgumentException> {
            MockOAuth2Server(OAuth2Config(staticAssetsPath = ""))
        }
    }
}
