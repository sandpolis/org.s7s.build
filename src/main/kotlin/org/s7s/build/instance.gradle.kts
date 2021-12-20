//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.build

import com.google.common.hash.Hashing;
import com.google.common.io.Files
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

@Serializable
data class BuildConfig(
	val platform: String,
	val timestamp: Long,
	val versions: VersionCfg,
	val dependencies: List<DependencyCfg>
)

@Serializable
data class VersionCfg(
	val instance: String,
	val gradle: String,
	val java: String? = null,
	val kotlin: String? = null,
	val rust: String? = null
)

@Serializable
data class DependencyCfg(
	val group: String,
	val artifact: String,
	val version: String,
	val classifier: String?,
	val hash: String
)

val writeBuildConfig by tasks.creating(DefaultTask::class) {

	doLast {
		val json = BuildConfig(

			// Build platform
			platform="${System.getProperty("os.name")} (${System.getProperty("os.arch")})",

			// Build time
			timestamp=System.currentTimeMillis(),

			// Version info
			versions=VersionCfg(

				// Instance version
				instance=project.version.toString(),

				// Gradle version
				gradle=project.getGradle().getGradleVersion(),

				// Java version
				java="${System.getProperty("java.version")} (${System.getProperty("java.vendor")})"
			),

			// Runtime dependencies
			dependencies=project
				.getConfigurations()
				.getByName("runtimeClasspath")
				.getResolvedConfiguration()
				.getResolvedArtifacts()
				.stream().map {
					DependencyCfg (
						group=if (it.moduleVersion.id.name.startsWith("org.s7s.")) "org.s7s" else it.moduleVersion.id.group,
						artifact=it.moduleVersion.id.name.replace("^com\\.sandpolis\\.".toRegex(), ""),
						version=it.moduleVersion.id.version,
						classifier=it.classifier,
						hash=Files.asByteSource(it.getFile()).hash(Hashing.sha256()).toString()
					)
				}.toList()
		)

		// Write object
		if (project.file("res").exists()) {
			project.file("res/org.s7s.build.json").writeText(Json.encodeToString(json))
		} else {
			project.file("src/gen/resources").mkdirs()
			project.file("src/gen/resources/org.s7s.build.json").writeText(Json.encodeToString(json))
		}
	}
}
tasks.findByName("processResources")?.dependsOn(writeBuildConfig)

// Add configuration for protobuf artifacts in non-Java instances
val proto by configurations.creating

// Extract protobuf downloaded archives
val assembleProto by tasks.creating(DefaultTask::class) {
	doLast {
		for (id in listOf("cpp", "python", "rust", "swift")) {
			proto.files.filter { it.name.endsWith("-${id}.zip") }.forEach { dep ->
				val module = dep.name.substringBefore("-")
				copy {
					from(zipTree(dep))
					into("src/gen/${id}/${module}")
				}
			}
		}
	}
}
tasks.findByName("assemble")?.dependsOn(assembleProto)

// Assemble the instance artifact and all dependencies into build/lib
tasks.findByName("jar")?.let {
	task<Sync>("assembleLib") {
		dependsOn(it)
		from(configurations.getByName("runtimeClasspath"))
		from(it)
		into("${buildDir}/lib")
	}
}
