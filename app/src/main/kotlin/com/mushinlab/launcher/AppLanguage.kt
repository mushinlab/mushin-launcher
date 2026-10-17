package com.mushinlab.launcher

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import com.mushinlab.launcher.feature.settings.AppLanguages

class AppLanguage(context: Context) {
    private val locales = context.getSystemService(LocaleManager::class.java)

    fun current(): String? = AppLanguages.match(locales.applicationLocales.toLanguageTags())

    fun set(tag: String?) {
        locales.applicationLocales = tag?.let(LocaleList::forLanguageTags) ?: LocaleList.getEmptyLocaleList()
    }
}
