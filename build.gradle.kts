plugins {
    base
    // Loaded at the root with `apply false` so every subproject shares one Spotless build service.
    id("hexagon.java-conventions") apply false
}

allprojects {
    version = "1.0.0"
}
