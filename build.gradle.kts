group   = "io.github.nayasis"
version = findReleaseVersionFromBranch() ?: "0.1.0-SNAPSHOT"

plugins {
	application
	kotlin("jvm") version "2.4.21"
	id("com.google.devtools.ksp") version "2.3.12"
	id("com.gradleup.shadow") version "9.6.1"
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
	// only for the libraries published locally (basica-kt, basica-swing)
	mavenLocal {
		content { includeGroup("io.github.nayasis") }
	}
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
}

dependencies {

	implementation("io.github.nayasis:basica-kt:0.4.0")
	implementation("io.github.nayasis:basica-swing:0.1.0")
	implementation("ch.qos.logback:logback-classic:1.6.3")

	// UI
	implementation("com.formdev:flatlaf:3.7.2")
	implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.11.0"))
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing")
	implementation("com.github.vatbub:mslinks:1.0.6.2")
	implementation("commons-cli:commons-cli:1.11.0")
	implementation(platform("com.fasterxml.jackson:jackson-bom:2.21.7"))
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

	// exposed
	ksp("io.github.nayasis:exposed-crud-processor:0.1.0")
	kspTest("io.github.nayasis:exposed-crud-processor:0.1.0")  // test entities (Department, ...)
	implementation("io.github.nayasis:exposed-crud:0.1.0")
	implementation("com.h2database:h2:2.3.232")

	// terminal
	implementation("org.jetbrains.jediterm:jediterm-core:3.72")
	implementation("org.jetbrains.jediterm:jediterm-ui:3.72")
	implementation("org.jetbrains.pty4j:pty4j:0.13.10")

	implementation(kotlin("scripting-compiler-embeddable"))

	testImplementation(kotlin("test"))
	testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
	testImplementation("io.github.classgraph:classgraph:4.8.184")

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

val requiredJdkModules = listOf(
	"java.base",
	"java.desktop",
	"java.logging",
	"java.sql",
	"java.xml",
	"java.naming",
	"java.scripting",   // javax.script, used by the embedded kotlin scripting compiler
	"jdk.unsupported",  // sun.misc.Unsafe, used by JNA (pty4j) and kotlinx-coroutines
	"jdk.crypto.ec",
)

/** jlink writes read-only files, which File.deleteRecursively cannot remove on Windows. */
fun File.deleteForce() {
	walkBottomUp().forEach { it.setWritable(true); it.delete() }
	if (exists()) throw GradleException("Cannot delete $absolutePath")
}

// jpackage runs jlink itself (--add-modules), so the app needs no separate runtime image step
tasks.register<Exec>("makeExecutable") {
	group       = "distribution"
	description = "Packages the app with a trimmed JVM using jpackage (build/dist/simplelauncher)"
	dependsOn("installDist")

	val appName     = application.applicationName
	val input       = layout.buildDirectory.dir("install/$appName/lib").get().asFile
	val dest        = layout.buildDirectory.dir("dist").get().asFile
	val packageType = providers.environmentVariable("SIMPLELAUNCHER_PACKAGE_TYPE").getOrElse("app-image")
	// jpackage accepts numbers only (1.2.3-SNAPSHOT -> 1.2.3), and macOS rejects a leading 0
	val appVersion  = version.toString().substringBefore('-').let { if (isMac && it.startsWith("0.")) "1${it.drop(1)}" else it }
	val jpackage    = javaToolchains.launcherFor(java.toolchain).get()
		.metadata.installationPath.file("bin/jpackage${if (isWindows) ".exe" else ""}").asFile

	val icon = when {
		isWindows -> "src/main/resources/image/icon/favicon.ico"
		isLinux   -> "src/main/resources/image/icon/favicon.png"
		else      -> null
	}

	commandLine(buildList {
		addAll(listOf(jpackage.absolutePath,
			"--type",         packageType,
			"--name",         appName,
			"--app-version",  appVersion,
			"--input",        input.absolutePath,
			"--main-jar",     tasks.jar.get().archiveFileName.get(),
			"--main-class",   application.mainClass.get(),
			"--dest",         dest.absolutePath,
			"--add-modules",  requiredJdkModules.joinToString(","),
			"--jlink-options", "--strip-debug --compress zip-6 --no-header-files --no-man-pages --strip-native-commands",
		))
		appJvmArgs.forEach { addAll(listOf("--java-options", it)) }
		icon?.let { addAll(listOf("--icon", file(it).absolutePath)) }
		if (packageType == "exe") addAll(listOf("--win-dir-chooser", "--win-menu", "--win-shortcut"))
	})

	// jpackage refuses to overwrite an existing app image
	doFirst { dest.resolve(appName).deleteForce() }
}

tasks.register("deploy") {
	group       = "distribution"
	description = "Packages the app and installs it to D:/app/SimpleLauncher (or C:/app/SimpleLauncher)"
	dependsOn("makeExecutable")

	val appName = application.applicationName
	val source  = layout.buildDirectory.dir("dist/$appName").get().asFile

	doLast {
		val target = listOf("d:/app/SimpleLauncher", "c:/app/SimpleLauncher").map(::File).firstOrNull { it.exists() }
			?: throw GradleException("No install directory: create D:/app/SimpleLauncher or C:/app/SimpleLauncher")

		// kill locked instance if it exists
		ProcessBuilder("taskkill", "/IM", "$appName.exe", "/F", "/T").redirectErrorStream(true).start().waitFor()

		// replace only what jpackage made: conf/ and db/ beside it are user data
		listOf("app", "runtime", "$appName.exe").forEach { name ->
			target.resolve(name).deleteForce()
			source.resolve(name).copyRecursively(target.resolve(name))
		}
	}
}

fun Project.findReleaseVersionFromBranch(): String? {
	val branchName = providers.environmentVariable("GITHUB_REF_NAME").orNull?.trim().takeUnless { it.isNullOrBlank() } ?: run {
		// no git, or not a repository: no branch, so a snapshot version
		runCatching {
			providers.exec {
				commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
				isIgnoreExitValue = true
			}.standardOutput.asText.get().trim()
		}.getOrNull()?.takeUnless { it.isBlank() || it == "HEAD" }
	}

	return branchName
		?.removePrefix("refs/heads/")
		?.takeIf { it.startsWith("release/") }
		?.removePrefix("release/")
		?.takeIf { it.isNotBlank() }
}
