/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.apollographql.apollo.gradle

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import com.apollographql.android.VersionKt
import com.google.common.collect.ImmutableList
import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.NodePlugin
import org.gradle.api.DomainObjectCollection
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile

import javax.inject.Inject

class ApolloPlugin implements Plugin<Project> {
    private static final String NODE_VERSION = "6.7.0"
    public static final String TASK_GROUP = "apollo"
    private static final String AMAZON_DEP_GROUP = "com.amazonaws"
    private static final String RUNTIME_DEP_NAME = "aws-android-sdk-appsync-runtime"

    private Project project
    private final FileResolver fileResolver

    @Inject
    public ApolloPlugin(FileResolver fileResolver) {
        this.fileResolver = fileResolver
    }

    @Override
    void apply(Project project) {
        this.project = project
        if (project.plugins.hasPlugin(AppPlugin) || project.plugins.hasPlugin(LibraryPlugin) || project.plugins.hasPlugin(
                JavaPlugin)) {
            applyApolloPlugin()
        } else {
            throw new IllegalArgumentException(
                    "Apollo plugin couldn't be applied. The Android or Java plugin must be configured first")
        }
    }

    private void applyApolloPlugin() {
        setupNode()
        project.extensions.create(ApolloExtension.NAME, ApolloExtension)
        createSourceSetExtensions()

        def gradleVersion = project.getGradle().getGradleVersion()
        def depType = "implementation"
        if (gradleVersion.startsWith("2")) {
            depType = "compile"
        }
        def depSet = project.configurations.getByName(depType).dependencies

        project.getGradle().addListener(new DependencyResolutionListener() {
            @Override
            void beforeResolve(ResolvableDependencies resolvableDependencies) {
                def appsyncRuntimeDep = depSet.find { dep ->
                    dep.group == AMAZON_DEP_GROUP
                    dep.name == RUNTIME_DEP_NAME
                }
                if (appsyncRuntimeDep != null && appsyncRuntimeDep.version != VersionKt.VERSION && !appsyncRuntimeDep instanceof ProjectDependency) {
                    throw new GradleException(
                            "apollo-runtime version ${appsyncRuntimeDep.version} isn't compatible with the apollo-gradle-plugin version ${VersionKt.VERSION}")
                }
                if (System.getProperty("apollographql.skipRuntimeDep") != "true" && appsyncRuntimeDep == null) {
                    depSet.add(project.dependencies.create("$AMAZON_DEP_GROUP:$RUNTIME_DEP_NAME:$VersionKt.VERSION"))
                }
            }

            @Override
            void afterResolve(ResolvableDependencies resolvableDependencies) {
                project.getGradle().removeListener(this)
            }
        })

        project.tasks.create(ApolloCodeGenInstallTask.NAME, ApolloCodeGenInstallTask.class)
        addApolloTasks()
    }

    private void addApolloTasks() {
        Task apolloIRGenTask = project.task("generateApolloIR")
        apolloIRGenTask.group(TASK_GROUP)
        Task apolloClassGenTask = project.task("generateApolloClasses")
        apolloClassGenTask.group(TASK_GROUP)
        getVariants().all { v ->
            addVariantTasks(v, apolloIRGenTask, apolloClassGenTask, v.sourceSets)
        }
    }

    private void addVariantTasks(Object variant, Task apolloIRGenTask, Task apolloClassGenTask, Collection<?> sourceSets) {
        ImmutableList.Builder<File> sourceSetFiles = ImmutableList.builder();
        sourceSets.each { sourceSet ->
            if (sourceSet.hasProperty('graphql')) {
                sourceSetFiles.add(sourceSet.graphql.getFiles())
            }
        }
        ImmutableList<File> files = sourceSetFiles.build()
        if (!files.isEmpty()) {
            ApolloIRGenTask variantIRTask = createApolloIRGenTask(variant.name, sourceSets)
            ApolloClassGenTask variantClassTask = createApolloClassGenTask(variant.name)
            variant.registerJavaGeneratingTask(variantClassTask, variantClassTask.outputDir)
            apolloIRGenTask.dependsOn(variantIRTask)
            apolloClassGenTask.dependsOn(variantClassTask)
        }
    }

    private void setupNode() {
        project.plugins.apply NodePlugin
        NodeExtension nodeConfig = project.extensions.findByName("node") as NodeExtension
        nodeConfig.download = true
        nodeConfig.version = NODE_VERSION
    }

    private ApolloIRGenTask createApolloIRGenTask(String sourceSetOrVariantName, Collection<Object> sourceSets) {
        String taskName = String.format(ApolloIRGenTask.NAME, sourceSetOrVariantName.capitalize())
        ApolloIRGenTask task = project.tasks.create(taskName, ApolloIRGenTask) {
            group = TASK_GROUP
            description = "Generate an IR file using apollo-codegen for ${sourceSetOrVariantName.capitalize()} GraphQL queries"
            dependsOn(ApolloCodeGenInstallTask.NAME)
            sourceSets.each { sourceSet ->
                if (sourceSet.hasProperty('graphql')) {
                    inputs.files(sourceSet.graphql).skipWhenEmpty()
                }
            }
        }

        ImmutableList.Builder<String> sourceSetNamesList = ImmutableList.builder();
        sourceSets.each { sourceSet -> sourceSetNamesList.add(sourceSet.name) }
        task.init(sourceSetOrVariantName, sourceSetNamesList.build(), project.apollo)
        return task
    }

    private ApolloClassGenTask createApolloClassGenTask(String name) {
        String taskName = String.format(ApolloClassGenTask.NAME, name.capitalize())
        ApolloClassGenTask task = project.tasks.create(taskName, ApolloClassGenTask) {
            group = TASK_GROUP
            description = "Generate Android classes for ${name.capitalize()} GraphQL queries"
            dependsOn(getProject().getTasks().findByName(String.format(ApolloIRGenTask.NAME, name.capitalize())));
            source = project.tasks.findByName(String.format(ApolloIRGenTask.NAME, name.capitalize())).outputFolder
            include "**${File.separatorChar}*API.json"
        }
        task.init(name, project.apollo)
        return task
    }

    private void createSourceSetExtensions() {
        project.android.sourceSets.main { sourceSet ->
            def sourceDirectorySet =
                project.objects.sourceDirectorySet('graphql', "main GraphQL source")
            sourceSet.extensions.add('graphql', sourceDirectorySet)
            sourceSet.extensions.configure('graphql', { graphQlSet ->
                graphQlSet.srcDir("src/main/graphql")
                graphQlSet.include('**/*.graphql', '**/schema.json')
            })
        }
    }

    private DomainObjectCollection<BaseVariant> getVariants() {
        return project.android.hasProperty(
                'libraryVariants') ? project.android.libraryVariants : project.android.applicationVariants
    }
}

