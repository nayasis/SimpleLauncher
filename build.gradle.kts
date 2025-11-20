group = "com.github.nayasis"
version = "0.1.5"

plugins {
	application
	kotlin("jvm") version "2.2.10"
	kotlin("plugin.serialization") version "2.2.10"
	id("com.google.devtools.ksp") version "2.1.21-2.0.1"
	id("org.openjfx.javafxplugin") version "0.1.0"
	id("org.beryx.runtime") version "1.13.1"
}

application {
	mainClass.set("io.github.nayasis.simplelauncher.SimplelauncherKt")
	applicationName = "simplelauncher"
	applicationDefaultJvmArgs = listOf(
		"--add-exports=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED",
		"--add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED",
		"--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED",
	)
}

javafx {
	version = "21.0.2"
	modules = listOf("javafx.graphics","javafx.controls","javafx.fxml","javafx.swing")
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenLocal()
	mavenCentral()
	google()
}

configurations.all {
    exclude(group = "org.slf4j", module = "slf4j-jdk14")
    resolutionStrategy {
        force("com.h2database:h2:2.1.214")
    }
}

dependencies {

	// Komapper dependencies
	platform("org.komapper:komapper-platform:5.7.0").let {
		implementation(it)
		ksp(it)
	}
	implementation("org.komapper:komapper-starter-jdbc")
	implementation("org.komapper:komapper-dialect-h2-jdbc")
	ksp("org.komapper:komapper-processor")

	implementation("io.github.nayasis:basica-kt:0.3.7")
	implementation("io.github.nayasis:basicafx-kt:0.2.4")
	implementation("no.tornado:tornadofx:1.7.20") {
		exclude("org.jetbrains.kotlin")
	}
	implementation("org.controlsfx:controlsfx:11.2.2")
	implementation("com.github.vatbub:mslinks:1.0.6.2")
	implementation("commons-cli:commons-cli:1.4")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
	implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
	implementation("ch.qos.logback:logback-classic:1.5.19")

	implementation("com.h2database:h2:2.1.214")

	implementation("de.jensd:fontawesomefx:8.9")

    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.10.2")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

	// jeditermfx for terminal UI
	implementation("com.techsenger.jeditermfx:jeditermfx-core:1.1.0")
	implementation("com.techsenger.jeditermfx:jeditermfx-ui:1.1.0")
	implementation("com.techsenger.jeditermfx:jeditermfx-app:1.0.0") {
		exclude(group = "org.jetbrains.pty4j", module = "purejavacomm")
	}
	implementation("org.jetbrains.pty4j:pty4j:0.13.10")


	testImplementation("org.apache.pdfbox:pdfbox:2.0.24")
	testImplementation("com.levigo.jbig2:levigo-jbig2-imageio:2.0")
	testImplementation("org.apache.httpcomponents.client5:httpclient5:5.3.1")
	testImplementation("org.jetbrains.pty4j:pty4j:0.12.34")
	implementation(kotlin("scripting-compiler-embeddable"))

	// JNA (windows)
	testImplementation("net.java.dev.jna:jna:5.9.0")
	testImplementation("net.java.dev.jna:jna-platform:5.9.0")

    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("org.testfx:testfx-junit5:4.0.18")
	testImplementation("org.yaml:snakeyaml:2.2")

	// exposed
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")
	testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")

	ksp("com.dshatz.exposed-crud:processor:1.0.3")
	implementation("com.dshatz.exposed-crud:lib:1.0.3")
	testImplementation("com.dshatz.exposed-crud:lib:1.0.3")

	testImplementation("org.jetbrains.exposed:exposed-core:0.60.0")
	testImplementation("org.jetbrains.exposed:exposed-dao:0.60.0")
	testImplementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.60.0")
	testImplementation("org.jetbrains.exposed:exposed-json:0.60.0")
	testImplementation("org.jetbrains.exposed:exposed-jdbc:0.60.0")

}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}


tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<JavaCompile> {
	options.release.set(17)
}

runtime {
	options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
	launcher {
		noConsole = true
	}
}
