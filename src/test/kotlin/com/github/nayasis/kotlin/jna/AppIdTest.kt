package com.github.nayasis.kotlin.jna

import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.WString
import com.sun.jna.ptr.PointerByReference

object AppIdTest {

    @JvmStatic
    fun main(args: Array<String>) {
//        currentProcessExplicitAppUserModelID = AppIdTest::class.java.name
//        setCurrentProcessExplicitAppUserModelID("merong")
        setCurrentProcessExplicitAppUserModelID(AppIdTest::class.java.name)
        println(getCurrentProcessExplicitAppUserModelID())
    }// here we leak native memory by lazyness

    // DO NOT DO THIS, IT'S JUST FOR TESTING PURPOSE AS I'M NOT FREEING THE MEMORY
    // AS REQUESTED BY THE DOCUMENTATION:
    //
    // http://msdn.microsoft.com/en-us/library/dd378419%28VS.85%29.aspx
    //
    // "The caller is responsible for freeing this string with CoTaskMemFree when
    // it is no longer needed"
    fun getCurrentProcessExplicitAppUserModelID(): String {
        val r = PointerByReference()
        if (GetCurrentProcessExplicitAppUserModelID(r).toLong() == 0L) {
            val p = r.value
            return p.getStringArray(0).toString() // here we leak native memory by lazyness
        }
        return "N/A"
    }

    fun setCurrentProcessExplicitAppUserModelID(appID: String) {
        if (SetCurrentProcessExplicitAppUserModelID(WString(appID)).toLong() != 0L) throw RuntimeException("unable to set current process explicit AppUserModelID to: $appID")
    }

    private external fun GetCurrentProcessExplicitAppUserModelID(appID: PointerByReference): NativeLong
    private external fun SetCurrentProcessExplicitAppUserModelID(appID: WString): NativeLong

    init {
        Native.register("shell32")
    }
}