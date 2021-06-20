//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//

plugins {
	id("maven-publish")
	id("signing")
}

// Locate additional artifact tasks if they exist
val protoZipRust = tasks.findByName("protoZipRust") as? Zip
val protoZipSwift = tasks.findByName("protoZipSwift") as? Zip
val protoZipCpp = tasks.findByName("protoZipCpp") as? Zip

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			groupId = "com.sandpolis"
			artifactId = project.name.toString().replace("com.sandpolis.", "")
			version = project.version.toString()

			if (project.name.startsWith("com.sandpolis.plugin")) {
				artifact(project.tasks.getByName("pluginArchive"))
			} else {
				from(components["java"])
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
		}

		if (protoZipRust != null) {
			create<MavenPublication>("mavenRust") {
				groupId = "com.sandpolis"
				artifactId = project.name.toString().replace("com.sandpolis.", "") + "-rust"
				version = project.version.toString()

				artifact(protoZipRust)
			}
		}

		if (protoZipSwift != null) {
			create<MavenPublication>("mavenSwift") {
				groupId = "com.sandpolis"
				artifactId = project.name.toString().replace("com.sandpolis.", "") + "-swift"
				version = project.version.toString()

				artifact(protoZipSwift)
			}
		}

		if (protoZipCpp != null) {
			create<MavenPublication>("mavenCpp") {
				groupId = "com.sandpolis"
				artifactId = project.name.toString().replace("com.sandpolis.", "") + "-cpp"
				version = project.version.toString()

				artifact(protoZipCpp)
			}
		}
	}
	repositories {
		maven {
			name = "GitHubPackages"
			url = uri("https://maven.pkg.github.com/sandpolis/${project.name}")
			credentials {
				username = System.getenv("GITHUB_ACTOR")
				password = System.getenv("GITHUB_TOKEN")
			}
		}
	}
}
signing {
	useInMemoryPgpKeys(System.getenv("SIGNING_PGP_KEY") ?: "", System.getenv("SIGNING_PGP_PASSWORD") ?: "")
	publishing.publications.forEach {
		sign(it)
	}
}
