//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.build

import org.ajoberstar.grgit.Grgit
import org.gradle.plugins.ide.eclipse.model.EclipseModel

// Set project version according to the latest git tag
project.version = Grgit.open {
	currentDir = project.getProjectDir().toString()
}.describe {
	tags = true
}?.replaceFirst("^v".toRegex(), "") ?: "0.0.0"

// Add repository configuration
repositories {
	mavenLocal()
	mavenCentral()
}

// Apply Java configuration if needed
if (pluginManager.hasPlugin("java")) {

	pluginManager.apply("eclipse")
	pluginManager.apply("idea")

	configure<JavaPluginExtension> {
		modularity.inferModulePath.set(true)

		withJavadocJar()
		withSourcesJar()

		toolchain {
			languageVersion.set(JavaLanguageVersion.of(17))
		}
	}

	// Configure unit testing
	tasks.withType<Test> {
		useJUnitPlatform()

		testLogging {
			setExceptionFormat("full")
		}
	}

	// Configure Eclipse plugin
	configure<EclipseModel> {
		project {
			name = project.name
			comment = project.name
		}
	}

	// Clear eclipse bin directory on "clean"
	tasks.findByName("clean")?.doLast {
		delete("bin")
	}

	// Configure module version
	tasks.withType<JavaCompile> {
		options.javaModuleVersion.set(provider { project.version as String })
	}
}
