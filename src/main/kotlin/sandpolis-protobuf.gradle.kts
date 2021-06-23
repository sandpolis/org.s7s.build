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

					// Post-process generated rust to fix import paths. Remove this when
					// https://github.com/stepancheg/rust-protobuf/issues/409 is fixed.
					val fixRustImports by tasks.creating(DefaultTask::class) {
						dependsOn(tasks.findByName("generateProto"))
						doLast {
							project.fileTree("gen/main/rust").forEach {
								it.writeText(it.readText()
									.replace("super::platform::OsType", "crate::core::foundation::platform::OsType")
									.replace("super::auth::KeyContainer", "crate::core::instance::auth::KeyContainer")
									.replace("super::auth::PasswordContainer", "crate::core::instance::auth::PasswordContainer")
									.replace("super::metatypes::InstanceType", "crate::core::instance::metatypes::InstanceType")
									.replace("super::metatypes::InstanceFlavor", "crate::core::instance::metatypes::InstanceFlavor"))
							}
						}
					}
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
	val protoZipRust by tasks.creating(Zip::class) {
		dependsOn("generateProto")
		dependsOn("fixRustImports")

		from("gen/main/rust")
		archiveAppendix.set("rust")
	}
}

// Create separate swift artifact
if (System.getenv("S7S_BUILD_PROTO_SWIFT") == "1") {
	val protoZipSwift by tasks.creating(Zip::class) {
		dependsOn("generateProto")

		from("gen/main/swift")
		archiveAppendix.set("swift")
	}
}

// Create separate c++ artifact
if (System.getenv("S7S_BUILD_PROTO_CPP") == "1") {
	val protoZipCpp by tasks.creating(Zip::class) {
		dependsOn("generateProto")

		from("gen/main/cpp")
		archiveAppendix.set("cpp")
	}
}

// Add explicit dependency to supress Gradle warning
tasks.findByName("sourcesJar")?.dependsOn("generateProto")
