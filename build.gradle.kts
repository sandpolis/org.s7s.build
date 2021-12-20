//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//

plugins {
	`kotlin-dsl`
	kotlin("plugin.serialization") version "1.5.20"
	id("java-gradle-plugin")
	id("maven-publish")
	id("com.gradle.plugin-publish") version "0.18.0"
	id("org.ajoberstar.grgit") version "4.1.1"
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
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
}

pluginBundle {
	website = "https://sandpolis.com"
	vcsUrl = "https://github.com/sandpolis/org.s7s.build"
	tags = listOf("sandpolis")
}

group = "org.s7s"

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
		getByName("org.s7s.build.publish") {
			id = "org.s7s.build.publish"
			displayName = "Publish Plugin"
			description = "This plugin applies publishing configuration for Sandpolis modules"
			implementationClass = "org.s7s.build.PublishPlugin"
		}
		getByName("org.s7s.build.instance") {
			id = "org.s7s.build.instance"
			displayName = "Instance Plugin"
			description = "This plugin applies configuration specific to Sandpolis instance modules"
			implementationClass = "org.s7s.build.InstancePlugin"
		}
		getByName("org.s7s.build.codegen") {
			id = "org.s7s.build.codegen"
			displayName = "Code Generator Plugin"
			description = "This plugin applies configuration for code generators"
			implementationClass = "org.s7s.build.CodegenPlugin"
		}
		getByName("org.s7s.build.module") {
			id = "org.s7s.build.module"
			displayName = "Module Plugin"
			description = "This plugin applies configuration for all Sandpolis modules"
			implementationClass = "org.s7s.build.ModulePlugin"
		}
		getByName("org.s7s.build.protobuf") {
			id = "org.s7s.build.protobuf"
			displayName = "Protocol Buffer Plugin"
			description = "This plugin applies configuration for modules containing protobuf code"
			implementationClass = "org.s7s.build.ProtobufPlugin"
		}
		getByName("org.s7s.build.plugin") {
			id = "org.s7s.build.plugin"
			displayName = "Plugin Plugin"
			description = "This plugin applies configuration for building Sandpolis plugins"
			implementationClass = "org.s7s.build.PluginPlugin"
		}
	}
}
