package com.storytellerf.mirror

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import kotlin.test.assertContains
import kotlin.test.assertEquals

class MirrorSettingsPluginFunctionalTest {
    @TempDir
    lateinit var projectDir: Path

    @Test
    fun `adds selected mirror before existing repositories`() {
        runBuild(properties = "mavenMirror=aliyun")

        assertEquals(
            listOf(
                "https://maven.aliyun.com/repository/public",
                "https://repo.maven.apache.org/maven2/",
            ),
            repositoryUrls(),
        )
    }

    @Test
    fun `does not add a duplicate mirror`() {
        runBuild(
            properties = "mavenMirror=aliyun",
            repositories = """
                mavenCentral()
                maven(url = "https://maven.aliyun.com/repository/public/")
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                "https://repo.maven.apache.org/maven2/",
                "https://maven.aliyun.com/repository/public/",
            ),
            repositoryUrls(),
        )
    }

    @Test
    fun `does nothing when mirror is disabled`() {
        runBuild(properties = "mavenMirror=none")

        assertEquals(
            listOf("https://repo.maven.apache.org/maven2/"),
            repositoryUrls(),
        )
    }

    @Test
    fun `explicit URL takes precedence over named mirror`() {
        runBuild(
            properties = """
                mavenMirror=aliyun
                mavenMirrorUrl=https://packages.example.test/maven
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                "https://packages.example.test/maven",
                "https://repo.maven.apache.org/maven2/",
            ),
            repositoryUrls(),
        )
    }

    @Test
    fun `rejects URL in named mirror property`() {
        val result = runBuild(
            properties = "mavenMirror=https://packages.example.test/maven",
            expectFailure = true,
        )

        assertContains(
            result.output,
            "Unknown mavenMirror value 'https://packages.example.test/maven'",
        )
        assertContains(
            result.output,
            "Use mavenMirrorUrl for a custom Maven repository URL.",
        )
    }

    private fun runBuild(
        properties: String,
        repositories: String = "mavenCentral()",
        expectFailure: Boolean = false,
    ): BuildResult {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            plugins {
                id("com.storytellerf.mirror")
            }

            dependencyResolutionManagement {
                repositories {
                    $repositories
                }
            }

            gradle.settingsEvaluated {
                val urls = dependencyResolutionManagement.repositories
                    .filterIsInstance<org.gradle.api.artifacts.repositories.MavenArtifactRepository>()
                    .joinToString(System.lineSeparator()) { it.url.toString() }
                file("repository-urls.txt").writeText(urls)
            }
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText("")

        val gradleUserHome = Path.of("build", "testkit", UUID.randomUUID().toString())
            .toAbsolutePath()
            .createDirectories()
        gradleUserHome.resolve("gradle.properties").writeText(properties)

        val runner = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments(
                "--gradle-user-home",
                gradleUserHome.toString(),
                "--stacktrace",
                "help",
            )

        return if (expectFailure) runner.buildAndFail() else runner.build()
    }

    private fun repositoryUrls(): List<String> =
        projectDir.resolve("repository-urls.txt").readLines()
}
