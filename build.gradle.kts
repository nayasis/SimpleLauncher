group   = "io.github.nayasis"
version = "0.1.5"

plugins {
	application
	kotlin("jvm") version "2.2.0"
	id("com.google.devtools.ksp") version "2.2.0-2.0.2"
	id("org.openjfx.javafxplugin") version "0.1.0"
	id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
	mainClass.set("io.github.nayasis.simplelauncher.SimplelauncherKt")
	applicationName = "simplelauncher"
	applicationDefaultJvmArgs = listOf(
		"--add-exports=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED",
		"--add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED",
		"--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED"
	)
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

javafx {
	version = "21.0.2"
	modules = listOf("javafx.graphics","javafx.controls","javafx.fxml","javafx.swing")
}

repositories {
	mavenLocal()
	mavenCentral()
	google()
	gradlePluginPortal()
}

configurations.all {
    exclude(group = "org.slf4j", module = "slf4j-jdk14")
    resolutionStrategy {
        force("com.h2database:h2:2.1.214")
    }
}

dependencies {

	// core
//	implementation("io.github.nayasis:basica-kt:0.3.11")
	implementation("io.github.nayasis:basica-kt:0.1.0-SNAPSHOT")
//	implementation("io.github.nayasis:basicafx-kt:0.2.6")
	implementation("io.github.nayasis:basicafx-kt:0.1.0-SNAPSHOT")
	implementation("ch.qos.logback:logback-classic:1.5.31")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.10.2")

	// exposed
	ksp("com.dshatz.exposed-crud:processor:0.1.0-SNAPSHOT")
	implementation("com.dshatz.exposed-crud:lib:0.1.0-SNAPSHOT")
	implementation("com.h2database:h2:2.3.232")

	// UI
	implementation("no.tornado:tornadofx:1.7.20") {
		exclude("org.jetbrains.kotlin")
	}
	implementation("org.controlsfx:controlsfx:11.2.2")
	implementation("com.github.vatbub:mslinks:1.0.6.2")
	implementation("commons-cli:commons-cli:1.4")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.1")
	implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
	implementation("de.jensd:fontawesomefx:8.9")

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

val isWindows = System.getProperty("os.name").lowercase().contains("win")

fun File.hasSuffix(suffixes: Set<String>): Boolean =
	suffixes.any { suffix -> name.contains(suffix, ignoreCase = true) }

fun filterJavaFxJars(jars: Collection<File>): List<File> {
	val platformSuffixes = if (isWindows) setOf("-win") else setOf("-linux", "-mac")
	val allSuffixes      = setOf("-win", "-linux", "-mac")
	return jars
		.filter { it.name.startsWith("javafx", ignoreCase = true) }
		.filter { jar ->
			jar.hasSuffix(platformSuffixes) || !jar.hasSuffix(allSuffixes)
		}
}

tasks.register<Delete>("cleanCreateRuntimeImage") {
	group       = "distribution"
	description = "Cleans the custom runtime image directory"
	delete("build/runtime-image")
}

val requiredJdkModules = listOf(
	"java.base",
	"java.desktop",
	"java.logging",
	"java.sql",
	"java.xml",
	"java.naming",
	"java.scripting",  // Required for javax.script.* (used by tornadofx)
	"jdk.unsupported",  // Required for JavaFX
	"jdk.crypto.ec"
)

tasks.register<Exec>("createRuntimeImage") {
	group       = "distribution"
	description = "Creates a custom runtime image with jlink (smaller size)"
	
	dependsOn("build", "cleanCreateRuntimeImage")
	
	val javaToolchain   = javaToolchains.launcherFor(java.toolchain).get()
	val javaHome        = javaToolchain.metadata.installationPath.asFile
	val jlinkPath       = javaHome.resolve("bin/jlink${if (isWindows) ".exe" else ""}")
	val runtimeImageDir = file("build/runtime-image")
	
	val allJars         = configurations.runtimeClasspath.get().files.filter { it.name.endsWith(".jar") }
	val javafxJars      = filterJavaFxJars(allJars).map { it.parentFile.absolutePath }.distinct()
	val javaFxModulePath = javafxJars.joinToString(File.pathSeparator)
	
	val modulePath = if (javaFxModulePath.isNotEmpty()) {
		"${javaHome.resolve("jmods").absolutePath}${File.pathSeparator}$javaFxModulePath"
	} else {
		javaHome.resolve("jmods").absolutePath
	}
	
	doFirst {
		logger.info("Creating custom runtime image with jlink...")
		logger.info("Required modules: ${requiredJdkModules.joinToString(", ")}")
		if (javaFxModulePath.isNotEmpty()) {
			logger.info("JavaFX module path: $javaFxModulePath")
		}
	}
	
	commandLine(
		jlinkPath.absolutePath,
		"--module-path", modulePath,
		"--add-modules", requiredJdkModules.joinToString(","),
		"--strip-debug",
		"--compress", "2",
		"--no-header-files",
		"--no-man-pages",
		"--output", runtimeImageDir.absolutePath
	)
}

tasks.register<Exec>("createNativeExe") {
	group       = "distribution"
	description = "Creates a native executable using jpackage"
	
	dependsOn("createRuntimeImage")

	val javaToolchain      = javaToolchains.launcherFor(java.toolchain).get()
	val jpackageExecutable = if (isWindows) "jpackage.exe" else "jpackage"
	val javaHome           = javaToolchain.metadata.installationPath.asFile
	val jpackagePath       = javaHome.resolve("bin/$jpackageExecutable")
	val jarFile            = tasks.named<Jar>("jar").get().archiveFile.get().asFile
	val outputDir          = file("build/dist")
	val runtimeImageDir    = file("build/runtime-image")
	val jpackageInputDir   = file("build/jpackage-input")
	
	val runtimeClasspath   = configurations.runtimeClasspath.get().files
	val allJars            = runtimeClasspath.filter { it.name.endsWith(".jar") }
	val javafxJars         = filterJavaFxJars(allJars)
	
	val useExe = runCatching {
		Runtime.getRuntime().exec("light.exe -?").waitFor()
		true
	}.getOrElse { false }

	doFirst {
		// Validation
		if (!jarFile.exists())
			throw GradleException("JAR file not found: ${jarFile.absolutePath}. Run 'gradlew build' first.")
		if (!jpackagePath.exists())
			throw GradleException("jpackage not found at: ${jpackagePath.absolutePath}. Make sure you're using Java 14 or higher.")
		if (!runtimeImageDir.exists())
			throw GradleException("Runtime image not found: ${runtimeImageDir.absolutePath}. Run 'gradlew createRuntimeImage' first.")

		// Prepare JAR files for jpackage input
		jpackageInputDir.deleteRecursively()
		jpackageInputDir.mkdirs()
		jarFile.copyTo(jpackageInputDir.resolve(jarFile.name), overwrite = true)
		(allJars.filterNot { it.name.startsWith("javafx", ignoreCase = true) } + javafxJars).forEach { jar ->
			jar.copyTo(jpackageInputDir.resolve(jar.name), overwrite = true)
		}
		
		logger.info("Creating native executable with jpackage...")
		logger.info("Output directory: ${outputDir.absolutePath}")
	}
	
	val jpackageArgs = mutableListOf<String>(
		"--type",          if (useExe) "exe" else "app-image",
		"--input",         jpackageInputDir.absolutePath,
		"--name",          application.applicationName,
		"--main-jar",      jarFile.name,
		"--main-class",    application.mainClass.get(),
		"--dest",          outputDir.absolutePath,
		"--runtime-image", runtimeImageDir.absolutePath
	)
	
	application.applicationDefaultJvmArgs.forEach { option ->
		jpackageArgs.add("--java-options")
		jpackageArgs.add(option)
	}
	
	if (useExe) {
		jpackageArgs.addAll(listOf("--win-dir-chooser", "--win-menu", "--win-shortcut"))
	}
	
	file("src/main/resources/image/icon/favicon.ico").takeIf { it.exists() }?.let { icon ->
		jpackageArgs.addAll(listOf("--icon", icon.absolutePath))
	}

	commandLine(listOf(jpackagePath.absolutePath) + jpackageArgs)
}