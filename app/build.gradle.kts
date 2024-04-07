plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.dagger.hilt.android)
}

val extVersionCode = 1
val extVersionName = "1.0"

android {
    namespace = "tornaco.apps.shortx.ext"
    compileSdk = 34

    defaultConfig {
        applicationId = "tornaco.apps.shortx.ext"
        minSdk = 26
        targetSdk = 34
        versionCode = extVersionCode
        versionName = extVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            "public.jks".also {
                storeFile = rootProject.file(it)
                storePassword = "123456"
                keyAlias = "shortx"
                keyPassword = "123456"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    applicationVariants.all {
        outputs.all {
            val impl =
                this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            println("impl.outputFileName changed to:${impl.outputFileName}")
        }
    }

    defaultConfig {
        setProperty("archivesBaseName", "ext-${extVersionName}")
    }
}

dependencies {
    implementation(files("libs/apkzlib.jar"))
    implementation(files("libs/ManifestEditor-1.0.2.jar"))
    implementation(libs.apksig)

    implementation(libs.ui.base)
    implementation(libs.core)

    implementation(libs.dagger.hilt.android)
    implementation(libs.dagger.hilt.navigation.compose)
    kapt(libs.dagger.hilt.android.compiler)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.hiddenapibypass)
    implementation(libs.tinypinyin)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}