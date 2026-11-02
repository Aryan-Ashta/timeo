package com.timeo.app

import android.app.Application
import android.content.Context
import com.timeo.app.data.Repository
import com.timeo.app.data.TimeoDatabase

class TimeoApp : Application() {
    val repository: Repository by lazy {
        Repository(TimeoDatabase.build(this).dao(), this)
    }
}

val Context.repository: Repository get() = (applicationContext as TimeoApp).repository
