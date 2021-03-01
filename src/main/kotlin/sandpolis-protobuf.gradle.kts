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
				if (findProject(":com.sandpolis.agent.nano") != null) {
					id("cpp") {
						option("lite")
					}
				}
				if (findProject(":com.sandpolis.agent.micro") != null) {
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
					tasks.findByName("assemble")?.dependsOn(fixRustImports)
				}
				if (findProject(":com.sandpolis.client.lockstone") != null) {
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
