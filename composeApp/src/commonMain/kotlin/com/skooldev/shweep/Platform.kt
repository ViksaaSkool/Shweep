package com.skooldev.shweep

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun shareText(text: String, title: String)