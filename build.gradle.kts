group   = "io.github.nayasis"
version = findReleaseVersionFromBranch() ?: "0.1.0-SNAPSHOT"

plugins {
	application
	kotlin("jvm") version "2.2.0"
	id("com.google.devtools.ksp") version "2.2.0-2.0.2"
	id("com.gradleup.shadow") version "9.4.1"
}

val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()
val isWindows = osName.contains("win")
val isLinux = osName.contains("linux")
val isMac = osName.contains("mac")
val javafxVersion = "26"
val javafxModules = listOf("javafx.graphics", "javafx.controls", "javafx.fxml", "javafx.swing")
val javafxPlatform = when {
	isWindows && (osArch.contains("aarch64") || osArch.contains("arm64")) -> "win-aarch64"
	isWindows && (osArch == "x86" || osArch == "i386") -> "win-x86"
	isWindows -> "win"
	isLinux && (osArch.contains("aarch64") || osArch.contains("arm64")) -> "linux-aarch64"
	isLinux -> "linux"
	isMac && (osArch.contains("aarch64") || osArch.contains("arm64")) -> "mac-aarch64"
	isMac -> "mac"
	else -> throw GradleException("Unsupported JavaFX platform: $osName / $osArch")
}

val appJvmArgs = listOf(
	"-Djavafx.enablePreview=true",
	"-Djavafx.suppressPreviewWarning=true",
	"-Djavafx.suppressUnsupportedConfiguration=true",
	"--enable-native-access=ALL-UNNAMED",
	"--enable-native-access=javafx.graphics",
	"--add-exports=javafx.base/com.sun.javafx.event=ALL-UNNAMED",
	"--add-exports=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED",
	"--add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED",
	"--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED",
)

application {
	mainClass.set("io.github.nayasis.simplelauncher.SimplelauncherKt")
	applicationName = "simplelauncher"
	applicationDefaultJvmArgs = appJvmArgs
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
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

	implementation("io.github.nayasis:basica-kt:0.3.13")
//	implementation("io.github.nayasis:basicafx-kt:0.3.1")
	implementation("io.github.nayasis:basicafx-kt:0.1.0-SNAPSHOT")
	implementation("ch.qos.logback:logback-classic:1.5.31")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.10.2")
	implementation("org.openjfx:javafx-base:$javafxVersion:$javafxPlatform")
	implementation("org.openjfx:javafx-graphics:$javafxVersion:$javafxPlatform")
	implementation("org.openjfx:javafx-controls:$javafxVersion:$javafxPlatform")
	implementation("org.openjfx:javafx-fxml:$javafxVersion:$javafxPlatform")
	implementation("org.openjfx:javafx-swing:$javafxVersion:$javafxPlatform")

	// exposed
	ksp("io.github.nayasis:exposed-crud-processor:0.1.0")
	implementation("io.github.nayasis:exposed-crud:0.1.0")
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

	// jediterm-fx for terminal UI
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
	testImplementation("io.github.classgraph:classgraph:4.8.184")
	testImplementation("org.testfx:testfx-junit5:4.0.18")
	testImplementation("org.yaml:snakeyaml:2.2")

}

kotlin {
	compilerOptions {
		jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_24)
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}


