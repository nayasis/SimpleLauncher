// Copyright 2020 Kalkidan Betre Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.nayasis.sample.swing.decoratedUi.customdecoration

import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinUser.WindowProc
import com.sun.jna.platform.win32.BaseTSD.LONG_PTR
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinDef.LRESULT

interface User32Ex: User32 {
    fun SetWindowLong(hWnd: HWND?, nIndex: Int, wndProc: WindowProc?): LONG_PTR?
    fun SetWindowLong(hWnd: HWND?, nIndex: Int, wndProc: LONG_PTR?): LONG_PTR?
    fun SetWindowLongPtr(hWnd: HWND?, nIndex: Int, wndProc: WindowProc?): LONG_PTR?
    fun SetWindowLongPtr(hWnd: HWND?, nIndex: Int, wndProc: LONG_PTR?): LONG_PTR?
    fun CallWindowProc(proc: LONG_PTR?, hWnd: HWND?, uMsg: Int, uParam: WPARAM?, lParam: LPARAM?): LRESULT?

    companion object {
        const val GWLP_WNDPROC = -4
    }
}