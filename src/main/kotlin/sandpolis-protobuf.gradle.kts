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

plugins {
	id("java")
	id("com.google.protobuf")
}

sourceSets {
	main {
		java {
			srcDirs("src/main/proto")
			srcDirs("gen/main/java")
		}
	}
}

protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc:3.14.0"
	}

	generatedFilesBaseDir = "$projectDir/gen/"

	tasks {
		clean {
			delete(generatedFilesBaseDir)
		}
	}
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
		dependsOn("fixRustImports")

		from("gen/main/rust")
		archiveAppendix.set("rust")
	}
}

// Create separate swift artifact
if (System.getenv("S7S_BUILD_PROTO_SWIFT") == "1") {
	tasks.register<Zip>("protoZipSwift") {
		dependsOn("generateProto")

		from("gen/main/swift")
		archiveAppendix.set("swift")
	}
}

// Create separate c++ artifact
if (System.getenv("S7S_BUILD_PROTO_CPP") == "1") {
	tasks.register<Zip>("protoZipCpp") {
		dependsOn("generateProto")

		from("gen/main/cpp")
		archiveAppendix.set("cpp")
	}
}

// Add explicit dependency to supress Gradle warning
tasks.findByName("sourcesJar")?.dependsOn("generateProto")
