import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.dontchaoda"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.dontchaoda"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

abstract class PrintJUnitSummaryTask : DefaultTask() {

    @TaskAction
    fun printSummary() {
        val reportType = project.findProperty("summaryType")?.toString().orEmpty()
        val resultsDirPath = project.findProperty("summaryDir")?.toString().orEmpty()
        if (resultsDirPath.isBlank()) return

        val resultsDir = File(resultsDirPath)
        if (!resultsDir.exists()) {
            println("\n=== ${if (reportType.isBlank()) "Test" else reportType} Summary ===")
            println("- No test results found.")
            return
        }

        val xmlFiles = resultsDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "xml" && it.name.startsWith("TEST-") }
            .sortedBy { it.name }
            .toList()

        if (xmlFiles.isEmpty()) {
            println("\n=== ${if (reportType.isBlank()) "Test" else reportType} Summary ===")
            println("- No XML test reports found.")
            return
        }

        println("\n=== ${if (reportType.isBlank()) "Test" else reportType} Summary ===")

        val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        xmlFiles.forEach { xmlFile ->
            val document = documentBuilder.parse(xmlFile)
            val testCases = document.getElementsByTagName("testcase")
            for (index in 0 until testCases.length) {
                val testCase = testCases.item(index) as? Element ?: continue
                val methodName = testCase.getAttribute("name")
                val className = testCase.getAttribute("classname").substringAfterLast('.')
                val status = when {
                    testCase.getElementsByTagName("failure").length > 0 -> "FAILED"
                    testCase.getElementsByTagName("error").length > 0 -> "ERROR"
                    testCase.getElementsByTagName("skipped").length > 0 -> "SKIPPED"
                    else -> "PASSED"
                }

                println("- [$status] $className: ${methodName.replace('_', ' ')}")
            }
        }
    }
}

dependencies {
    val room_version = "2.6.1"

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation("org.opencv:opencv:4.9.0")

    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation("androidx.room:room-testing:$room_version")

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("androidx.navigation:navigation-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("com.google.mlkit:object-detection:17.0.2")

    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    val cameraxVersion = "1.4.2"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-auth")

    // Room Database
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
}

val printUnitTestSummary by tasks.registering(PrintJUnitSummaryTask::class) {
    group = "verification"
    description = "Prints a readable unit test summary to the terminal."
    doFirst {
        project.extensions.extraProperties["summaryType"] = "Unit Test"
        project.extensions.extraProperties["summaryDir"] = layout.buildDirectory.dir("test-results/testDebugUnitTest").get().asFile.absolutePath
    }
}

val printInstrumentationTestSummary by tasks.registering(PrintJUnitSummaryTask::class) {
    group = "verification"
    description = "Prints a readable instrumentation test summary to the terminal."
    doFirst {
        project.extensions.extraProperties["summaryType"] = "Instrumentation Test"
        project.extensions.extraProperties["summaryDir"] = layout.buildDirectory.dir("outputs/androidTest-results/connected/debug").get().asFile.absolutePath
    }
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
}

tasks.matching { it.name == "testDebugUnitTest" }.configureEach {
    finalizedBy(printUnitTestSummary)
}

tasks.matching { it.name == "connectedDebugAndroidTest" }.configureEach {
    finalizedBy(printInstrumentationTestSummary)
}
