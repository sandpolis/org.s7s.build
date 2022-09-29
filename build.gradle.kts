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
	kotlin("plugin.serialization") version "1.7.20"
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
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")
}

pluginBundle {
	website = "https://sandpolis.org"
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
			displayName = "Sandpolis Publish Plugin"
			description = "Applies publishing configuration for Sandpolis modules"
			implementationClass = "org.s7s.build.PublishPlugin"
		}
		getByName("org.s7s.build.instance") {
			id = "org.s7s.build.instance"
			displayName = "Sandpolis Instance Plugin"
			description = "Applies configuration specific to Sandpolis instance modules"
			implementationClass = "org.s7s.build.InstancePlugin"
		}
		getByName("org.s7s.build.codegen") {
			id = "org.s7s.build.codegen"
			displayName = "Sandpolis Code Generator Plugin"
			description = "Applies configuration for code generators"
			implementationClass = "org.s7s.build.CodegenPlugin"
		}
		getByName("org.s7s.build.module") {
			id = "org.s7s.build.module"
			displayName = "Sandpolis Module Plugin"
			description = "Applies configuration for Sandpolis modules"
			implementationClass = "org.s7s.build.ModulePlugin"
		}
		getByName("org.s7s.build.protobuf") {
			id = "org.s7s.build.protobuf"
			displayName = "Sandpolis Protocol Buffer Plugin"
			description = "Applies configuration for modules containing protobuf code"
			implementationClass = "org.s7s.build.ProtobufPlugin"
		}
		getByName("org.s7s.build.plugin") {
			id = "org.s7s.build.plugin"
			displayName = "Sandpolis Plugin Plugin"
			description = "Applies configuration for building Sandpolis plugins"
			implementationClass = "org.s7s.build.PluginPlugin"
		}
		getByName("org.s7s.build.jextract") {
			id = "org.s7s.build.jextract"
			displayName = "Sandpolis Jextract Plugin"
			description = "Applies Jextract configuration for Sandpolis modules"
			implementationClass = "org.s7s.build.JextractPlugin"
		}
	}
}
