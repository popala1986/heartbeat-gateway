plugins {
    id("heartbeat.java-conventions")
    application
}

dependencies {
    implementation(project(":common"))
    implementation(libs.slf4j.api)

    runtimeOnly(libs.grpc.netty.shaded)
    runtimeOnly(libs.logback.classic)

    testImplementation(libs.grpc.inprocess)
}

application {
    mainClass = "io.github.popala1986.heartbeat.simulator.GrpcSimulatorApplication"
}
