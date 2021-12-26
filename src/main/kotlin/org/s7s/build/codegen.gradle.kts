//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.build

import com.squareup.javapoet.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.lang.model.SourceVersion
import javax.lang.model.element.Modifier.*

plugins {
    id("java")
}

sourceSets {
    main {
        java {
            srcDirs("src/gen/java")
        }
    }
}

@Serializable
data class AttributeSpec(
    val name: String,
    val type: String,
    val description: String? = null,
    val id: Boolean = true,
    val osquery: String? = null,
    val immutable: Boolean? = false,
    val list: Boolean? = false
)

@Serializable
data class DocumentSpec(
    val collection: Boolean = true,
    val attributes: List<AttributeSpec>
)

val rootName = project.name.split(".").last().capitalize() + "Oids"

class OidClass(val parent: OidClass?, val name: String, val spec: DocumentSpec?) {

    private var children = HashMap<String, OidClass>()

    fun oid(): String {
        if (parent != null && spec != null) {
            if (spec.collection) {
                return "${parent.oid()}/${name}(null)"
            } else {
                return "${parent.oid()}/${name}"
            }
        }

        return ""
    }

    fun generateRust(): String {
        return ""
    }

    fun generateSwift(): String {
        return ""
    }

    fun generateJava(parent: TypeSpec.Builder? = null): TypeSpec {

        val t: TypeSpec.Builder

        // Root special case
        if (parent == null) {

            t = TypeSpec.classBuilder(rootName).addModifiers(PUBLIC)

            // Add root field
            t.addField(
                FieldSpec
                    .builder(ClassName.get(project.name + ".state", rootName), "root", PRIVATE, STATIC, FINAL)
                    .initializer("new ${rootName}()")
                    .build()
            )

            // Add root method
            t.addMethod(
                MethodSpec
                    .methodBuilder(rootName)
                    .addModifiers(PUBLIC, STATIC)
                    .returns(ClassName.get(project.name + ".state", rootName))
                    .addStatement("return root")
                    .build()
            )
        } else {

            val classPackage = oid().replace("(null)", "").split("/").filter { it != "" }.map { it + "Oid" }.joinToString(".")

            t = TypeSpec
                .classBuilder(name + "Oid")
                .addModifiers(PUBLIC, STATIC, FINAL)

            // Add OID field
            t.addField(FieldSpec.builder(ClassName.get("org.s7s.core.instance.state.oid", "Oid"), "_oid", PUBLIC /*,FINAL*/).build())

            // Add constructor
            t.addMethod(
                MethodSpec
                    .constructorBuilder()
                    .addModifiers(PRIVATE)
                    .addParameter(String::class.java, "path")
                    .addStatement("_oid = Oid.of(\"\$L:\" + path)", project.name)
                    .build()
            )

            // Add OID field to parent
            parent.addField(
                FieldSpec
                    .builder(
                        ClassName.bestGuess("${project.name}.state.${rootName}.${classPackage}"),
                        name,
                        PUBLIC,
                        FINAL
                    )
                    .initializer("new \$L(\$L\"/\$L\")", name + "Oid", if (path.count { it == '/'} > 2) "_oid.pathString() + " else "" , name)
                    .build()
            )

            // Process attributes
            spec?.let {
                it.attributes.forEach { attr ->

                    // Validate name
                    if (!SourceVersion.isIdentifier(attr.name)) {
                        throw RuntimeException("Invalid name: " + attr.name)
                    }

                    // Create fields
                    t.addField(
                        FieldSpec
                            .builder(
                                ClassName.get("org.s7s.core.instance.state.oid", "Oid"),
                                attr.name, PUBLIC, FINAL
                            )
                            .addJavadoc(attr.description ?: "")
                            .initializer(CodeBlock.of("_oid.relative(\"\$L\")", attr.name))
                            .build()
                    )

                    t.addField(
                        FieldSpec
                            .builder(
                                ClassName.get("org.s7s.core.instance.state.oid", "Oid"),
                                attr.name.toUpperCase(), PUBLIC, FINAL, STATIC
                            )
                            .addJavadoc(attr.description ?: "")
                            .initializer(CodeBlock.of("Oid.of(\"\$L:\$L\")", project.name, this.oid() + "/" + attr.name))
                            .build()
                    )
                }
            }

            if (spec!!.collection) {

                parent.addMethod(
                    MethodSpec
                        .methodBuilder(name)
                        .addModifiers(PUBLIC)
                        .addParameter(String::class.java, "id")
                        .returns(ClassName.bestGuess("${project.name}.state.${rootName}.${classPackage}"))
                        .addStatement(
                            "return new \$L(this.toString() + \"/\$L/\" + id)",
                            name + "Oid",
                            name
                        )
                        .build()
                )
            }
        }

        // Generate all children
        children.forEach { (_, v) ->
            t.addType(v.generateJava(t))
        }

        return t.build()
    }

    fun add(name: String, document: DocumentSpec): OidClass {

        if (name.contains("//")
            || name.contains("**")
            || name.endsWith("/")
            || name.startsWith("*")
        ) {
            throw RuntimeException("Illegal name: " + name)
        }

        if (children.containsKey(name.split("/").first())) {
            children[name.split("/").first()]!!.add(name.substringAfter("/"), document)
        } else {
            if (name.contains("/")) {
                children[name.split("/").first()] = OidClass(this, name.split("/").first(), document).add(name.substringAfter("/"), document)
            } else {
                children[name.split("/").first()] = OidClass(this, name.split("/").first(), document)
            }
        }

        return this
    }
}

project.afterEvaluate {

    if (project.file("src/main/json").exists()) {
        val generateOids by tasks.creating(DefaultTask::class) {

            doLast {

                val root = OidClass(null, "/", null)

                // Load the specification (sorted to ensure the top of the tree is processed first)
                project.fileTree("src/main/json").files.sortedBy{ it.absolutePath }.forEach {
                    root.add(it.absolutePath.substringAfter("src/main/json/").substringBefore(".json"), Json.decodeFromString<DocumentSpec>(it.readText()))
                }

                // Write Java class
                JavaFile.builder(project.name + ".state", root.generateJava())
                    .addFileComment("This source file was automatically generated by the Sandpolis codegen plugin.")
                    .skipJavaLangImports(true).build().writeTo(project.file("src/gen/java"))

                // Write Rust struct
                //project.file("src/gen/rust/oid.rs").writeString(root.generateRust())

                // Write Swift class
                //project.file("src/gen/rust/oid.swift").writeString(root.generateSwift())
            }
        }

        // Generated sources are required for compilation
        tasks.findByName("compileJava")?.dependsOn(generateOids)

        // Temporarily required because generateProto deletes src/gen/java
        project.tasks.findByName("generateProto")?.let {
            generateOids.dependsOn(it)
        }

        // Remove generated sources in clean task
        tasks.findByName("clean")?.doLast {
            delete(file("src/gen/java"))
        }
    }
}
