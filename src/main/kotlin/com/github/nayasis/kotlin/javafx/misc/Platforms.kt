package com.github.nayasis.kotlin.javafx.misc

val Platforms = Meta(
    Os(
        System.getProperty("os.name").toLowerCase(),
        System.getProperty("os.arch"),
        System.getProperty("os.version"),
        System.getProperty("sun.jnu.encoding"),
    ),
    System.getProperty("sun.arch.data.model")
)

data class Meta(
    val os: Os,
    val jvm: String
) {
    fun isWindows(): Boolean = os.name.contains("win")
    fun isLinux(): Boolean = os.name.contains("linux")
    fun isUnix(): Boolean = os.name.contains("unix")
    fun isSolaris(): Boolean = os.name.contains("solaris") || os.name.contains("sunos")
    fun isMac(): Boolean = os.name.contains("mac")
    fun isAndroid(): Boolean = "Dalvik" == System.getProperty("java.vm.name")
}

data class Os(
    val name: String,
    val architecture: String,
    val version: String,
    val charset: String,
)