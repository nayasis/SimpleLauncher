import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.6.3"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	kotlin("jvm") version "1.6.10"
	kotlin("plugin.jpa") version "1.6.10"
	kotlin("plugin.noarg") version "1.6.10"
	kotlin("plugin.allopen") version "1.6.10"
	kotlin("plugin.spring") version "1.6.10"

	// javafx
	application
	id("org.openjfx.javafxplugin") version "0.0.10"
	id("org.beryx.jlink") version "2.22.1"

}

val compileKotlin: KotlinCompile by tasks
val compileJava: JavaCompile by tasks

compileJava.destinationDirectory.set(compileKotlin.destinationDirectory)

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
//	mainModule.set("main.kotlin")
//	mainClass.set("org.beryx.jlink.test.kotlin.JavaFX")
}

javafx {
	version = "19"
	modules = listOf("javafx.graphics","javafx.controls","javafx.fxml","javafx.web","javafx.swing")
}

group = "com.github.nayasis"
version = "0.1.3"
java.sourceCompatibility = JavaVersion.VERSION_11

configurations.all {
	resolutionStrategy.cacheChangingModulesFor(0, "seconds")
	resolutionStrategy.cacheDynamicVersionsFor(5, "minutes")
}

repositories {
	mavenLocal()
	mavenCentral()
	jcenter()
	maven { url = uri("https://jitpack.io") }
}

dependencies {

	// application
	implementation("com.github.nayasis:basica-kt:0.2.14")
//	implementation("com.github.nayasis:basica-kt:develop-SNAPSHOT"){ isChanging = true }
//	implementation("com.github.nayasis:basicafx-kt:0.1.17")
	implementation("com.github.nayasis:basicafx-kt:develop-SNAPSHOT"){ isChanging = true }
//	implementation("com.github.nayasis:basicafx-kt:0.1.13-SNAPSHOT"){ isChanging = true }
	implementation("no.tornado:tornadofx:1.7.20") {
		exclude("org.jetbrains.kotlin")
	}
	implementation("org.controlsfx:controlsfx:11.1.0")
	implementation("com.github.vatbub:mslinks:1.0.6.2")
	implementation("commons-cli:commons-cli:1.4")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.+")
	implementation("no.tornado:tornadofx:1.7.20")

	// spring
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-aop")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
		exclude(group="com.vaadin.external.google",module="android-json")
	}

	// db
	implementation("com.h2database:h2")

	// kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("io.github.microutils:kotlin-logging:2.0.10")
	implementation("au.com.console:kassava:2.1.0")

	// test
	testImplementation("de.jensd:fontawesomefx:8.9")
	testImplementation("org.apache.pdfbox:pdfbox:2.0.16")
	testImplementation("com.levigo.jbig2:levigo-jbig2-imageio:2.0")
	testImplementation("org.apache.httpcomponents:httpclient:4.5.13")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
	testImplementation("org.junit.jupiter:junit-jupiter-engine:5.3.1")
	testImplementation("ch.qos.logback:logback-classic:1.2.3")

	// JNA (windows)
	testImplementation("net.java.dev.jna:jna:5.9.0")
	testImplementation("net.java.dev.jna:jna-platform:5.9.0")

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

jlink {
	launcher {
		name = "Simple Launcher"
	}
	addOptions("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")
	addExtraDependencies("javafx")
	imageZip.set(project.file("${project.buildDir}/image-zip/simple-launcher.zip"))

//	mergedModule {
//            requires("org.controlsfx.controls")
//            requires("com.fasterxml.classmate")
//            requires("java.json")
//            requires("javafx.swing")
//            requires("java.xml")
//            requires("java.desktop")
//            requires("javafx.base")
//            requires("jdk.jfr")
//            requires("jdk.unsupported")
//            requires("org.apache.tomcat.embed.el")
//            requires("java.management")
//            requires("java.naming")
//            requires("java.instrument")
//            requires("jakarta.activation")
//            requires("javafx.graphics")
//            requires("com.fasterxml.jackson.annotation")
//            requires("java.logging")
//            requires("jdk.httpserver")
//            requires("java.sql")
//            requires("java.prefs")
//            requires("java.rmi")
//            requires("java.security.jgss")
//            requires("javafx.fxml")
//            requires("java.xml.bind")
//            requires("javafx.web")
//            requires("javafx.controls")
//            requires("java.scripting")
//            requires("javafx.media")
//            requires("java.sql.rowset")
//            requires("java.compiler")
//            requires("java.transaction.xa")
//	}
}