//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//

import com.google.common.io.MoreFiles
import com.google.protobuf.gradle.*
import java.nio.file.Files

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
		artifact = "com.google.protobuf:protoc:3.18.0"
	}

	generatedFilesBaseDir = "${buildDir}/generated-proto"

	generateProtoTasks {
		ofSourceSet("main").forEach { task ->
			task.builtins {
				getByName("java") {
					option("lite")
					setOutputSubDir("java.zip")
				}

				if (System.getenv("S7S_BUILD_PROTO_CPP") == "1") {
					id("cpp") {
						option("lite")
						setOutputSubDir("cpp.zip")
					}
				}

				if (System.getenv("S7S_BUILD_PROTO_PYTHON") == "1") {
					id("python") {
						setOutputSubDir("python.zip")
					}
				}

				if (System.getenv("S7S_BUILD_PROTO_RUST") == "1") {
					id("rust") {
						setOutputSubDir("rust.zip")
					}
				}

				if (System.getenv("S7S_BUILD_PROTO_SWIFT") == "1") {
					id("swift") {
						setOutputSubDir("swift.zip")
					}
				}
			}

			// Extract generated archives to src/gen
			task.doLast {
				for (id in listOf("java", "cpp", "python", "rust", "swift")) {
					if (file("${buildDir}/generated-proto/main/${id}.zip").exists()) {
						copy {
							from(zipTree(file("${buildDir}/generated-proto/main/${id}.zip")))
							into("src/gen/${id}")
						}
					}
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

// Add explicit dependency to supress Gradle warning
tasks.findByName("sourcesJar")?.dependsOn("generateProto")

tasks.findByName("clean")?.doLast {
	delete("src/gen")
}
