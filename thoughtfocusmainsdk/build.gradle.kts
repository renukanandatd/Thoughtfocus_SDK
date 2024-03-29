plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.thoughtfocusmainsdk"
    compileSdk = 34

    defaultConfig {
        //applicationId = "com.example.thoughtfocusmainsdk"
        minSdk = 24
        targetSdk = 34
       //versionCode = 1
        //versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation(files("C:\\Users\\107503\\AndroidStudioProjects\\ThoughtfocusSDK\\thoughtfocusmainsdk\\libs\\acra-4.7.0.jar"))
    implementation(files("C:\\Users\\107503\\AndroidStudioProjects\\ThoughtfocusSDK\\thoughtfocusmainsdk\\libs\\bbdevice-android-3.28.2.jar"))
    implementation(files("C:\\Users\\107503\\AndroidStudioProjects\\ThoughtfocusSDK\\thoughtfocusmainsdk\\libs\\bbdeviceota-android-1.6.26.jar"))
    implementation(files("C:\\Users\\107503\\AndroidStudioProjects\\ThoughtfocusSDK\\thoughtfocusmainsdk\\libs\\ksoap2-android-assembly-2.4-jar-with-dependencies.jar"))


}