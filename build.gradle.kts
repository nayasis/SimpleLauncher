group   = "io.github.nayasis"
version = findReleaseVersionFromBranch() ?: "0.1.0-SNAPSHOT"

plugins {
	application
	kotlin("jvm") version "2.2.20"
	id("com.google.devtools.ksp") version "2.2.20-2.0.4"
	id("com.gradleup.shadow") version "9.4.1"
	id("org.graalvm.buildtools.native") version "1.1.12"
}

val osName = System.getProperty("os.name").lowercase()
val isWindows = osName.contains("win")
val isLinux = osName.contains("linux")
val isMac = osName.contains("mac")

val appJvmArgs = listOf(
	// JNA (pty4j) and FlatLaf load native libraries
	"--enable-native-access=ALL-UNNAMED",
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
	// the Swing JediTerm is published only here
	maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") {
		content { includeGroup("org.jetbrains.jediterm") }
	}
}

configurations.all {
    exclude(group = "org.slf4j", module = "slf4j-jdk14")
    resolutionStrategy {
        force("com.h2database:h2:2.1.214")
    }
}

dependencies {

	implementation("io.github.nayasis:basica-kt:0.3.14")
	implementation("io.github.nayasis:basica-swing:0.1.0-SNAPSHOT")
	implementation("ch.qos.logback:logback-classic:1.5.31")

	// UI
	implementation("com.formdev:flatlaf:3.7.2")
	implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.11.0"))
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing")
	implementation("com.github.vatbub:mslinks:1.0.6.2")
	implementation("commons-cli:commons-cli:1.11.0")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.1")
	implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

	// exposed
	ksp("io.github.nayasis:exposed-crud-processor:0.1.0")
	implementation("io.github.nayasis:exposed-crud:0.1.0")
	implementation("com.h2database:h2:2.3.232")

	// terminal: 3.73+ needs kotlin-stdlib 2.4, which this compiler cannot read
	implementation("org.jetbrains.jediterm:jediterm-core:3.72")
	implementation("org.jetbrains.jediterm:jediterm-ui:3.72")
	implementation("org.jetbrains.pty4j:pty4j:0.13.10")

	testImplementation("org.apache.pdfbox:pdfbox:2.0.24")
	testImplementation("com.levigo.jbig2:levigo-jbig2-imageio:2.0")
	testImplementation("org.apache.httpcomponents.client5:httpclient5:5.3.1")
	implementation(kotlin("scripting-compiler-embeddable"))

	// JNA (windows)
	testImplementation("net.java.dev.jna:jna:5.9.0")
	testImplementation("net.java.dev.jna:jna-platform:5.9.0")

	testImplementation(kotlin("test"))
	testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
	testImplementation("io.github.classgraph:classgraph:4.8.184")
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
	jvmArgs(appJvmArgs)
}

tasks.register<JavaExec>("runShortcutEditorTest") {
	group = "verification"
	description = "Runs the shortcut settings dialog only"
	classpath = sourceSets["test"].runtimeClasspath
	mainClass.set("io.github.nayasis.simplelauncher.view.shortcut.ShortcutEditorManualTestKt")
}

tasks.register<JavaExec>("runTerminalDemo") {
	group = "verification"
	description = "Runs a command in the terminal window only"
	classpath = sourceSets["test"].runtimeClasspath
	mainClass.set("io.github.nayasis.simplelauncher.view.terminal.TerminalDemoKt")
}

val isGitHubActions = System.getenv("GITHUB_ACTIONS") == "true"
val requestedPackageType = System.getenv("SIMPLELAUNCHER_PACKAGE_TYPE")?.trim()?.lowercase()?.takeIf { it.isNotBlank() }

fun Project.toJpackageAppVersion(): String {
	// jpackage only accepts numeric components, so drop any qualifier (-SNAPSHOT, -rc1, ...)
	val parts = version.toString()
		.substringBefore('-')
		.split('.')
		.mapNotNull { it.trim().toIntOrNull() }
		.take(3)
		.toMutableList()

	if (parts.isEmpty()) {
		return "1"
	}

	if (isMac && parts[0] <= 0) {
		parts[0] = 1
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
	"java.scripting",   // javax.script, used by the embedded kotlin scripting compiler
	"jdk.unsupported",  // sun.misc.Unsafe, used by JNA (pty4j) and kotlinx-coroutines
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
	
	val modulePath      = javaHome.resolve("jmods").absolutePath

	doFirst {
		logger.info("Creating custom runtime image with jlink...")
		logger.info("Required modules: ${requiredJdkModules.joinToString(", ")}")
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
		allJars.forEach { jar ->
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
// ─── GraalVM native-image (AOT) ──────────────────────────────────────────────
// Requires a GraalVM-based JDK 25 on JAVA_HOME (Liberica NIK 25 here) plus MSVC
// build tools on Windows. Build with: ./gradlew nativeCompile
graalvmNative {
	// use whatever GraalVM is running Gradle instead of resolving a toolchain,
	// so the build does not silently pick a non-GraalVM JDK 25
	toolchainDetection = false

	// The plugin's own downloader cannot fetch the shared metadata repository from
	// GitHub releases behind this proxy, and `enabled = false` still evaluates the
	// download. Point it at a local copy when one is cached:
	//   curl -L -o ~/.gradle/graalvm-metadata/graalvm-reachability-metadata-1.0.13.zip \
	//     https://github.com/oracle/graalvm-reachability-metadata/releases/download/1.0.13/graalvm-reachability-metadata-1.0.13.zip
	metadataRepository {
		val cached = file("${System.getProperty("user.home")}/.gradle/graalvm-metadata/graalvm-reachability-metadata-1.0.13.zip")
		if (cached.exists()) {
			uri(cached)
		}
	}

	binaries {
		named("main") {
			imageName    = "simplelauncher"
			mainClass    = "io.github.nayasis.simplelauncher.SimplelauncherKt"
			fallback     = false
			verbose      = true

			// bundle src/main/resources (images, message bundles) into the image;
			// a native image cannot read them off a classpath
			resources.autodetect()

			buildArgs.addAll(
				"--enable-native-access=ALL-UNNAMED",
				"-H:+ReportExceptionStackTraces",
			)
		}
	}
}
