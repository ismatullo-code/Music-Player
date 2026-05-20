package com.example.audio

import android.content.ContentUris
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.*
import java.io.File

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: String,
    val isSynth: Boolean = false,
    val synthPresetIdx: Int = 0,
    val coverColorStart: Long = 0xFF6C11D9, // Deep Purple
    val coverColorEnd: Long = 0xFF00FFCC,   // Neon Cyber Cyan
    val path: String = ""
)

data class Playlist(
    val name: String,
    val songs: SnapshotStateList<Song> = mutableStateListOf()
)

class AudioPlayerManager private constructor() {
    companion object {
        private var instance: AudioPlayerManager? = null
        fun getInstance(): AudioPlayerManager {
            if (instance == null) {
                instance = AudioPlayerManager()
            }
            return instance!!
        }
    }

    private val synthEngine = SynthwaveEngine()
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    // Synth tracks seeded by default to guarantee high-fidelity music offline instantly
    private val synthSongs = listOf(
        Song(
            id = "synth_pulse",
            title = "CYBERNETIC PULSE",
            artist = "VIPER-Z",
            album = "FUTURE GRID CORE",
            duration = 180000, // 3 min mock
            uri = "synth:0",
            isSynth = true,
            synthPresetIdx = 0,
            coverColorStart = 0xFF5B00FF, // Electric Purple
            coverColorEnd = 0xFF00FFF0    // Cyber Cyan
        ),
        Song(
            id = "synth_neon",
            title = "NEON NIGHTRIDER",
            artist = "GLOWGRID",
            album = "SYNTHETIC DRIVER",
            duration = 240000, 
            uri = "synth:1",
            isSynth = true,
            synthPresetIdx = 1,
            coverColorStart = 0xFFFF007F, // Razor Magenta
            coverColorEnd = 0xFF7B00FF    // Electric Indigo
        ),
        Song(
            id = "synth_toxic",
            title = "TOXIC WASTEWATER",
            artist = "SUBCORE-9",
            album = "SEWER ELECTRO",
            duration = 210000,
            uri = "synth:2",
            isSynth = true,
            synthPresetIdx = 2,
            coverColorStart = 0xFF39FF14, // Toxic Green
            coverColorEnd = 0xFF00BFFF    // Deep Sky Blue
        ),
        Song(
            id = "synth_vision",
            title = "VISION SLATE",
            artist = "ETHEREAL STREAM",
            album = "SPATIAL CHORDS",
            duration = 300000,
            uri = "synth:3",
            isSynth = true,
            synthPresetIdx = 3,
            coverColorStart = 0xFFFF4500, // Carbon Orange
            coverColorEnd = 0xFFFFD700    // Luxury Gold
        )
    )

    // Observable States for Jetpack Compose UI
    val allSongs = mutableStateListOf<Song>().apply { addAll(synthSongs) }
    val playlists = mutableStateListOf<Playlist>().apply {
        add(Playlist("FAVORITES").apply { songs.add(synthSongs[0]); songs.add(synthSongs[1]) })
        add(Playlist("CYBER AMBIENT").apply { songs.add(synthSongs[3]) })
    }
    
    var currentSong by mutableStateOf<Song?>(synthSongs[0])
        private set
        
    var isPlaying by mutableStateOf(false)
        private set

    var currentProgress by mutableStateOf(0L)
        private set

    var volume by mutableStateOf(0.7f)
        private set

    var isScanning by mutableStateOf(false)
        private set

    // Equalizer levels (simulated bands for visualizers)
    val eqBands = mutableStateListOf(50, 60, 45, 30, 65, 80, 55, 40) // Sliders 0-100%

    // Current index tracker
    private fun getCurrentSongIndex(): Int {
        val cs = currentSong ?: return -1
        return allSongs.indexOfFirst { it.id == cs.id }
    }

    init {
        // Start progress tracking loop
        startProgressTracker()
    }

    fun play(song: Song) {
        stopCurrentPlayback()
        currentSong = song
        isPlaying = true
        currentProgress = 0L

        if (song.isSynth) {
            synthEngine.setPreset(song.synthPresetIdx)
            synthEngine.setVolume(volume)
            synthEngine.start()
        } else {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(song.uri)
                    setVolume(volume, volume)
                    prepare()
                    start()
                    setOnCompletionListener {
                        skipNext()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fail-safe: if local file playback fails, play synthwave instead
                isPlaying = false
            }
        }
    }

    fun pause() {
        if (!isPlaying) return
        isPlaying = false
        if (currentSong?.isSynth == true) {
            synthEngine.stop()
        } else {
            mediaPlayer?.pause()
        }
    }

    fun resume() {
        val song = currentSong ?: return
        if (isPlaying) return
        isPlaying = true

        if (song.isSynth) {
            synthEngine.setPreset(song.synthPresetIdx)
            synthEngine.setVolume(volume)
            synthEngine.start()
        } else {
            mediaPlayer?.start()
        }
    }

    fun togglePlayback() {
        if (isPlaying) pause() else resume()
    }

    fun skipNext() {
        val nextIdx = getCurrentSongIndex() + 1
        if (nextIdx < allSongs.size) {
            play(allSongs[nextIdx])
        } else if (allSongs.isNotEmpty()) {
            play(allSongs[0]) // loop to start
        }
    }

