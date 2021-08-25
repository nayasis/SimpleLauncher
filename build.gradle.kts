import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {

	// kotlin
	kotlin("jvm") version "1.4.32"
	kotlin("plugin.serialization") version "1.4.32"

	// spring
	id("org.springframework.boot") version "2.3.5.RELEASE"
	id("io.spring.dependency-management") version "1.0.10.RELEASE"
	kotlin("plugin.jpa") version "1.4.20"
	kotlin("plugin.noarg") version "1.4.20"
	kotlin("plugin.allopen") version "1.4.20"
	kotlin("plugin.spring") version "1.4.20"

	// javafx
	application
	id("org.openjfx.javafxplugin") version "0.0.8"

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
	invokeInitializers = true
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
	mainClassName = "com.github.nayasis.simplelauncher.Simplelauncher"
}

javafx {
	version = "11.0.2"
	modules = listOf("javafx.controls","javafx.fxml","javafx.web","javafx.swing")
}

group = "com.github.nayasis"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

configurations.all {
	resolutionStrategy.cacheChangingModulesFor(  0, "seconds" )
	resolutionStrategy.cacheDynamicVersionsFor(  5, "minutes" )
}

repositories {
	mavenLocal()
	mavenCentral()
	jcenter()
	maven { url = uri("https://jitpack.io") }
}

dependencies {

	// common
	implementation("com.github.nayasis:basica-kt:develop-SNAPSHOT"){ isChanging = true }
//	implementation("commons-io:commons-io:2.4")
//	implementation("commons-cli:commons-cli:1.4")
//	implementation("commons-codec:commons-codec:1.13")
	implementation("ch.qos.logback:logback-classic:1.2.3")

	// basicafx
	implementation( "org.jclarion:image4j:0.7" )
	implementation( "org.apache.commons:commons-exec:1.3" )
	implementation("org.apache.httpcomponents:httpclient:4.5.8")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.+")

	// application
	implementation( "no.tornado:tornadofx:1.7.20" )
	implementation("de.jensd:fontawesomefx:8.9")
	implementation("org.controlsfx:controlsfx:8.40.10")
	implementation( "com.github.vatbub:mslinks:1.0.6" )
	implementation( "commons-cli:commons-cli:1.4" )

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
//	implementation("org.xerial:sqlite-jdbc:3.34.0")
//	implementation("com.zsoltfabok:sqlite-dialect:1.0")
//	implementation("com.github.gwenn:sqlite-dialect:0.1.0")
	implementation("com.h2database:h2")

	// kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
	implementation( "io.github.microutils:kotlin-logging:2.0.10" )
	implementation("au.com.console:kassava:2.1.0")

	testImplementation("org.apache.pdfbox:pdfbox:2.0.16")
	testImplementation("com.levigo.jbig2:levigo-jbig2-imageio:2.0")
	testImplementation("org.apache.httpcomponents:httpclient:4.5.13")

	testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
	testImplementation("org.junit.jupiter:junit-jupiter-engine:5.3.1")
	testImplementation("ch.qos.logback:logback-classic:1.2.3")

}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf(
			"-Xjsr305=strict",
			"-Xuse-experimental=kotlinx.serialization.ExperimentalSerializationApi"
		)
		jvmTarget = "11"
	}
}