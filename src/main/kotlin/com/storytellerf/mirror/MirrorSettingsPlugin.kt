package com.storytellerf.mirror

import org.gradle.api.GradleException
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.initialization.Settings
import java.net.URI

/**
 * Adds the Maven mirror selected in Gradle properties after the settings file
 * has been evaluated, so repositories declared by the build are considered.
 */
class MirrorSettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        settings.gradle.settingsEvaluated(object : Action<Settings> {
            override fun execute(evaluatedSettings: Settings) {
                val mirrorUrl = resolveMirrorUrl(evaluatedSettings)
                if (mirrorUrl != null) {
                    addFirstIfAbsent(evaluatedSettings, mirrorUrl)
                }
            }
        })
    }

    private fun resolveMirrorUrl(settings: Settings): String? {
        val explicitUrl = settings.providers.gradleProperty(MIRROR_URL_PROPERTY)
            .orNull
            ?.trim()
            ?.takeIf(String::isNotEmpty)

        if (explicitUrl != null) return explicitUrl

        val selection = settings.providers.gradleProperty(MIRROR_PROPERTY)
            .orNull
            ?.trim()
            ?.lowercase()
            ?: return null

        if (selection.isEmpty() || selection == "none") return null

        return MIRRORS[selection]
            ?: throw GradleException(
                "Unknown $MIRROR_PROPERTY value '$selection'. " +
                    "Supported values: ${MIRRORS.keys.joinToString()}, none. " +
                    "Use $MIRROR_URL_PROPERTY for a custom Maven repository URL.",
            )
    }

    private fun addFirstIfAbsent(settings: Settings, mirrorUrl: String) {
        val repositories = settings.dependencyResolutionManagement.repositories
        val normalizedMirrorUrl = normalizeUrl(mirrorUrl)

        val alreadyPresent = repositories
            .filterIsInstance<MavenArtifactRepository>()
            .any { normalizeUrl(it.url.toString()) == normalizedMirrorUrl }

        if (alreadyPresent) return

        val mirror = repositories.maven {
            name = uniqueRepositoryName(repositories)
            url = URI(mirrorUrl)
        }

        // RepositoryHandler.maven adds at the end; move the new repository to the front.
        repositories.remove(mirror)
        repositories.addFirst(mirror)
    }

    private fun uniqueRepositoryName(repositories: RepositoryHandler): String {
        if (MIRROR_REPOSITORY_NAME !in repositories.names) return MIRROR_REPOSITORY_NAME

        return generateSequence(2) { it + 1 }
            .map { "$MIRROR_REPOSITORY_NAME$it" }
            .first { it !in repositories.names }
    }

    private fun normalizeUrl(url: String): String = url.trim().trimEnd('/')

    private companion object {
        const val MIRROR_PROPERTY = "mavenMirror"
        const val MIRROR_URL_PROPERTY = "mavenMirrorUrl"
        const val MIRROR_REPOSITORY_NAME = "GlobalMavenMirror"

        val MIRRORS = linkedMapOf(
            "aliyun" to "https://maven.aliyun.com/repository/public",
            "tencent" to "https://mirrors.cloud.tencent.com/nexus/repository/maven-public/",
            "huawei" to "https://repo.huaweicloud.com/repository/maven/",
        )
    }
}
