plugins{
    `kotlin-dsl-base`
    `kotlin-dsl`.apply(false)
}

repositories{
    mavenCentral()
    gradlePluginPortal()
    google()
}

dependencies{
    implementation("org.javassist:javassist:3.30.2-GA")
    implementation("com.android.tools.smali:smali-dexlib2:3.0.5")
    implementation("de.undercouch:gradle-download-task:5.6.0")
}

sourceSets.main{
    kotlin.srcDir("plugins")
}

//later apply to use srcDir
apply(plugin = "org.gradle.kotlin.kotlin-dsl")