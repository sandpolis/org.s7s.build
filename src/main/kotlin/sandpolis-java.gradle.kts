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
	id("java")
	id("eclipse")
	id("idea")
}

// Configure Java plugin
java {
	modularity.inferModulePath.set(true)

	withJavadocJar()
	withSourcesJar()

	toolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}
}

// Configure unit testing
tasks.test {
	useJUnitPlatform()

	testLogging {
		setExceptionFormat("full")
	}
}

// Configure Eclipse plugin
eclipse {
	project {
		name = project.name
		comment = project.name
	}
}

// Configure module version
tasks.withType<JavaCompile> {
	options.javaModuleVersion.set(provider { project.version as String })
}

// Clear eclipse bin directory on "clean"
tasks.findByName("clean")?.doLast {
	delete("bin")
}
