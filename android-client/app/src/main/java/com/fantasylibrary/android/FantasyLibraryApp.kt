package com.fantasylibrary.android

import android.app.Application
import coil.Coil
import com.fantasylibrary.android.di.AppContainer

class FantasyLibraryApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Coil.setImageLoader(container.imageLoader)
    }
}
