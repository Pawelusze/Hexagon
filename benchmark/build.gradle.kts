plugins {
    id("hexagon.java-conventions")
    alias(libs.plugins.jmh)
}

description = "Region lookup benchmarks, Hexagon against WorldGuard."

dependencies {
    jmh(project(":hexagon-api"))
    jmh(project(":hexagon-plugin"))
    jmh(libs.paper.api)
    jmh(libs.worldguard.core)
}

// A full run takes a couple of minutes. Raise the counts when a number has to be quotable.
jmh {
    warmupIterations = 3
    iterations = 5
    fork = 2
    warmup = "2s"
    timeOnIteration = "2s"
}
