@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "EXPOSED_PARAMETER_TYPE")
package moe.tlaster.precompose

import androidx.compose.runtime.Composable
import androidx.compose.ui.createSkiaLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.native.ComposeLayer
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.AppKit.NSBackingStoreBuffered
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowDelegateProtocol
import platform.AppKit.NSWindowDidResizeNotification
import platform.AppKit.NSWindowStyleMaskClosable
import platform.AppKit.NSWindowStyleMaskFullSizeContentView
import platform.AppKit.NSWindowStyleMaskMiniaturizable
import platform.AppKit.NSWindowStyleMaskResizable
import platform.AppKit.NSWindowStyleMaskTitled
import platform.AppKit.NSWindowTitleHidden
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSMakeRect
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.darwin.NSObject

internal class ComposeWindow(
    hideTitleBar: Boolean = false,
) : NSObject(), NSWindowDelegateProtocol {
    private val layer = ComposeLayer(
        layer = createSkiaLayer(),
        showSoftwareKeyboard = {},
        hideSoftwareKeyboard = {},
        getTopLeftOffset = { Offset.Zero },
    )

    val title: String
        get() = nsWindow.title()

    fun setTitle(title: String) {
        nsWindow.setTitle(title)
    }

    private val windowStyle =
        (
            NSWindowStyleMaskTitled or
                NSWindowStyleMaskMiniaturizable or
                NSWindowStyleMaskClosable or
                NSWindowStyleMaskResizable
            ).let {
            if (hideTitleBar) {
                it or NSWindowStyleMaskFullSizeContentView
            } else {
                it
            }
        }

    private val contentRect = NSMakeRect(0.0, 0.0, 640.0, 480.0)

    private val nsWindow = NSWindow(
        contentRect = contentRect,
        styleMask = windowStyle,
        backing = NSBackingStoreBuffered,
        defer = true,
    ).apply {
        if (hideTitleBar) {
            titlebarAppearsTransparent = true
            titleVisibility = NSWindowTitleHidden
        }
    }

    init {
        layer.layer.attachTo(nsWindow)
        nsWindow.orderFrontRegardless()
        nsWindow.delegate = this
        NSNotificationCenter.defaultCenter().addObserver(
            this,
            selector = NSSelectorFromString("windowDidResize:"),
            name = NSWindowDidResizeNotification,
            `object` = nsWindow,
        )
        updateLayerSize()
    }

    @ObjCAction
    override fun windowDidResize(notification: NSNotification) {
        updateLayerSize()
    }

    private fun updateLayerSize() {
        val (w, h) = nsWindow.contentView!!.frame.useContents {
            size.width to size.height
        }
        layer.setSize(w.toInt(), h.toInt())
        layer.layer.nsView.frame = CGRectMake(0.0, 0.0, w, h)
        layer.layer.redrawer?.syncSize()
        layer.layer.redrawer?.redrawImmediately()
    }

    /**
     * Sets Compose content of the ComposeWindow.
     *
     * @param content Composable content of the ComposeWindow.
     */
    fun setContent(
        content: @Composable () -> Unit
    ) {
        layer.setContent(
            content = content
        )
    }

    // TODO: need to call .dispose() on window close.
    fun dispose() {
        layer.dispose()
    }
}
