//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//

import com.google.protobuf.gradle.*
import java.nio.file.Files
import com.google.common.io.MoreFiles

plugins {
	id("java")
	id("com.google.protobuf")
}

sourceSets {
	main {
		java {
			srcDirs("src/gen/java")
		}
	}
}

protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc:3.14.0"
	}

	generatedFilesBaseDir = "$projectDir/src/gen/"

	generateProtoTasks {
		ofSourceSet("main").forEach { task ->
			task.builtins {
				remove("java")
				id("java") {
					option("lite")
				}
				if (System.getenv("S7S_BUILD_PROTO_CPP") == "1") {
					id("cpp") {
						option("lite")
					}
				}
				if (System.getenv("S7S_BUILD_PROTO_RUST") == "1") {
					id("rust")
				}
				if (System.getenv("S7S_BUILD_PROTO_SWIFT") == "1") {
					id("swift")
				}
				if (System.getenv("S7S_BUILD_PROTO_PYTHON") == "1") {
					id("python")
				}
			}

			// Move generated output
			task.doLast {
				file("src/gen/main").listFiles().forEach {
					val dest = file("src/gen").toPath().resolve(it.name)
					if (Files.exists(dest)) {
						MoreFiles.deleteRecursively(dest)
					}
					Files.move(it.toPath(), dest)
				}
				file("src/gen/main").delete()
			}
		}
	}
}

// Ignore errors in generated protobuf sources
tasks {
	javadoc {
		setFailOnError(false)
	}
}

// Create separate rust artifact
if (System.getenv("S7S_BUILD_PROTO_RUST") == "1") {
	tasks.register<Zip>("protoZipRust") {
		dependsOn("generateProto")

		from("src/gen/rust")
		archiveAppendix.set("rust")
	}
}

// Create separate swift artifact
if (System.getenv("S7S_BUILD_PROTO_SWIFT") == "1") {
	tasks.register<Zip>("protoZipSwift") {
		dependsOn("generateProto")

		from("src/gen/swift")
		archiveAppendix.set("swift")
	}
}

// Create separate c++ artifact
if (System.getenv("S7S_BUILD_PROTO_CPP") == "1") {
	tasks.register<Zip>("protoZipCpp") {
		dependsOn("generateProto")

		from("src/gen/cpp")
		archiveAppendix.set("cpp")
	}
}

// Create separate python artifact
if (System.getenv("S7S_BUILD_PROTO_PYTHON") == "1") {
	tasks.register<Zip>("protoZipPython") {
		dependsOn("generateProto")

		from("src/gen/python")
		archiveAppendix.set("python")
	}
}

// Add explicit dependency to supress Gradle warning
tasks.findByName("sourcesJar")?.dependsOn("generateProto")

tasks.findByName("clean")?.doLast {
	delete("src/gen")
}
