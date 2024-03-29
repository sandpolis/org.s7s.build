//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.build

import org.gradle.internal.os.OperatingSystem

plugins {
	id("maven-publish")
	id("signing")
}

publishing {
	publications {
		create<MavenPublication>(project.name.split(".").last()) {
			groupId = "org.s7s"
			artifactId = project.name.replace("org.s7s.", "")
			version = project.version.toString()

			// Use resolved versions instead of declared
			versionMapping {
				allVariants {
					fromResolutionResult()
				}
			}

			pom {
				name.set(project.name)
				description.set("${project.name} module")
				url.set("https://github.com/sandpolis/${project.name}")
				licenses {
					license {
						name.set("Mozilla Public License, Version 2.0")
						url.set("https://mozilla.org/MPL/2.0")
					}
				}
				developers {
					developer {
						id.set("cilki")
						name.set("Tyler Cook")
						email.set("tcc@sandpolis.com")
					}
				}
				scm {
					connection.set("scm:git:git://github.com/sandpolis/${project.name}.git")
					developerConnection.set("scm:git:ssh://git@github.com/sandpolis/${project.name}.git")
					url.set("https://github.com/sandpolis/${project.name}")
				}
			}

			// Special handling for platform-specific modules
			if (project.name == "org.s7s.instance.client.desktop") {
				artifact(tasks.named<Jar>("jar")) {
					if (OperatingSystem.current().isLinux()) {
						classifier = "linux"
					} else if (OperatingSystem.current().isMacOsX()) {
						classifier = "mac"
					} else if (OperatingSystem.current().isWindows()) {
						classifier = "windows"
					}
				}
			}

			// Special handling for plugin modules
			else if (project.name.startsWith("org.s7s.plugin")) {
				artifact(tasks.getByName("pluginArchive") as Zip)
			}

			// Standard handling for regular modules
			else {
				tasks.findByName("jar")?.let {
					from(components["java"])
				}
			}

			// Add any configured protobuf artifacts
			for (id in listOf("rust", "swift", "cpp", "python")) {
				if (System.getenv("S7S_BUILD_PROTO_${id.toUpperCase()}") == "1") {
					artifact(file("${buildDir}/generated-proto/main/${id}.zip")) {
						classifier = id
					}
				}
			}
		}
	}
	System.getenv("SONATYPE_USERNAME")?.let { user ->
		System.getenv("SONATYPE_PASSWORD")?.let { pass ->
			repositories {
				maven {
					name = "CentralStaging"
					url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
					credentials {
						username = user
						password = pass
					}
				}
			}
		}
	}
}

System.getenv("SIGNING_PGP_KEY")?.let { key ->
	System.getenv("SIGNING_PGP_PASSWORD")?.let { password ->
		signing {
			useInMemoryPgpKeys(key, password)
			publishing.publications.forEach {
				sign(it)
			}
		}
	}
}
