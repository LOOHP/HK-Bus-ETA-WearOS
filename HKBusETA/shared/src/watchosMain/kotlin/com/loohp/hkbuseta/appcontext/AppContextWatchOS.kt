/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.a
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.loohp.hkbuseta.appcontext

import com.benasher44.uuid.uuid4
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppBundle
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppIntentResult
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.HapticFeedback
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.utils.BackgroundRestrictionType
import com.loohp.hkbuseta.common.utils.getTextResponse
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import io.ktor.utils.io.charsets.Charset
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toNSDateComponents
import platform.Foundation.NSBundle
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.URLByAppendingPathComponent
import platform.Foundation.create
import platform.Foundation.lastPathComponent
import platform.Foundation.writeToURL
import platform.WatchKit.WKInterfaceDevice
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue


val appContext: AppActiveContextWatchOS = AppActiveContextWatchOS()

private var firebaseImpl: (String, AppBundle) -> Unit = { _, _ -> }

fun setFirebaseLogImpl(handler: (String, AppBundle) -> Unit) {
    firebaseImpl = handler
}

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class, BetaInteropApi::class)
open class AppContextWatchOS internal constructor() : AppContext {

    override val packageName: String
        get() = NSBundle.mainBundle.bundleIdentifier?.split('.')?.subList(0, 3)?.joinToString(".") ?: "Unknown"

    override val versionName: String
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString")?.toString()?: "Unknown"

    override val versionCode: Long
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion")?.toString()?.toLong()?: -1L

    override val screenWidth: Int
        get() = WKInterfaceDevice.currentDevice().screenBounds.useContents { size.width }.toInt()

    override val screenHeight: Int
        get() = WKInterfaceDevice.currentDevice().screenBounds.useContents { size.height }.toInt()

    override val minScreenSize: Int
        get() = screenWidth.coerceAtMost(screenHeight)

    override val screenScale: Float
        get() = minScreenSize / 198F

    override val density: Float
        get() = 0F

    override val scaledDensity: Float
        get() = 0F

    override fun readTextFileLines(fileName: String, charset: Charset): List<String> {
        return NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first().let { dir ->
            dir as NSURL
            val fileURL = dir.URLByAppendingPathComponent(fileName)!!
            val text = NSString.create(contentsOfURL = fileURL)
            text.toString().split("\n").toList()
        }
    }

    override fun writeTextFileList(fileName: String, charset: Charset, writeText: () -> List<String>) {
        NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first().let { dir ->
            dir as NSURL
            val fileURL = dir.URLByAppendingPathComponent(fileName)!!
            val text = NSString.create(string = writeText.invoke().joinToString("\n"))
            text.writeToURL(fileURL, atomically = false)
        }
    }

    override fun listFiles(): List<String> {
        val fileManager = NSFileManager.defaultManager
        return fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first().let { dir ->
            dir as NSURL
            fileManager.contentsOfDirectoryAtURL(dir, includingPropertiesForKeys = null, options = 0u, error = null)?.mapNotNull {
                it as NSURL
                it.lastPathComponent
            }?: emptyList()
        }
    }

    override fun deleteFile(fileName: String): Boolean {
        val fileManager = NSFileManager.defaultManager
        return fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first().let { dir ->
            dir as NSURL
            val fileURL = dir.URLByAppendingPathComponent(fileName)!!
            if (fileManager.fileExistsAtPath(fileURL.path!!)) {
                fileManager.removeItemAtURL(fileURL, error = null)
            } else {
                false
            }
        }
    }

    override fun hasConnection(): Boolean {
        return getTextResponse("https://www.google.com/") != null
    }

    override fun currentBackgroundRestrictions(): BackgroundRestrictionType {
        return BackgroundRestrictionType.NONE
    }

    override fun logFirebaseEvent(title: String, values: AppBundle) {
        firebaseImpl.invoke(title, values)
    }

    override fun getResourceString(resId: Int): String {
        throw RuntimeException("Unsupported Platform Operation")
    }

    override fun isScreenRound(): Boolean {
        return false
    }

    override fun startActivity(appIntent: AppIntent) {
        throw RuntimeException("Unsupported Platform Operation")
    }

    override fun startForegroundService(appIntent: AppIntent) {
        throw RuntimeException("Unsupported Platform Operation")
    }

    override fun showToastText(text: String, duration: ToastDuration) {
        throw RuntimeException("Unsupported Platform Operation")
    }