tasks.withType<Test> {
	useJUnitPlatform()
	jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.withType<JavaCompile> {
	options.release.set(24)
}

tasks.withType<JavaExec> {
	jvmArgs(
		"-Djavafx.suppressUnsupportedConfiguration=true",
		"--enable-native-access=ALL-UNNAMED",
	)
}

tasks.named<JavaExec>("run") {
	jvmArgs(appJvmArgs)
	useJavaFxModulePath()
}

tasks.register<JavaExec>("runChildWindowLifecycleTest") {
	group = "verification"
	description = "Runs the manual child-window hide/restore viewer"
	classpath = sourceSets["test"].runtimeClasspath
	mainClass.set("io.github.nayasis.simplelauncher.view.lifecycle.ChildWindowLifecycleTestKt")
	jvmArgs(appJvmArgs)
	useJavaFxModulePath()
}

tasks.register<JavaExec>("runShortcutEditorTest") {
	group = "verification"
	description = "Runs the shortcut settings dialog only"
	classpath = sourceSets["test"].runtimeClasspath
	mainClass.set("io.github.nayasis.simplelauncher.view.shortcut.ShortcutEditorManualTestKt")
	jvmArgs(appJvmArgs)
	useJavaFxModulePath()
}
val isGitHubActions = System.getenv("GITHUB_ACTIONS") == "true"
val requestedPackageType = System.getenv("SIMPLELAUNCHER_PACKAGE_TYPE")?.trim()?.lowercase()?.takeIf { it.isNotBlank() }

fun Project.toJpackageAppVersion(): String {
	val parts = version.toString()
		.split('.')
		.filter { it.isNotBlank() }
		.take(3)
		.toMutableList()

	if (parts.isEmpty()) {
		return "1"
	}

	if (isMac && (parts[0].toIntOrNull() ?: 0) <= 0) {
		parts[0] = "1"
	}

	return parts.joinToString(".")
}

fun hasCommand(command: String): Boolean {
	if (isWindows) {
		return runCatching {
			ProcessBuilder("where.exe", command)
				.redirectErrorStream(true)
				.start()
				.apply {
					inputStream.bufferedReader().use { it.readText() }
				}
				.waitFor() == 0
		}.getOrDefault(false)
	}

	return runCatching {
		ProcessBuilder("which", command)
			.redirectErrorStream(true)
			.start()
			.apply {
				inputStream.bufferedReader().use { it.readText() }
			}
			.waitFor() == 0
	}.getOrDefault(false)
}

fun File.hasSuffix(suffixes: Set<String>): Boolean =
	suffixes.any { suffix -> name.contains(suffix, ignoreCase = true) }

fun filterJavaFxJars(jars: Collection<File>): List<File> {
	val platformSuffixes = when {
		isWindows && (osArch.contains("aarch64") || osArch.contains("arm64")) -> setOf("-win-aarch64")
		isWindows && (osArch == "x86" || osArch == "i386") -> setOf("-win-x86")
		isWindows -> setOf("-win")
		isLinux && (osArch.contains("aarch64") || osArch.contains("arm64")) -> setOf("-linux-aarch64")
		isLinux -> setOf("-linux")
		isMac && (osArch.contains("aarch64") || osArch.contains("arm64")) -> setOf("-mac-aarch64")
		isMac -> setOf("-mac")
		else -> emptySet()
	}
	val allSuffixes = setOf("-win", "-win-x86", "-win-aarch64", "-linux", "-linux-aarch64", "-mac", "-mac-aarch64")
	return jars
		.filter { it.name.startsWith("javafx", ignoreCase = true) }
		.filter { jar ->
			jar.hasSuffix(platformSuffixes) || !jar.hasSuffix(allSuffixes)
		}
}

fun isJavaFxJar(file: File): Boolean =
	file.name.startsWith("javafx", ignoreCase = true) && filterJavaFxJars(listOf(file)).isNotEmpty()

fun JavaExec.useJavaFxModulePath() {
	val originalClasspath = classpath
	val javaFxClasspath = originalClasspath.filter(::isJavaFxJar)
	classpath = originalClasspath.filter { !isJavaFxJar(it) }
	jvmArgumentProviders.add(
		org.gradle.process.CommandLineArgumentProvider {
			val modulePath = javaFxClasspath.asPath
			if (modulePath.isBlank()) {
				emptyList()
			} else {
				listOf("--module-path", modulePath, "--add-modules", javafxModules.joinToString(","))
			}
		}
	)
}

fun deleteRecursivelyForce(target: File) {
	if (!target.exists()) return
	target.walkBottomUp().forEach { file ->
		file.setWritable(true)
		if (!file.delete() && file.exists()) {
			throw GradleException("Failed to delete existing path: ${file.absolutePath}")
		}
	}
}

fun copyRecursivelyForce(source: File, target: File) {
	if (!source.exists()) {
		throw GradleException("Deploy source not found: ${source.absolutePath}")
	}
	if (source.isDirectory) {
		target.mkdirs()
		source.copyRecursively(target, overwrite = true)
	} else {
		target.parentFile.mkdirs()
		source.copyTo(target, overwrite = true)
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
	
	dependsOn("jar", "cleanCreateRuntimeImage")
	
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
		"--compress", "zip-6",
		"--no-header-files",
		"--no-man-pages",
		"--output", runtimeImageDir.absolutePath
	)
}

tasks.register<Exec>("createNativeExe") {
	group       = "distribution"
	description = "Creates a native package using jpackage"
	
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
	
	val useExe = isWindows && !isGitHubActions && hasCommand("light.exe")
	val defaultPackageType = when {
		isWindows && isGitHubActions -> "app-image"
		isLinux && isGitHubActions -> "deb"
		isMac && isGitHubActions -> "dmg"
		useExe -> "exe"
		else -> "app-image"
	}
	val packageType = requestedPackageType ?: defaultPackageType

	doFirst {
		// Validation
		if (!jarFile.exists())
			throw GradleException("JAR file not found: ${jarFile.absolutePath}. Run 'gradlew build' first.")
		if (!jpackagePath.exists())
			throw GradleException("jpackage not found at: ${jpackagePath.absolutePath}. Make sure you're using Java 14 or higher.")
		if (!runtimeImageDir.exists())
			throw GradleException("Runtime image not found: ${runtimeImageDir.absolutePath}. Run 'gradlew createRuntimeImage' first.")

		// Prepare JAR files for jpackage input
		deleteRecursivelyForce(outputDir.resolve(application.applicationName))
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
		"--type",          packageType,
		"--input",         jpackageInputDir.absolutePath,
		"--name",          application.applicationName,
		"--app-version",   project.toJpackageAppVersion(),
		"--main-jar",      jarFile.name,
		"--main-class",    application.mainClass.get(),
		"--dest",          outputDir.absolutePath,
		"--runtime-image", runtimeImageDir.absolutePath
	)
	
	application.applicationDefaultJvmArgs.forEach { option ->
		jpackageArgs.add("--java-options")
		jpackageArgs.add(option)
	}
	
	if (packageType == "exe") {
		jpackageArgs.addAll(listOf("--win-dir-chooser", "--win-menu", "--win-shortcut"))
	}
	
	val iconFile = when {
		isWindows -> file("src/main/resources/image/icon/favicon.ico").takeIf { it.exists() }
		isLinux -> file("src/main/resources/image/icon/favicon.png").takeIf { it.exists() }
		else -> null
	}

	iconFile?.let { icon ->
		jpackageArgs.addAll(listOf("--icon", icon.absolutePath))
	}

	commandLine(listOf(jpackagePath.absolutePath) + jpackageArgs)
}

tasks.register("deploy") {
	group       = "distribution"
	description = "Builds the native executable and deploys it to D:/app/SimpleLauncher"

	dependsOn("createNativeExe")

	doLast {
		val appName = application.applicationName
		val sourceDir = file("build/dist/$appName")
		val targetDir = file("d:/app/SimpleLauncher").takeIf{ it.exists() }
			?: file("c:/app/SimpleLauncher").takeIf{ it.exists() }
			?: return@doLast
		val executable = "$appName.exe"

		if (isWindows) {
			ProcessBuilder("taskkill", "/IM", executable, "/F", "/T")
				.redirectErrorStream(true)
				.start()
				.waitFor()
		}

		targetDir.mkdirs()
		listOf("app", "runtime").forEach { name ->
			deleteRecursivelyForce(targetDir.resolve(name))
		}
		deleteRecursivelyForce(targetDir.resolve(executable))

		copyRecursivelyForce(sourceDir.resolve("app"), targetDir.resolve("app"))
		copyRecursivelyForce(sourceDir.resolve("runtime"), targetDir.resolve("runtime"))
		copyRecursivelyForce(sourceDir.resolve(executable), targetDir.resolve(executable))
	}
}

fun Project.findReleaseVersionFromBranch(): String? {
	val branchName = providers.environmentVariable("GITHUB_REF_NAME").orNull?.trim().takeUnless { it.isNullOrBlank() } ?: run {
		try {
			val process = ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
				.redirectErrorStream(true)
				.start()
			process.waitFor()
			process.inputStream.bufferedReader().readText().trim().takeUnless { it.isBlank() || it == "HEAD" }
		} catch (_: Exception) {
			return null
		}
	}

	return branchName
		?.removePrefix("refs/heads/")
		?.takeIf { it.startsWith("release/") }
		?.removePrefix("release/")
		?.takeIf { it.isNotBlank() }
}