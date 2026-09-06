plugins {
    id("hexagon.java-conventions")
    `java-library`
}

description = "Public API of Hexagon, the region protection plugin for Paper."

dependencies {
    compileOnly(libs.paper.api)
    compileOnlyApi(libs.jetbrains.annotations)
    testImplementation(libs.paper.api)
}

java {
    withJavadocJar()
}
