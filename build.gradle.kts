plugins {
    java
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.versions)
}

tasks.wrapper {
    gradleVersion = "8.8"
    distributionType = Wrapper.DistributionType.ALL
}

repositories {
    mavenCentral()
}

dependencies {
    // logging
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.log4j)
    implementation(libs.slf4j.jcl)
    implementation(libs.slf4j.jul)
    implementation(libs.logback)
    implementation(libs.janino)

    // app specific
    implementation(libs.undertow)
    implementation(libs.snakeyaml)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.dataformat.xml)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

application {
    mainClass.set("com.kvaster.iptv.App")
}

configurations.forEach {
    it.exclude("org.apache.httpcomponents", "httpclient")
    it.exclude("org.apache.httpcomponents", "httpcore")

    it.exclude("com.sun.mail", "javax.mail")
    it.exclude("javax.activation", "activation")
}
