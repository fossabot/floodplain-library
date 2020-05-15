apply(from="$rootProject.projectDir/gradle/dependencies.gradle.kts")

val globalConf = rootProject.ext

dependencies {
    testCompile("junit:junit:4.12")
    testRuntime("org.junit.vintage:junit-vintage-engine:5.2.0")
    testImplementation ("org.junit.jupiter:junit-jupiter-api:5.2.0")
    testRuntimeOnly ("org.junit.jupiter:junit-jupiter-engine:5.2.0")
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
task copyTestResources(type: Copy) {
    from = "${projectDir}/test"
    into = "${buildDir}/classes/test"
}


processTestResources.dependsOn copyTestResources


