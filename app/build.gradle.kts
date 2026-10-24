plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.branchlocator"
    compileSdk = 35

    // Redirect build directory outside OneDrive to prevent AccessDeniedException file locking
    layout.buildDirectory.set(file("C:/AndroidBuilds/BranchLocator/app/build"))

    defaultConfig {
        applicationId = "com.example.branchlocator"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/license.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/notice.txt"
            excludes += "/META-INF/ASL2.0"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    // Google Sign-In / Credential Manager
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Google Drive API Client
    implementation("com.google.api-client:google-api-client-android:2.6.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0")
    implementation("com.google.http-client:google-http-client-gson:1.44.1")

    // Coroutines for background tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Chrome Custom Tabs for In-App Browser Google Sign-In
    implementation("androidx.browser:browser:1.8.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

// Automatically copy generated APK to Google Drive G:\ drive on build completion
tasks.register("copyApkToGoogleDrive") {
    description = "Copies generated APK to Google Drive folder"
    doLast {
        val apkFile = file("C:/AndroidBuilds/BranchLocator/app/build/outputs/apk/debug/app-debug.apk")
        val gDriveFolder = file("G:/")
        val myDriveFolder = file("G:/My Drive/")
        val agnpAppFolder = file("G:/My Drive/AGNP APP/")

        if (apkFile.exists()) {
            val targetDriveDir = if (agnpAppFolder.exists()) agnpAppFolder else (if (myDriveFolder.exists()) myDriveFolder else gDriveFolder)
            copy {
                from(apkFile)
                into(targetDriveDir)
                rename { "BranchLocator_v1.0.apk" }
            }
            if (myDriveFolder.exists() && agnpAppFolder.exists()) {
                copy {
                    from(apkFile)
                    into(myDriveFolder)
                    rename { "BranchLocator_v1.0.apk" }
                }
            }
            println("==================================================")
            println("✅ APK successfully copied & synced to Google Drive: $targetDriveDir")
            println("==================================================")
        }
    }
}

afterEvaluate {
    tasks.findByName("assembleDebug")?.finalizedBy("copyApkToGoogleDrive")
}
