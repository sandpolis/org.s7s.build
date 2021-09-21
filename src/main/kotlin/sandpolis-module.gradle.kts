//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//

import org.ajoberstar.grgit.Grgit

// Set project version according to the latest git tag
project.version = Grgit.open {
	currentDir = project.getProjectDir().toString()
}.describe {
	tags = true
}?.replaceFirst("^v".toRegex(), "") ?: "0.0.0"

// Configure GitHub Packages repositories
repositories {
	for (module in listOf(
		"core.foundation",
		"core.instance",
		"core.net",
		"core.clientserver",
		"plugin.desktop",
		"plugin.shell",
		"plugin.device",
		"plugin.snapshot")
	) {
		maven("https://maven.pkg.github.com/sandpolis/com.sandpolis.${module}") {
			credentials {
				username = System.getenv("GITHUB_ACTOR")
				password = System.getenv("GITHUB_TOKEN")
			}
		}
	}
}
