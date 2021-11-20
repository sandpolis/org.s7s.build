//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.build

import org.gradle.internal.os.OperatingSystem

plugins {
	id("maven-publish")
	id("signing")
}

publishing {
	publications {
		create<MavenPublication>(project.name.split(".").last()) {
			groupId = "com.sandpolis"
			artifactId = project.name.replace("com.sandpolis.", "")
			version = project.version.toString()

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

			// Special handling for certain modules
			if (project.name == "com.sandpolis.client.lifegem" || project.name == "com.sandpolis.core.foreign") {
				artifact(tasks.named<Jar>("jar")) {
					if (OperatingSystem.current().isLinux()) {
						classifier = "linux"
					} else if (OperatingSystem.current().isMacOsX()) {
						classifier = "mac"
					} else if (OperatingSystem.current().isWindows()) {
						classifier = "windows"
					}
				}
			} else if (project.name.startsWith("com.sandpolis.plugin")) {
				tasks.findByName("pluginArchive")?.let {
					artifact(it as Zip)
				}
			} else {
				tasks.findByName("jar")?.let {
					from(components["java"])
				}
			}

			for (id in listOf("rust", "swift", "cpp", "python")) {
				if (file("${buildDir}/generated-proto/main/${id}.zip").exists()) {
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
