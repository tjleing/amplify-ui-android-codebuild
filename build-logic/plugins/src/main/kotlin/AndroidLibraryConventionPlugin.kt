/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * This convention plugin configures an Android library module
 */
@Suppress("LocalVariableName")
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target.pluginManager) {
            apply("com.android.library")
            apply("org.jetbrains.kotlin.android")
            apply("amplify.android.ktlint")
        }

        val POM_GROUP: String by target

        with(target) {
            group = POM_GROUP
            extensions.configure<LibraryExtension> {
                configureAndroid(this)
            }

            tasks.withType<JavaCompile>().configureEach {
                options.compilerArgs.apply {
                    add("-Xlint:all")
                    add("-Werror")
                }
            }

            tasks.withType<Test>().configureEach {
                minHeapSize = "128m"
                maxHeapSize = "4g"
            }

            tasks.withType<KotlinCompile>().configureEach {
                kotlinOptions {
                    jvmTarget = JavaVersion.VERSION_11.toString()
                }
            }
        }
    }

    private fun Project.configureAndroid(extension: LibraryExtension) {
        val sdkVersionName = findProperty("VERSION_NAME") ?: rootProject.findProperty("VERSION_NAME")

        if (hasProperty("signingKeyId")) {
            println("Getting signing info from protected source.")
            extra["signing.keyId"] = findProperty("signingKeyId")
            extra["signing.password"] = findProperty("signingPassword")
            extra["signing.inMemoryKey"] = findProperty("signingInMemoryKey")
        }

        extension.apply {
            compileSdk = 34

            buildFeatures {
                buildConfig = true
            }

            defaultConfig {
                minSdk = 24
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                testInstrumentationRunnerArguments += "clearPackageData" to "true"
                consumerProguardFiles += rootProject.file("configuration/consumer-rules.pro")

                testOptions {
                    animationsDisabled = true
                    unitTests {
                        isIncludeAndroidResources = true
                    }
                }

                buildConfigField("String", "VERSION_NAME", "\"$sdkVersionName\"")
            }

            lint {
                warningsAsErrors = true
                abortOnError = true
                enable += listOf("UnusedResources")
                disable += listOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }

            // Needed when running integration tests. The oauth2 library uses relies on two
            // dependencies (Apache's httpcore and httpclient), both of which include
            // META-INF/DEPENDENCIES. Tried a couple other options to no avail.
            // More collisions occurred using JUnit, so also adding LICENSE-*.md files.
            packaging {
                resources.excludes.add("META-INF/DEPENDENCIES")
                resources.excludes.add("META-INF/LICENSE.md")
                resources.excludes.add("META-INF/LICENSE-notice.md")
            }

            buildFeatures {
                compose = true
            }

            composeOptions {
                kotlinCompilerExtensionVersion = "1.4.3"
            }
        }
    }
}
