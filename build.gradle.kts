import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
//	id("org.springframework.boot") version "2.3.5.RELEASE"
//	id("io.spring.dependency-management") version "1.0.10.RELEASE"
	kotlin("jvm") version "1.4.20"
//	application
	kotlin("plugin.serialization") version "1.4.20"
//	kotlin("plugin.spring") version "1.4.20"
//	kotlin("plugin.allopen") version "1.4.20"
//	kotlin("plugin.noarg") version "1.4.20"
//	kotlin("plugin.jpa") version "1.4.20"
}

//application {
////	mainClassName = "com.github.nayasis.helloworld.HelloWorldApp"
//	mainClassName = "com.github.nayasis.example.Squeeze"
//}

//allOpen {
//	annotation("javax.persistence.Entity")
//	annotation("javax.persistence.MappedSuperclass")
//	annotation("javax.persistence.Embeddable")
//}
//
//noArg {
//	annotation("javax.persistence.Entity")
//	annotation("javax.persistence.MappedSuperclass")
//	annotation("javax.persistence.Embeddable")
////	annotation("com.onestorecorp.point.api.biz.common.annotation.NoArg")
//	invokeInitializers = true
//}

group = "com.github.nayasis"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

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
	implementation( "com.github.nayasis:basica:0.3.6-SNAPSHOT" ){ isChanging = true }
	implementation( "org.apache.httpcomponents:httpclient:4.4" )
//	implementation( "commons-io:commons-io:2.4" )
//	implementation( "commons-cli:commons-cli:1.4" )
//	implementation( "commons-codec:commons-codec:1.13" )

	// application
	implementation( "no.tornado:tornadofx:1.7.20" )
	implementation( "com.github.vatbub:mslinks:1.0.6" )

//	// spring
//	implementation("org.springframework.boot:spring-boot-starter")
//	implementation("org.springframework.boot:spring-boot-starter-aop")
//	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
//	implementation("org.springframework.boot:spring-boot-starter-cache")
//	implementation("org.springframework.boot:spring-boot-starter-validation")
//	implementation( "com.h2database:h2" )
//	testImplementation("org.springframework.boot:spring-boot-starter-test") {
//		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
//		exclude(group="com.vaadin.external.google",module="android-json")
//	}

	implementation("de.jensd:fontawesomefx:8.9")
	implementation("org.controlsfx:controlsfx:8.40.10")

	// kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
//	implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.1")
	implementation( "io.github.microutils:kotlin-logging:1.8.3" )
	implementation("au.com.console:kassava:2.1.0-rc.1")

	testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
	testImplementation("org.junit.jupiter:junit-jupiter-engine:5.3.1")
	testImplementation("ch.qos.logback:logback-classic:1.2.3")

}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "1.8"
	}
}