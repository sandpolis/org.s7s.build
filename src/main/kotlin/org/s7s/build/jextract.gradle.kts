//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package org.s7s.build

import org.gradle.internal.os.OperatingSystem

plugins {
	id("java-library")
}

open class JExtractExtension {
	lateinit var invocations: Map<String, List<String>>
}

val extension = project.extensions.create("jextract", JExtractExtension::class)

fun find_header(path: String): String? {

	if (OperatingSystem.current().isLinux()) {

		for (f in listOf(
			File("/usr/include/${path}"),
			File("/usr/include/x86_64-linux-gnu/${path}")
		)) {
			if (f.exists()) {
				return f.getAbsolutePath()
			}
		}

		// Search the filesystem as a last resort
		for (f in File("/usr/include").walkBottomUp().filter { it.path.endsWith("/${path}") }) {
			return f.getAbsolutePath()
		}
	}

	if (OperatingSystem.current().isWindows()) {

		for (f in listOf(
			File("C:/Program Files (x86)/Microsoft Visual Studio/2019/Professional/SDK/ScopeCppSDK/vc15/SDK/include/um/${path}"),
			File("C:/Program Files (x86)/Windows Kits/10/Include/10.0.18362.0/um/${path}")
		)) {
			if (f.exists()) {
				return f.getAbsolutePath()
			}
		}

		// Search the filesystem as a last resort
		for (f in File("C:/Program Files (x86)").walkBottomUp().filter { it.path.endsWith("/${path}") }) {
			return f.getAbsolutePath()
		}
	}

	if (OperatingSystem.current().isMacOsX()) {

		for (f in listOf(
			File("/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/usr/include/${path}")
		)) {
			if (f.exists()) {
				return f.getAbsolutePath()
			}
		}

		// Search the filesystem as a last resort
		for (f in File("C:/Program Files (x86)").walkBottomUp().filter { it.path.endsWith("/${path}") }) {
			return f.getAbsolutePath()
		}
	}

	return null
}

sourceSets {
	main {
		java {
			srcDirs("src/gen/java")
		}
	}
}

val jextract by tasks.creating(DefaultTask::class) {
	outputs.dir("src/gen/java")

	doLast {
		for ((header, invocation) in extension.invocations) {
			find_header(header)?.let { path ->
				exec {
					commandLine(invocation)
					args(path)
				}
			}
		}
	}
}

tasks.findByName("compileJava")?.dependsOn(jextract)

tasks.findByName("clean")?.doLast {
	delete("src/gen")
}