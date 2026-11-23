package com.musikku.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.widget.Toast

object Player {
    private var mp: MediaPlayer? = null
    var songs: List<Song> = emptyList()
    var index = -1
    val listeners = HashSet<() -> Unit>()

    val current: Song? get() = songs.getOrNull(index)
    val isPlaying: Boolean get() = try { mp?.isPlaying == true } catch (e: Exception) { false }
    val duration: Int get() = try { mp?.duration ?: 0 } catch (e: Exception) { 0 }
    val position: Int get() = try { mp?.currentPosition ?: 0 } catch (e: Exception) { 0 }

    private fun fire() {
        listeners.toList().forEach { it() }
    }

    fun play(ctx: Context, list: List<Song>, i: Int) {
        songs = list
        index = i
        load(ctx.applicationContext)
    }

    private fun load(ctx: Context) {
        val s = current ?: return
        try { mp?.release() } catch (e: Exception) { }
        mp = null
        try {
            val p = MediaPlayer()
            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            p.setWakeMode(ctx, PowerManager.PARTIAL_WAKE_LOCK)
            p.setDataSource(ctx, Uri.parse(s.uri))
            p.setOnCompletionListener { next(ctx) }
            p.setOnErrorListener { _, _, _ -> true }
            p.prepare()
            p.start()
            mp = p
        } catch (e: Exception) {
            Toast.makeText(ctx, "Gagal memutar: " + s.title, Toast.LENGTH_SHORT).show()
        }
        fire()
    }

    fun next(ctx: Context) {
        if (songs.isEmpty()) return
        index = (index + 1) % songs.size
        load(ctx.applicationContext)
    }

    fun prev(ctx: Context) {
        if (songs.isEmpty()) return
        if (position > 3000) {
            seekTo(0)
        } else {
            index = (index - 1 + songs.size) % songs.size
            load(ctx.applicationContext)
        }
    }

    fun toggle(ctx: Context) {
        val p = mp
        if (p == null) {
            if (current != null) load(ctx.applicationContext)
            return
        }
        try { if (p.isPlaying) p.pause() else p.start() } catch (e: Exception) { }
        fire()
    }

    fun seekTo(ms: Int) {
        try { mp?.seekTo(ms) } catch (e: Exception) { }
    }

    fun stop() {
        try { mp?.release() } catch (e: Exception) { }
        mp = null
        fire()
    }
}
