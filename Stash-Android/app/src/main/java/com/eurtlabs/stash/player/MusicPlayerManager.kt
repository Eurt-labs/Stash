package com.eurtlabs.stash.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.eurtlabs.stash.data.downloader.InnerTubeMusicRepository
import com.eurtlabs.stash.data.lyrics.LyricsRepository
import com.eurtlabs.stash.data.model.DownloadItem
import com.eurtlabs.stash.data.model.LyricLine
import com.eurtlabs.stash.data.model.PlaybackRepeatMode
import com.eurtlabs.stash.data.model.SearchResultItem
import com.eurtlabs.stash.data.model.TrackInfo
import com.eurtlabs.stash.service.MusicPlaybackService
import com.eurtlabs.stash.util.ArtworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PlayerState(
    val currentTrack: TrackInfo? = null,
    val currentItem: DownloadItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isStreaming: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.OFF,
    val isShuffleEnabled: Boolean = false,
    val queue: List<TrackInfo> = emptyList(),
    val queueIndex: Int = 0,
    val syncedLyrics: List<LyricLine> = emptyList(),
    val plainLyrics: String? = null,
    val isLyricsLoading: Boolean = false,
    val sleepTimerRemainingSeconds: Int = 0,
    val currentEqPreset: String = "No Effect (Off)",
    val bassBoostStrength: Int = 0
)

@OptIn(UnstableApi::class)
object MusicPlayerManager {

    private const val TAG = "MusicPlayerManager"

