import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

license {
    header = rootProject.file("gradle/LICENSE_HEADER.txt")
    mapping("java", "SLASHSTAR_STYLE")
    mapping("kt", "SLASHSTAR_STYLE")
    ext.year = "2020"
    ext.name = "Frank Lyaruu"
}

compileKotlin {
    kotlinOptions.jvmTarget = "11"
}
compileTestKotlin {
    kotlinOptions.jvmTarget = "11"
}

dokka {
    outputFormat = "gfm"
    outputDirectory = "$buildDir/dokka"
}

task sourceJar(type: Jar, dependsOn: classes) {
    classifier "sources"
    from sourceSets.main.allSource
}

task javadocJar(type: Jar, dependsOn: javadoc) {
    classifier = "javadoc"
    from javadoc.destinationDir
}

task publishAll {
    dependsOn "build"
    dependsOn "javadocJar"
    dependsOn "sourceJar"

    doLast {
        println "We release now"
    }
}
