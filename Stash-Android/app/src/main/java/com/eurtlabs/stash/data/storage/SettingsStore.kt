package com.eurtlabs.stash.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.eurtlabs.stash.data.model.ColorTheme
import com.eurtlabs.stash.data.model.DownloadFormat
import com.eurtlabs.stash.data.model.DownloadQuality
import com.eurtlabs.stash.data.model.MediaType

object SettingsStore {
    private const val PREFS_NAME = "stash_settings"
    private const val KEY_THEME = "app_theme"
    private const val KEY_MEDIA_TYPE = "media_type"
    private const val KEY_AUDIO_FORMAT = "audio_format"
    private const val KEY_AUDIO_QUALITY = "audio_quality"
    private const val KEY_VIDEO_FORMAT = "video_format"
    private const val KEY_VIDEO_QUALITY = "video_quality"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveTheme(context: Context, theme: ColorTheme) {
        getPrefs(context).edit().putString(KEY_THEME, theme.name).apply()
    }

    fun loadTheme(context: Context): ColorTheme {
        val name = getPrefs(context).getString(KEY_THEME, ColorTheme.OBSIDIAN.name) ?: ColorTheme.OBSIDIAN.name
        return try {
            ColorTheme.valueOf(name)
        } catch (e: Exception) {
            ColorTheme.OBSIDIAN
        }
    }

    fun saveMediaType(context: Context, type: MediaType) {
        getPrefs(context).edit().putString(KEY_MEDIA_TYPE, type.name).apply()
    }

    fun loadMediaType(context: Context): MediaType {
        val name = getPrefs(context).getString(KEY_MEDIA_TYPE, MediaType.AUDIO.name) ?: MediaType.AUDIO.name
        return try {
            MediaType.valueOf(name)
        } catch (e: Exception) {
            MediaType.AUDIO
        }
    }

    fun saveAudioFormat(context: Context, format: DownloadFormat) {
        getPrefs(context).edit().putString(KEY_AUDIO_FORMAT, format.name).apply()
    }

    fun loadAudioFormat(context: Context): DownloadFormat {
        val name = getPrefs(context).getString(KEY_AUDIO_FORMAT, DownloadFormat.MP3.name) ?: DownloadFormat.MP3.name
        return try {
            DownloadFormat.valueOf(name)
        } catch (e: Exception) {
            DownloadFormat.MP3
        }
    }

    fun saveAudioQuality(context: Context, quality: DownloadQuality) {
        getPrefs(context).edit().putString(KEY_AUDIO_QUALITY, quality.name).apply()
    }

    fun loadAudioQuality(context: Context): DownloadQuality {
        val name = getPrefs(context).getString(KEY_AUDIO_QUALITY, DownloadQuality.AUDIO_320K.name) ?: DownloadQuality.AUDIO_320K.name
        return try {
            DownloadQuality.valueOf(name)
        } catch (e: Exception) {
            DownloadQuality.AUDIO_320K
        }
    }

    fun saveVideoFormat(context: Context, format: DownloadFormat) {
        getPrefs(context).edit().putString(KEY_VIDEO_FORMAT, format.name).apply()
    }

    fun loadVideoFormat(context: Context): DownloadFormat {
        val name = getPrefs(context).getString(KEY_VIDEO_FORMAT, DownloadFormat.MP4.name) ?: DownloadFormat.MP4.name
        return try {
            DownloadFormat.valueOf(name)
        } catch (e: Exception) {
            DownloadFormat.MP4
        }
    }

    fun saveVideoQuality(context: Context, quality: DownloadQuality) {
        getPrefs(context).edit().putString(KEY_VIDEO_QUALITY, quality.name).apply()
    }

    fun loadVideoQuality(context: Context): DownloadQuality {
        val name = getPrefs(context).getString(KEY_VIDEO_QUALITY, DownloadQuality.VIDEO_1080P.name) ?: DownloadQuality.VIDEO_1080P.name
        return try {
            DownloadQuality.valueOf(name)
        } catch (e: Exception) {
            DownloadQuality.VIDEO_1080P
        }
    }
}