    fun skipPrevious() {
        val prevIdx = getCurrentSongIndex() - 1
        if (prevIdx >= 0) {
            play(allSongs[prevIdx])
        } else if (allSongs.isNotEmpty()) {
            play(allSongs[allSongs.size - 1]) // loop to end
        }
    }

    fun seekTo(progressMs: Long) {
        val song = currentSong ?: return
        currentProgress = progressMs.coerceIn(0L, song.duration)
        if (!song.isSynth) {
            try {
                mediaPlayer?.seekTo(currentProgress.toInt())
            } catch (_: Exception) {}
        }
    }

    fun setVolumeLevel(vol: Float) {
        this.volume = vol.coerceIn(0f, 1f)
        if (currentSong?.isSynth == true) {
            synthEngine.setVolume(this.volume)
        } else {
            mediaPlayer?.setVolume(this.volume, this.volume)
        }
    }

    fun setBandLevel(bandIndex: Int, value: Int) {
        if (bandIndex in eqBands.indices) {
            eqBands[bandIndex] = value.coerceIn(0, 100)
        }
    }

    fun getVisualizerSpectrum(): FloatArray {
        return if (currentSong?.isSynth == true) {
            synthEngine.getVisualizerData()
        } else if (isPlaying) {
            // Read simulated audio frequency buckets based on equalizer bands and media player progress
            FloatArray(16) { i ->
                val base = eqBands[i % eqBands.size] / 100f
                val fluctuation = (Math.sin(System.currentTimeMillis() * 0.015 + i) * 0.18 + 0.22).toFloat()
                (base * 0.6f + fluctuation * volume).coerceIn(0f, 1f)
            }
        } else {
            // Slow decay
            FloatArray(16) { 0f }
        }
    }

    fun createPlaylist(name: String) {
        if (name.isNotBlank() && playlists.none { it.name.lowercase() == name.lowercase() }) {
            playlists.add(Playlist(name))
        }
    }

    fun addSongToPlaylist(playlistName: String, song: Song) {
        val pl = playlists.find { it.name == playlistName } ?: return
        if (pl.songs.none { it.id == song.id }) {
            pl.songs.add(song)
        }
    }

    fun removeSongFromPlaylist(playlistName: String, song: Song) {
        val pl = playlists.find { it.name == playlistName } ?: return
        pl.songs.removeAll { it.id == song.id }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (isPlaying) {
                    val song = currentSong
                    if (song != null) {
                        if (song.isSynth) {
                            currentProgress = (currentProgress + 200L) % song.duration
                        } else {
                            try {
                                currentProgress = mediaPlayer?.currentPosition?.toLong() ?: currentProgress
                            } catch (_: Exception) {}
                        }
                    }
                }
                delay(200)
            }
        }
    }

    private fun stopCurrentPlayback() {
        synthEngine.stop()
        try {
            mediaPlayer?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    // MediaStore database audio files locator
    fun scanLocalMusic(context: Context) {
        if (isScanning) return
        isScanning = true

        scope.launch(Dispatchers.IO) {
            val fetchedSongs = mutableListOf<Song>()
            
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
            )

            // Select only files marked as music files
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

            try {
                context.contentResolver.query(
                    collection,
                    projection,
                    selection,
                    null,
                    "${MediaStore.Audio.Media.TITLE} ASC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val title = cursor.getString(titleCol) ?: "Unknown Cyber Track"
                        val artist = cursor.getString(artistCol) ?: "<SubNode>"
                        val album = cursor.getString(albumCol) ?: "Cyber Grid"
                        val duration = cursor.getLong(durationCol)
                        val path = cursor.getString(dataCol) ?: ""
                        
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        ).toString()

                        // Make some custom procedural high-tech color profiles for local cover arts as well!
                        val coverSeed = title.hashCode()
                        val colorStart = when (Math.abs(coverSeed) % 5) {
                            0 -> 0xFF00FFCC // Neon Cyan
                            1 -> 0xFFFF007F // Razor Magenta
                            2 -> 0xFF39FF14 // Neon Green
                            3 -> 0xFFFF4500 // Spark Orange
                            else -> 0xFF7B00FF // Cyber Purple
                        }
                        val colorEnd = when (Math.abs(coverSeed) % 3) {
                            0 -> 0xFF0055FF // Electric Blue
                            1 -> 0xFFFFCC00 // Spark Yellow
                            else -> 0xFF2C006B // Dark Indigo
                        }

                        fetchedSongs.add(
                            Song(
                                id = "local_$id",
                                title = title.uppercase(),
                                artist = artist.uppercase(),
                                album = album.uppercase(),
                                duration = if (duration > 0) duration else 180000L,
                                uri = contentUri,
                                isSynth = false,
                                coverColorStart = colorStart,
                                coverColorEnd = colorEnd,
                                path = path
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            withContext(Dispatchers.Main) {
                // Keep the default cyber sint-tracks, and merge with scanned local tracks!
                val cleanList = mutableListOf<Song>()
                cleanList.addAll(synthSongs)
                cleanList.addAll(fetchedSongs)
                
                allSongs.clear()
                allSongs.addAll(cleanList)
                isScanning = false
            }
        }
    }
}
