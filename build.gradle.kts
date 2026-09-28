plugins {
  kotlin("jvm") version "2.4.20"
  application
}

group = "com.aliadnani"

version = "0.1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  testImplementation(kotlin("test"))

  implementation("info.picocli:picocli:4.7.7")

  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.22.2")
  implementation("org.xerial:sqlite-jdbc:3.53.4.0")
}

kotlin {
  jvmToolchain(21)
}

application {
  mainClass.set("com.aliadnani.MainKt")
  applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

tasks.test {
  useJUnitPlatform()
  jvmArgs("--enable-native-access=ALL-UNNAMED")
}
