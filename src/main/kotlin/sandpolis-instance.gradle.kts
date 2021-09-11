//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

plugins {
	id("sandpolis-module")
}

@Serializable
data class BuildConfig(
	val build_platform: String,
	val build_timestamp: Long,
	val instance_version: String,
	val gradle_version: String,
	val java_version: String? = null,
	val kotlin_version: String? = null,
	val rust_version: String? = null
)

val writeBuildConfig by tasks.creating(DefaultTask::class) {

	doLast {
		val json = BuildConfig(

			// Build platform
			build_platform="${System.getProperty("os.name")} (${System.getProperty("os.arch")})",

			// Build time
			build_timestamp=System.currentTimeMillis(),

			// Instance version
			instance_version=project.version.toString(),

			// Gradle version
			gradle_version=project.getGradle().getGradleVersion(),

			// Java version
			java_version="${System.getProperty("java.version")} (${System.getProperty("java.vendor")})"
		)

		// Module dependencies
		/*props.setProperty("build.dependencies", project
			.getConfigurations()
			.getByName("runtimeClasspath")
			.getResolvedConfiguration()
			.getResolvedArtifacts().stream().map {
				it.getModuleVersion().getId()
			}.map {
				it.getGroup() + ":" + it.getName() + ":" + it.getVersion()
			}.collect(Collectors.joining(",")))*/

		// Write object
		if (project.file("res").exists()) {
			project.file("res/build.json").writeText(Json.encodeToString(json))
		} else {
			project.file("src/gen/resources").mkdirs()
			project.file("src/gen/resources/build.json").writeText(Json.encodeToString(json))
		}
	}
}

tasks.findByName("processResources")?.dependsOn(writeBuildConfig)
