package org.sonarsource.java.buildsrc.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.repositories
import org.gradle.process.ExecResult

import java.io.File

const val ruleApiVersion = "2.+"

abstract class FetchRuleMetadata : DefaultTask() {

    private fun addRuleApiToProjectConfig(): Configuration {
        project.repositories {
            maven {
                url = project.uri("https://repox.jfrog.io/repox/sonarsource-private-releases")
                authentication {
                    credentials {
                        val artifactoryUsername: String by project
                        val artifactoryPassword: String by project
                        username = artifactoryUsername
                        password = artifactoryPassword
                    }
                }
            }
        }

        val ruleApi = project.configurations.create("ruleApi")
        project.dependencies {
            ruleApi("com.sonarsource.rule-api:rule-api:$ruleApiVersion")
        }

        return ruleApi
    }

    internal fun executeRuleApi(arguments: List<String>): ExecResult {
        val ruleApi = addRuleApiToProjectConfig()
        return project.javaexec {
            classpath = project.files(ruleApi.resolve())
            args = arguments
            mainClass.set("com.sonarsource.ruleapi.Main")
            workingDir = project.projectDir
        }
    }

    abstract class FetchSpecificRulesMetadata : FetchRuleMetadata() {
        @get:Input
        val ruleKey: String by project

        @get:Input
        @get:Optional
        val branch: String? by project

        @TaskAction
        fun downloadMetadata() =
            executeRuleApi(listOf("generate", "-rule", ruleKey) + (branch?.let { listOf("-branch", it) } ?: emptyList()))
    }

    abstract class FetchAllRulesMetadata : FetchRuleMetadata() {
        @TaskAction
        fun downloadMetadata() = executeRuleApi(listOf("update"))
    }
}