    override fun formatTime(localDateTime: LocalDateTime): String {
        return NSCalendar.currentCalendar.dateFromComponents(localDateTime.toNSDateComponents())?.let {
            NSDateFormatter().apply {
                dateStyle = NSDateFormatterNoStyle
                timeStyle = NSDateFormatterShortStyle
            }.stringFromDate(it)
        }?: localDateTime.let { "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}" }
    }

}

class AppActiveContextWatchOS internal constructor() : AppContextWatchOS(), AppActiveContext {

    @NativeCoroutinesState
    val historyStack: MutableStateFlow<List<HistoryStackEntry>> = MutableStateFlow(listOf(HistoryStackEntry.DEFAULT_ENTRY))

    fun appendStack(screen: AppScreen) {
        appendStack(screen, emptyMap())
    }

    fun appendStack(screen: AppScreen, mutableData: MutableMap<String, Any?>) {
        appendStack(screen, data = mutableData)
    }

    fun appendStack(screen: AppScreen, data: Map<String, Any?>) {
        historyStack.value = historyStack.value.toMutableList().apply { add(HistoryStackEntry(screen, data)) }
    }

    fun peekStack(): HistoryStackEntry? {
        return historyStack.value.lastOrNull()
    }

    fun popStack(): HistoryStackEntry? {
        val stack = historyStack.value.toMutableList()
        val last = stack.removeLastOrNull()
        if (stack.isEmpty()) {
            stack.add(HistoryStackEntry.DEFAULT_ENTRY)
        }
        historyStack.value = stack
        return last
    }

    fun popStackIfMatches(predicate: (HistoryStackEntry) -> Boolean): HistoryStackEntry? {
        val stack = historyStack.value.toMutableList()
        val last = stack.lastOrNull()?.let {
            if (predicate.invoke(it)) {
                stack.removeLastOrNull()
                it
            } else {
                null
            }
        }
        if (stack.isEmpty()) {
            stack.add(HistoryStackEntry.DEFAULT_ENTRY)
        }
        historyStack.value = stack
        return last
    }

    fun popSecondLastStack(): HistoryStackEntry? {
        val stack = historyStack.value.toMutableList()
        if (stack.size < 2) {
            return null
        }
        val last = stack.removeAt(stack.size - 2)
        historyStack.value = stack
        return last
    }

    fun popSecondLastStackIfMatches(predicate: (HistoryStackEntry) -> Boolean): HistoryStackEntry? {
        val stack = historyStack.value.toMutableList()
        if (stack.size < 2) {
            return null
        }
        val last = stack.removeAt(stack.size - 2).let {
            if (predicate.invoke(it)) {
                stack.removeAt(stack.size - 2)
                it
            } else {
                null
            }
        }
        historyStack.value = stack
        return last
    }

    fun clearStack() {
        historyStack.value = listOf(HistoryStackEntry.DEFAULT_ENTRY)
    }

    override fun runOnUiThread(runnable: () -> Unit) {
        dispatch_async(dispatch_get_main_queue(), runnable)
    }

    override fun startActivity(appIntent: AppIntent, callback: (AppIntentResult) -> Unit) {
        throw RuntimeException("Unsupported Platform Operation")
    }

    override fun handleOpenMaps(lat: Double, lng: Double, label: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        throw RuntimeException("Unsupported Platform Operation")
    }

    override fun handleWebpages(url: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        throw RuntimeException("Unsupported Platform Operation")
    }

    override fun handleWebImages(url: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        throw RuntimeException("Unsupported Platform Operation")
    }

    override fun setResult(result: AppIntentResult) {
        throw RuntimeException("Unsupported Platform Operation")
    }

    override fun finish() {
        throw RuntimeException("Unsupported Platform Operation")
    }

    override fun finishAffinity() {
        throw RuntimeException("Unsupported Platform Operation")
    }

}

fun createMutableAppDataContainer(): MutableMap<String, Any?> {
    return HashMap()
}

fun dispatcherIO(task: () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch { task.invoke() }
}

data class HistoryStackEntry(val screen: AppScreen, val data: Map<String, Any?>, val storage: MutableMap<String, Any?> = mutableMapOf()) {

    companion object {

        val DEFAULT_ENTRY = HistoryStackEntry(AppScreen.MAIN, emptyMap())

    }

    private val id = uuid4()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HistoryStackEntry) return false

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}