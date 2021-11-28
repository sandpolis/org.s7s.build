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
	`kotlin-dsl`
	kotlin("plugin.serialization") version "1.5.20"
	id("java-gradle-plugin")
	id("maven-publish")
	id("com.gradle.plugin-publish") version "0.18.0"
	id("org.ajoberstar.grgit") version "4.1.0"
}

repositories {
	mavenLocal()
	gradlePluginPortal()
}

dependencies {

	// For protobuf plugin
	implementation("com.google.protobuf:protobuf-gradle-plugin:0.8.18")

	// For module plugin
	implementation("org.ajoberstar.grgit:grgit-core:4.1.0")

	// For codegen plugin
	implementation("com.squareup:javapoet:1.13.0")
	implementation("com.google.guava:guava:31.0.1-jre")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")
}

pluginBundle {
    website = "https://sandpolis.com"
    vcsUrl = "https://github.com/sandpolis/com.sandpolis.build"
    tags = listOf("sandpolis")
}

group = "com.sandpolis"

// Set project version according to the latest git tag
version = org.ajoberstar.grgit.Grgit.open {
	currentDir = project.getProjectDir().toString()
}.describe {
	tags = true
}?.replaceFirst("^v".toRegex(), "") ?: "0.0"

// Set publishing credentials
ext {
	set("gradle.publish.key", System.getenv("PLUGIN_PUBLISH_KEY"))
	set("gradle.publish.secret", System.getenv("PLUGIN_PUBLISH_SECRET"))
}

gradlePlugin {
	plugins {
		getByName("com.sandpolis.build.publish") {
			id = "com.sandpolis.build.publish"
			displayName = "Publish Plugin"
			description = "This plugin applies publishing configuration for Sandpolis modules"
			implementationClass = "com.sandpolis.build.PublishPlugin"
		}
		getByName("com.sandpolis.build.instance") {
			id = "com.sandpolis.build.instance"
			displayName = "Instance Plugin"
			description = "This plugin applies configuration specific to Sandpolis instance modules"
			implementationClass = "com.sandpolis.build.InstancePlugin"
		}
		getByName("com.sandpolis.build.codegen") {
			id = "com.sandpolis.build.codegen"
			displayName = "Code Generator Plugin"
			description = "This plugin applies configuration for code generators"
			implementationClass = "com.sandpolis.build.CodegenPlugin"
		}
		getByName("com.sandpolis.build.module") {
			id = "com.sandpolis.build.module"
			displayName = "Module Plugin"
			description = "This plugin applies configuration for all Sandpolis modules"
			implementationClass = "com.sandpolis.build.ModulePlugin"
		}
		getByName("com.sandpolis.build.protobuf") {
			id = "com.sandpolis.build.protobuf"
			displayName = "Protocol Buffer Plugin"
			description = "This plugin applies configuration for modules containing protobuf code"
			implementationClass = "com.sandpolis.build.ProtobufPlugin"
		}
		getByName("com.sandpolis.build.plugin") {
			id = "com.sandpolis.build.plugin"
			displayName = "Plugin Plugin"
			description = "This plugin applies configuration for building Sandpolis plugins"
			implementationClass = "com.sandpolis.build.PluginPlugin"
		}
	}
}