    private var exoPlayer: ExoPlayer? = null
    private var appContext: Context? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null
    private var lyricsJob: Job? = null
    private var artworkJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var playbackStreamJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(8000)
        .setReadTimeoutMs(12000)

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }

    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15000,
                /* maxBufferMs = */ 60000,
                /* bufferForPlaybackMs = */ 200,
                /* bufferForPlaybackAfterRebufferMs = */ 800
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        exoPlayer = ExoPlayer.Builder(appContext!!)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                shuffleModeEnabled = false

                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
                        if (isPlaying) {
                            startProgressUpdates()
                        } else {
                            stopProgressUpdates()
                        }
                        appContext?.let { ctx ->
                            val pos = exoPlayer?.currentPosition ?: 0L
                            val dur = exoPlayer?.duration ?: 0L
                            MusicPlaybackService.update(ctx, _playerState.value.currentTrack, isPlaying, pos, dur)
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                _playerState.value = _playerState.value.copy(isBuffering = true)
                            }
                            Player.STATE_READY -> {
                                _playerState.value = _playerState.value.copy(
                                    isBuffering = false,
                                    durationMs = duration.coerceAtLeast(0L)
                                )
                                attachAudioEffects(audioSessionId)
                            }
                            Player.STATE_ENDED -> {
                                _playerState.value = _playerState.value.copy(isBuffering = false)
                                onTrackEnded()
                            }
                            Player.STATE_IDLE -> {
                                _playerState.value = _playerState.value.copy(isBuffering = false)
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "ExoPlayer error: ${error.errorCodeName} - ${error.message}", error)
                        _playerState.value = _playerState.value.copy(isBuffering = false)
                    }
                })
            }

        try {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            appContext?.registerReceiver(noisyReceiver, filter)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register noisy receiver: ${e.message}")
        }
    }

    /**
     * Plays a downloaded track from the local library.
     */
    fun playLibraryItem(item: DownloadItem, allItems: List<DownloadItem> = emptyList()) {
        val path = item.finalFilePath
        if (path.isNullOrBlank() || !File(path).exists()) {
            Log.e(TAG, "File does not exist: $path")
            return
        }

        val track = item.trackInfo
        val queueTracks = if (allItems.isNotEmpty()) allItems.map { it.trackInfo } else listOf(track)
        val index = queueTracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        _playerState.value = _playerState.value.copy(
            currentTrack = track,
            currentItem = item,
            isStreaming = false,
            queue = queueTracks,
            queueIndex = index,
            syncedLyrics = emptyList(),
            plainLyrics = null,
            isLyricsLoading = true
        )

        val uri = Uri.fromFile(File(path))
        val mediaItem = MediaItem.fromUri(uri)

        exoPlayer?.apply {
            stop()
            clearMediaItems()
            setMediaItem(mediaItem)
            prepare()
            play()
        }

        appContext?.let { ctx ->
            MusicPlaybackService.start(ctx, track, isPlaying = true, positionMs = 0L, durationMs = track.durationMs)
        }

        fetchStudioArtworkForTrack(track)
        fetchLyricsForTrack(track)
        prefetchQueue(index)
    }

    /**
     * Instantly streams an online song from search or discovery.
     */
    fun playOnlineTrack(searchItem: SearchResultItem, allItems: List<SearchResultItem> = emptyList()) {
        val initialArtUrl = ArtworkUtils.getHighResArtworkUrl(searchItem.thumbnailUrl, searchItem.id)

        val track = TrackInfo(
            id = searchItem.id,
            title = searchItem.title,
            artists = listOf(searchItem.artist),
            durationMs = 0L,
            albumArtUrl = initialArtUrl,
            source = com.eurtlabs.stash.data.model.Platform.YOUTUBE_MUSIC,
            sourceUrl = searchItem.url,
            safeFileName = searchItem.title
        )

        val queueTracks = if (allItems.isNotEmpty()) {
            allItems.map { item ->
                TrackInfo(
                    id = item.id,
                    title = item.title,
                    artists = listOf(item.artist),
                    durationMs = 0L,
                    albumArtUrl = ArtworkUtils.getHighResArtworkUrl(item.thumbnailUrl, item.id),
                    source = com.eurtlabs.stash.data.model.Platform.YOUTUBE_MUSIC,
                    sourceUrl = item.url,
                    safeFileName = item.title
                )
            }
        } else listOf(track)

        val index = queueTracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        _playerState.value = _playerState.value.copy(
            currentTrack = track,
            currentItem = null,
            isStreaming = true,
            isBuffering = true,
            queue = queueTracks,
            queueIndex = index,
            syncedLyrics = emptyList(),
            plainLyrics = null,
            isLyricsLoading = true
        )

        appContext?.let { ctx ->
            MusicPlaybackService.start(ctx, track, isPlaying = true, positionMs = 0L, durationMs = track.durationMs)
        }

        playbackStreamJob?.cancel()
        playbackStreamJob = scope.launch {
            val streamUrl = InnerTubeMusicRepository.resolveStreamUrl(searchItem.id)
            if (streamUrl != null) {
                withContext(Dispatchers.Main) {
                    try {
                        val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
                        val mediaSource = ProgressiveMediaSource.Factory(httpDataSourceFactory)
                            .createMediaSource(mediaItem)

                        exoPlayer?.apply {
                            stop()
                            clearMediaItems()
                            setMediaSource(mediaSource)
                            prepare()
                            play()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start ExoPlayer stream: ${e.message}")
                        _playerState.value = _playerState.value.copy(isBuffering = false)
                    }
                }
            } else {
                Log.e(TAG, "Could not resolve stream URL for ${searchItem.id}")
                _playerState.value = _playerState.value.copy(isBuffering = false)
            }
        }

        fetchStudioArtworkForTrack(track)
        fetchLyricsForTrack(track)
        prefetchQueue(index)
    }

    fun playTrack(item: DownloadItem, queue: List<DownloadItem> = emptyList()) {
        playLibraryItem(item, queue)
    }

    fun playTrackFromQueue(index: Int) {
        val state = _playerState.value
        val track = state.queue.getOrNull(index) ?: return

        _playerState.value = state.copy(
            currentTrack = track,
            currentItem = null,
            queueIndex = index,
            isStreaming = true,
            isBuffering = true,
            syncedLyrics = emptyList(),
            plainLyrics = null,
            isLyricsLoading = true
        )

        appContext?.let { ctx ->
            MusicPlaybackService.start(ctx, track, isPlaying = true, positionMs = 0L, durationMs = track.durationMs)
        }

        scope.launch {
            val streamUrl = InnerTubeMusicRepository.resolveStreamUrl(track.id)
            if (streamUrl != null) {
                withContext(Dispatchers.Main) {
                    try {
                        val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
                        val mediaSource = ProgressiveMediaSource.Factory(httpDataSourceFactory)
                            .createMediaSource(mediaItem)

                        exoPlayer?.apply {
                            stop()
                            clearMediaItems()
                            setMediaSource(mediaSource)
                            prepare()
                            play()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error playing track from queue: ${e.message}")
                    }
                }
            }
        }

        fetchStudioArtworkForTrack(track)
        fetchLyricsForTrack(track)
        prefetchQueue(index)
    }

    private fun prefetchQueue(currentIndex: Int) {
        val state = _playerState.value
        val upcoming = listOf(currentIndex + 1, currentIndex + 2)
        for (idx in upcoming) {
            val nextTrack = state.queue.getOrNull(idx) ?: continue
            scope.launch(Dispatchers.IO) {
                InnerTubeMusicRepository.resolveStreamUrl(nextTrack.id)
                val artist = nextTrack.artists.firstOrNull() ?: ""
                ArtworkUtils.fetchStudioArtwork(nextTrack.title, artist, nextTrack.id, nextTrack.albumArtUrl)
            }
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun stop() {
        exoPlayer?.stop()
        _playerState.value = _playerState.value.copy(isPlaying = false, currentTrack = null, currentItem = null)
        appContext?.let { ctx ->
            MusicPlaybackService.stop(ctx)
        }
    }

    fun closePlayer() {
        stop()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
    }

    fun skipForward10() {
        val current = exoPlayer?.currentPosition ?: 0L
        seekTo(current + 10_000L)
    }

    fun skipBackward10() {
        val current = exoPlayer?.currentPosition ?: 0L
        seekTo((current - 10_000L).coerceAtLeast(0L))
    }

    fun skipNext() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return

        val nextIndex = if (state.isShuffleEnabled) {
            (0 until state.queue.size).random()
        } else {
            (state.queueIndex + 1) % state.queue.size
        }

        playTrackFromQueue(nextIndex)
    }

    fun skipPrevious() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return

        if ((exoPlayer?.currentPosition ?: 0L) > 3000L) {
            seekTo(0L)
            return
        }

        val prevIndex = if (state.queueIndex - 1 < 0) state.queue.size - 1 else state.queueIndex - 1
        playTrackFromQueue(prevIndex)
    }

    fun toggleShuffle() {
        val nextShuffle = !_playerState.value.isShuffleEnabled
        _playerState.value = _playerState.value.copy(isShuffleEnabled = nextShuffle)
        exoPlayer?.shuffleModeEnabled = nextShuffle
    }

    fun toggleRepeatMode() {
        val current = _playerState.value.repeatMode
        val next = when (current) {
            PlaybackRepeatMode.OFF -> PlaybackRepeatMode.ALL
            PlaybackRepeatMode.ALL -> PlaybackRepeatMode.ONE
            PlaybackRepeatMode.ONE -> PlaybackRepeatMode.OFF
        }
        _playerState.value = _playerState.value.copy(repeatMode = next)

        exoPlayer?.repeatMode = when (next) {
            PlaybackRepeatMode.OFF -> Player.REPEAT_MODE_OFF
            PlaybackRepeatMode.ALL -> Player.REPEAT_MODE_ALL
            PlaybackRepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun setEqualizerPreset(presetName: String) {
        _playerState.value = _playerState.value.copy(currentEqPreset = presetName)
        try {
            val eq = equalizer ?: return

            if (presetName.startsWith("No Effect", ignoreCase = true) || presetName.equals("Off", ignoreCase = true)) {
                eq.enabled = false
                return
            }

            val numBands = eq.numberOfBands
            val minEQLevel = eq.bandLevelRange[0]
            val maxEQLevel = eq.bandLevelRange[1]

            val targetGains = when (presetName.lowercase()) {
                "bass boost" -> listOf(7, 5, 2, 0, -1)
                "vocal clarity" -> listOf(-2, 1, 6, 5, 3)
                "electronic" -> listOf(6, 4, 0, 3, 5)
                "rock" -> listOf(5, 3, -1, 3, 6)
                "acoustic" -> listOf(3, 2, 2, 4, 3)
                "deep lounge" -> listOf(6, 4, 1, 3, 2)
                else -> listOf(0, 0, 0, 0, 0)
            }

            for (i in 0 until minOf(numBands.toInt(), targetGains.size)) {
                val gain = targetGains[i]
                val level = ((gain / 10f) * maxEQLevel).toInt().coerceIn(minEQLevel.toInt(), maxEQLevel.toInt())
                eq.setBandLevel(i.toShort(), level.toShort())
            }
            eq.enabled = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply EQ preset: ${e.message}")
        }
    }

    fun setBassBoost(strengthPercent: Int) {
        _playerState.value = _playerState.value.copy(bassBoostStrength = strengthPercent)
        try {
            val bb = bassBoost ?: return
            if (bb.strengthSupported) {
                val strength = ((strengthPercent / 100f) * 1000).toInt().coerceIn(0, 1000)
                bb.setStrength(strength.toShort())
                bb.enabled = strengthPercent > 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set bass boost: ${e.message}")
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _playerState.value = _playerState.value.copy(sleepTimerRemainingSeconds = 0)
            return
        }

        var remainingSeconds = minutes * 60
        _playerState.value = _playerState.value.copy(sleepTimerRemainingSeconds = remainingSeconds)

        sleepTimerJob = scope.launch {
            while (remainingSeconds > 0 && isActive) {
                delay(1000)
                remainingSeconds--
                _playerState.value = _playerState.value.copy(sleepTimerRemainingSeconds = remainingSeconds)
            }
            pause()
            _playerState.value = _playerState.value.copy(sleepTimerRemainingSeconds = 0)
        }
    }

    private fun onTrackEnded() {
        val state = _playerState.value
        when (state.repeatMode) {
            PlaybackRepeatMode.ONE -> {
                seekTo(0L)
                play()
            }
            PlaybackRepeatMode.ALL -> {
                skipNext()
            }
            PlaybackRepeatMode.OFF -> {
                if (state.queueIndex < state.queue.size - 1) {
                    skipNext()
                } else {
                    pause()
                    seekTo(0L)
                }
            }
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val player = exoPlayer
                if (player != null && player.isPlaying) {
                    _playerState.value = _playerState.value.copy(
                        currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = player.duration.coerceAtLeast(0L)
                    )
                }
                delay(200)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
    }

    private fun fetchStudioArtworkForTrack(track: TrackInfo) {
        artworkJob?.cancel()
        artworkJob = scope.launch(Dispatchers.IO) {
            val artist = track.artists.firstOrNull() ?: ""
            val studioArt = ArtworkUtils.fetchStudioArtwork(
                title = track.title,
                artist = artist,
                videoId = track.id,
                fallbackUrl = track.albumArtUrl
            )

            if (studioArt.isNotBlank() && studioArt != track.albumArtUrl) {
                withContext(Dispatchers.Main) {
                    if (_playerState.value.currentTrack?.id == track.id) {
                        _playerState.value = _playerState.value.copy(
                            currentTrack = _playerState.value.currentTrack?.copy(albumArtUrl = studioArt)
                        )
                        appContext?.let { ctx ->
                            MusicPlaybackService.update(ctx, _playerState.value.currentTrack, _playerState.value.isPlaying)
                        }
                    }
                }
            }
        }
    }

    private fun fetchLyricsForTrack(track: TrackInfo) {
        lyricsJob?.cancel()
        lyricsJob = scope.launch(Dispatchers.IO) {
            val artist = track.artists.firstOrNull() ?: "Unknown"
            val lyricsResult = LyricsRepository.fetchLyrics(
                trackTitle = track.title,
                artistName = artist,
                durationMs = track.durationMs
            )

            withContext(Dispatchers.Main) {
                _playerState.value = _playerState.value.copy(
                    syncedLyrics = lyricsResult?.syncedLyrics ?: emptyList(),
                    plainLyrics = lyricsResult?.plainLyrics,
                    isLyricsLoading = false
                )
            }
        }
    }

    private fun attachAudioEffects(sessionId: Int) {
        try {
            if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId != 0) {
                if (equalizer == null) {
                    equalizer = Equalizer(0, sessionId)
                }
                if (bassBoost == null) {
                    bassBoost = BassBoost(0, sessionId)
                }
                setEqualizerPreset(_playerState.value.currentEqPreset)
                setBassBoost(_playerState.value.bassBoostStrength)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach hardware audio effects: ${e.message}")
        }
    }
}
