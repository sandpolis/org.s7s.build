//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//

import com.google.common.base.CaseFormat
import com.squareup.javapoet.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.lang.model.SourceVersion
import javax.lang.model.element.Modifier.*

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
    val name: String,
    val attributes: List<AttributeSpec>
)

class OidClass(val path: String) {

    private val rootName = project.name.split(".").last().capitalize() + "Oids"
    private var children = HashMap<String, OidClass>()
    private var spec: DocumentSpec? = null

    fun generate(parent: TypeSpec.Builder? = null): TypeSpec {

        val t: TypeSpec.Builder

        // Root special case
        if (path == "/") {

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
            val pathUpper = path.split("/").filter { it != "*" && it != "" }
                .map { CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, "${it}_oid") }
            val className = pathUpper.takeLast(1).first()
            val classPackage = if (pathUpper.size > 1) "." + pathUpper.dropLast(1).joinToString(".") else ""

            t = TypeSpec
                .classBuilder(className)
                .addModifiers(PUBLIC, STATIC)
                .superclass(ClassName.get("com.sandpolis.core.instance.state.oid", "Oid"))

            // Add constructor
            t.addMethod(
                MethodSpec
                    .constructorBuilder()
                    .addModifiers(PRIVATE)
                    .addParameter(String::class.java, "path")
                    .addStatement("super(\"\$L\", path.replaceAll(\"^/+\", \"\").split(\"/\"))", project.name)
                    .build()
            )

            // Add OID field to parent
            parent?.addField(
                FieldSpec
                    .builder(
                        ClassName.get("${project.name}.state.${rootName}${classPackage}", className),
                        className.removeSuffix("Oid").toLowerCase(),
                        PUBLIC,
                        FINAL
                    )
                    .initializer("new \$L(\$L\"/\$L\")", className, if (path.count { it == '/'} > 2) "pathString() + " else "" , className.removeSuffix("Oid").toLowerCase())
                    .build()
            )

            spec?.let {
                processAttributes(t, it)
            }

            if (path.endsWith("/*")) {

                parent?.addMethod(
                    MethodSpec
                        .methodBuilder(className.removeSuffix("Oid").toLowerCase())
                        .addModifiers(PUBLIC)
                        .addParameter(String::class.java, "id")
                        .returns(ClassName.get("${project.name}.state.${rootName}${classPackage}", className))
                        .addStatement(
                            "return new \$L(this.toString() + \"/\$L/\" + id)",
                            className,
                            className.removeSuffix("Oid").toLowerCase()
                        )
                        .build()
                )
            }
        }

        children.forEach { (_, v) ->
            t.addType(v.generate(t))
        }

        return t.build()
    }

    fun add(document: DocumentSpec): OidClass {

        if (document.name.contains("//")
            || document.name.contains("**")
            || document.name.endsWith("/")
            || document.name.startsWith("*")
            || !document.name.startsWith("/")
        ) {
            throw RuntimeException("Illegal name: " + document.name)
        }

        if (path == document.name) {
            // Base case
            spec = document
        } else {
            val first = document.name.drop(path.length).replace("/*", "*").split("/").first { !it.isEmpty() }

            if (!children.contains(first)) {
                if (path == "/") {
                    children[first] = OidClass("/" + first.replace("*", "/*")).add(document)
                } else {
                    children[first] = OidClass("${path}/" + first.replace("*", "/*")).add(document)
                }
            } else {
                children[first]?.add(document)
            }
        }

        return this
    }
}

fun processAttributes(oidClass: TypeSpec.Builder, document: DocumentSpec) {

    document.attributes.forEach { attr ->

        // Validate name
        if (!SourceVersion.isIdentifier(attr.name)) {
            throw RuntimeException("Invalid name: " + attr.name)
        }

        // Create fields
        oidClass.addField(
            FieldSpec
                .builder(
                    ClassName.get("com.sandpolis.core.instance.state.oid", "Oid"),
                    attr.name, PUBLIC, FINAL
                )
                .addJavadoc(attr.description ?: "")
                .initializer(CodeBlock.of("relative(\"\$L\")", attr.name))
                .build()
        )

        oidClass.addField(
            FieldSpec
                .builder(
                    ClassName.get("com.sandpolis.core.instance.state.oid", "Oid"),
                    attr.name.toUpperCase(), PUBLIC, FINAL, STATIC
                )
                .addJavadoc(attr.description ?: "")
                .initializer(CodeBlock.of("Oid.of(\"\$L:\$L\")", project.name, document.name + "/" + attr.name))
                .build()
        )
    }
}

project.afterEvaluate {

    val specification = project.file("state.json")

    if (specification.exists()) {
        val generateOids by tasks.creating(DefaultTask::class) {

            doLast {

                // Load the specification
                val spec = Json.decodeFromString<List<DocumentSpec>>(specification.readText())

                // Convert spec into tree for easier class generation
                val root = OidClass("/")
                spec.forEach(root::add)

                // Write root class
                JavaFile.builder(project.name + ".state", root.generate())
                    .addFileComment("This source file was automatically generated by the Sandpolis codegen plugin.")
                    .skipJavaLangImports(true).build().writeTo(project.file("gen/main/java"))
            }
        }

        // Setup task dependencies
        project.tasks.findByName("generateProto")?.let {
            generateOids.dependsOn(it)
        }
        project.tasks.findByName("compileJava")?.dependsOn(generateOids)

        // Remove generated sources in clean task
        val cleanGeneratedSources by tasks.creating(Delete::class) {
            delete(project.file("gen/main/java"))
        }
        project.tasks.findByName("clean")?.dependsOn(cleanGeneratedSources)
    }
}
