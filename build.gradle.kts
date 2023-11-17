import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	application
	id("org.openjfx.javafxplugin") version "0.0.14"
	id("org.beryx.runtime") version "1.12.6"
	kotlin("jvm") version "1.8.20"
	kotlin("plugin.jpa") version "1.8.20"
	kotlin("plugin.noarg") version "1.8.20"
	kotlin("plugin.allopen") version "1.8.20"
}

allOpen {
	annotation("javax.persistence.Entity")
	annotation("javax.persistence.MappedSuperclass")
	annotation("javax.persistence.Embeddable")
}
noArg {
	annotation("javax.persistence.Entity")
	annotation("javax.persistence.MappedSuperclass")
	annotation("javax.persistence.Embeddable")
	annotation("com.github.nayasis.kotlin.spring.kotlin.annotation.NoArg")
	invokeInitializers = true
}

application {
	mainClass.set("com.github.nayasis.simplelauncher.SimplelauncherKt")
	applicationName = "simplelauncher"
}

javafx {
	version = "19.0.2.1"
	modules = listOf("javafx.graphics","javafx.controls","javafx.fxml","javafx.web","javafx.swing")
}

group = "com.github.nayasis"
version = "0.1.5"

java {
	sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11
}

configurations.all {
	resolutionStrategy.cacheChangingModulesFor(0, "seconds")
	resolutionStrategy.cacheDynamicVersionsFor(5, "minutes")
}

repositories {
	mavenLocal()
	mavenCentral()
	jcenter()
	maven { url = uri("https://jitpack.io") }
	maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.7.3")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
	implementation("org.jetbrains.exposed:exposed-core:0.44.1")
	implementation("org.jetbrains.exposed:exposed-java-time:0.44.1")
	implementation("org.jetbrains.exposed:exposed-jdbc:0.44.1")

	implementation("com.github.nayasis:basica-kt:0.3.1")
//	implementation("com.github.nayasis:basica-kt:develop-SNAPSHOT"){ isChanging = true }
//	implementation("com.github.nayasis:basicafx-kt:0.1.20")
//	implementation("com.github.nayasis:basicafx-kt:develop-SNAPSHOT"){ isChanging = true }
	implementation("com.github.nayasis:basicafx-kt:0.2.1-SNAPSHOT")
	implementation("no.tornado:tornadofx:1.7.20") {
		exclude("org.jetbrains.kotlin")
	}
	implementation("org.controlsfx:controlsfx:11.1.0")
	implementation("com.github.vatbub:mslinks:1.0.6.2")
	implementation("com.github.nayasis:terminalfx-kt:0.1.2")
	implementation("commons-cli:commons-cli:1.4")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.+")
	implementation("ch.qos.logback:logback-classic:1.4.11")

	implementation("com.h2database:h2:2.2.224")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("io.github.microutils:kotlin-logging:3.0.5")
	implementation("au.com.console:kassava:2.1.0")

	implementation("de.jensd:fontawesomefx:8.9")
	testImplementation("org.apache.pdfbox:pdfbox:2.0.24")
	testImplementation("com.levigo.jbig2:levigo-jbig2-imageio:2.0")
	testImplementation("org.apache.httpcomponents:httpclient:4.5.13")
//	testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
//	testImplementation("org.junit.jupiter:junit-jupiter-engine:5.3.1")

	// JNA (windows)
	testImplementation("net.java.dev.jna:jna:5.9.0")
	testImplementation("net.java.dev.jna:jna-platform:5.9.0")

//	testImplementation("io.kotest:kotest-runner-junit5:5.6.1")
//	testImplementation("io.kotest:kotest-assertions-core:5.6.1")
//	testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.3")

	testImplementation("io.kotest:kotest-assertions-core:5.7.2")
	testImplementation("io.kotest:kotest-runner-junit5:5.7.2")
	testImplementation("io.kotest:kotest-property:5.7.2")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
	testImplementation("org.yaml:snakeyaml:2.2")

}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf(
			"-Xjsr305=strict"
		)
		jvmTarget = "11"
	}
}

runtime {
	options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
	additive.set(true)
	modules.addAll("jdk.crypto.cryptoki")
	launcher {
		noConsole = true
	}
}