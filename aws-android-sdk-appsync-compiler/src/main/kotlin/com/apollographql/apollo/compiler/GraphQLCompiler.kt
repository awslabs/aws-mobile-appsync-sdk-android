/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.CodeGenerationIR
import com.apollographql.apollo.compiler.ir.ScalarType
import com.apollographql.apollo.compiler.ir.TypeDeclaration
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeName
import com.squareup.moshi.Moshi
import java.io.File

class GraphQLCompiler {
  private val moshi = Moshi.Builder().build()
  private val irAdapter = moshi.adapter(CodeGenerationIR::class.java)

  fun write(args: Arguments) {
    val ir = irAdapter.fromJson(args.irFile.readText())!!
    val irPackageName = args.outputPackageName ?: args.irFile.absolutePath.formatPackageName()
    val fragmentsPackage = if (irPackageName.isNotEmpty()) "$irPackageName.fragment" else "fragment"
    val typesPackage = if (irPackageName.isNotEmpty()) "$irPackageName.type" else "type"
    val customTypeMap = args.customTypeMap.supportedTypeMap(ir.typesUsed)
    val context = CodeGenerationContext(
        reservedTypeNames = emptyList(),
        typeDeclarations = ir.typesUsed,
        fragmentsPackage = fragmentsPackage,
        typesPackage = typesPackage,
        customTypeMap = customTypeMap,
        nullableValueType = args.nullableValueType,
        generateAccessors = args.generateAccessors,
        ir = ir,
        useSemanticNaming = args.useSemanticNaming,
        generateModelBuilder = args.generateModelBuilder
    )
    ir.writeJavaFiles(
        context = context,
        outputDir = args.outputDir,
        outputPackageName = args.outputPackageName
    )
  }

  private fun CodeGenerationIR.writeJavaFiles(context: CodeGenerationContext, outputDir: File,
      outputPackageName: String?) {
    fragments.forEach {
      val typeSpec = it.toTypeSpec(context.copy())
      JavaFile.builder(context.fragmentsPackage, typeSpec).build().writeTo(outputDir)
    }

    typesUsed.supportedTypeDeclarations().forEach {
      val typeSpec = it.toTypeSpec(context.copy())
      JavaFile.builder(context.typesPackage, typeSpec).build().writeTo(outputDir)
    }

    if (context.customTypeMap.isNotEmpty()) {
      val typeSpec = CustomEnumTypeSpecBuilder(context.copy()).build()
      JavaFile.builder(context.typesPackage, typeSpec).build().writeTo(outputDir)
    }

    operations.map { OperationTypeSpecBuilder(it, fragments, context.useSemanticNaming) }
        .forEach {
          val packageName = outputPackageName ?: it.operation.filePath.formatPackageName()
          val typeSpec = it.toTypeSpec(context.copy())
          JavaFile.builder(packageName, typeSpec).build().writeTo(outputDir)
        }
  }

  private fun List<TypeDeclaration>.supportedTypeDeclarations() =
      filter { it.kind == TypeDeclaration.KIND_ENUM || it.kind == TypeDeclaration.KIND_INPUT_OBJECT_TYPE }

  private fun Map<String, String>.supportedTypeMap(typeDeclarations: List<TypeDeclaration>): Map<String, String> {
    val idScalarTypeMap = ScalarType.ID.name to (this[ScalarType.ID.name] ?: ClassNames.STRING.toString())
    return typeDeclarations.filter { it.kind == TypeDeclaration.KIND_SCALAR_TYPE }
        .associate { it.name to (this[it.name] ?: ClassNames.OBJECT.toString()) }
        .plus(idScalarTypeMap)
        .plus("AWSDate" to ClassNames.STRING.toString())
        .plus("AWSTime" to ClassNames.STRING.toString())
        .plus("AWSDateTime" to ClassNames.STRING.toString())
        .plus("AWSTimestamp" to TypeName.LONG.box().toString())
        .plus("AWSEmail" to ClassNames.STRING.toString())
        .plus("AWSJSON" to ClassNames.STRING.toString())
        .plus("AWSURL" to ClassNames.STRING.toString())
        .plus("AWSPhone" to ClassNames.STRING.toString())
        .plus("AWSIPAddress" to ClassNames.STRING.toString())
  }

  companion object {
    const val FILE_EXTENSION = "graphql"
    val OUTPUT_DIRECTORY = listOf("generated", "source", "appsync")
    const val APOLLOCODEGEN_VERSION = "0.17.0-alpha.13"
  }

  data class Arguments(
      val irFile: File,
      val outputDir: File,
      val customTypeMap: Map<String, String>,
      val nullableValueType: NullableValueType,
      val generateAccessors: Boolean,
      val useSemanticNaming: Boolean,
      val generateModelBuilder: Boolean,
      val outputPackageName: String?
  )
}
