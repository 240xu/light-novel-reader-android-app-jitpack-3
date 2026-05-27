package io.github.dmzz_yyhyy.lnrplugin

import android.util.Log
import io.nightfish.lightnovelreader.api.plugin.LightNovelReaderPlugin
import io.nightfish.lightnovelreader.api.plugin.Plugin
import io.nightfish.lightnovelreader.api.userdata.UserDataRepositoryApi

@Plugin(
    version = BuildConfig.VERSION_CODE,
    name = "Legado Engine",
    versionName = BuildConfig.VERSION_NAME,
    author = "LegadoLNR",
    description = "Legado parsing engine plugin for LightNovelReader",
    updateUrl = "",
    apiVersion = 4
)
class LegadoLnrPlugin(
    val userDataRepositoryApi: UserDataRepositoryApi
) : LightNovelReaderPlugin {

    companion object {
        private const val TAG = "LegadoLnrPlugin"
    }

    override fun onLoad() {
        Log.i(TAG, "Legado Engine plugin loaded")
    }
}
