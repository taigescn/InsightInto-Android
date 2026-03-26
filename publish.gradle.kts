configure<PublishingExtension> {
    publications {
        register<MavenPublication>("release") {
            groupId = project.properties["GROUP_ID"] as String
            artifactId = project.properties["ARTIFACT_ID"] as String
            version = project.properties["SDK_VERSION"] as String

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}