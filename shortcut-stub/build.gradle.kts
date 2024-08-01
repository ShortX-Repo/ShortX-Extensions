plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "tornaco.apps.shortx.stub"

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

    defaultConfig {
        targetSdk = 29
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }

        getByName("debug") {
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = false
        buildConfig = false
        aidl = false
        renderScript = false
        resValues = false
        shaders = false
        viewBinding = false
        dataBinding = false
    }

    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }

    applicationVariants.all {
        outputs.all {
            val impl =
                this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
        }
    }

    defaultConfig {
        setProperty(
            "archivesBaseName",
            "shortcut-stub"
        )
    }
}

dependencies {
    implementation(libs.core)
    implementation(libs.hiddenapibypass)
}







