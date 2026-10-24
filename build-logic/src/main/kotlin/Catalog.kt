import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

private fun Project.libs() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun Project.catalogVersion(name: String): String = libs().findVersion(name).get().requiredVersion

fun Project.catalogLibrary(name: String) = libs().findLibrary(name).get()
