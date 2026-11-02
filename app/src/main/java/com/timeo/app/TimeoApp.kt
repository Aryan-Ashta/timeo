package com.timeo.app

import android.app.Application
import android.content.Context
import com.timeo.app.data.Repository
import com.timeo.app.data.TimeoDatabase
import com.timeo.app.widget.WidgetData
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class TimeoApp : Application() {
    val repository: Repository by lazy {
        Repository(TimeoDatabase.build(this).dao(), this)
    }

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        // Keep home screen widgets in step with changes made in the app.
        combine(repository.categories, repository.allSessions, repository.weekStart) { _, _, _ -> }
            .drop(1)
            .debounce(300)
            .onEach { WidgetData.refreshAll(this) }
            .launchIn(MainScope())
    }
}

val Context.repository: Repository get() = (applicationContext as TimeoApp).repository
