package com.skooldev.shweep

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIWindowScene

actual fun shareText(text: String, title: String) {
    val sharedApplication = UIApplication.sharedApplication
    val rootViewController = getKeyWindowScene(sharedApplication)?.keyWindow?.rootViewController ?: return

    val activityViewController = UIActivityViewController(
        activityItems = listOf(text),
        applicationActivities = null
    )
    @Suppress("CAST_NEVER_SUCCEEDS")
    (rootViewController as? platform.UIKit.UIViewController)?.presentViewController(
        activityViewController,
        animated = true,
        completion = null
    )
}

private fun getKeyWindowScene(application: UIApplication): UIWindowScene? {
    val connectedScenes = application.connectedScenes
    val iterator = connectedScenes.iterator()
    while (iterator.hasNext()) {
        val scene = iterator.next()
        if (scene is UIWindowScene) {
            return scene
        }
    }
    return null
}
