package com.thelightphone.sdk

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
val SimpleLightScreen<*>.androidContext: android.content.Context
    get() = this.activity
