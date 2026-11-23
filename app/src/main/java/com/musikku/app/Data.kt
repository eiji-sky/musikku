package com.musikku.app

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject

data class Song(val uri: String, val title: String, val artist: String)

object Library {
    /** Semua lagu di memori internal + kartu SD (lewat MediaStore) + file yang dipilih manual. */
    fun load(ctx: Context): List<Song> {
        val out = ArrayList<Song>()
        val seen = HashSet<String>()
        try {
            val proj = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST
            )
            ctx.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0", null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, c.getLong(0)
                    ).toString()
                    var artist = c.getString(2) ?: ""
                    if (artist.isEmpty() || artist == "<unknown>") artist = "Tidak diketahui"
                    out.add(Song(uri, c.getString(1) ?: "Tanpa judul", artist))
                    seen.add(uri)
                }
            }
        } catch (e: Exception) {
        }
        for (u in Store.extras(ctx)) {
            if (seen.add(u)) out.add(Song(u, displayName(ctx, Uri.parse(u)), "File pilihan"))
        }
        return out
    }

    private fun displayName(ctx: Context, uri: Uri): String {
        try {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) return it.getString(0).substringBeforeLast('.')
            }
        } catch (e: Exception) {
        }
        return uri.lastPathSegment ?: "Lagu"
    }
}

/** Playlist disimpan di SharedPreferences sebagai JSON: { nama: [uri, uri, ...] } */
object Store {
    private fun sp(ctx: Context) = ctx.getSharedPreferences("musikku", Context.MODE_PRIVATE)

    private fun pl(ctx: Context): JSONObject =
        try { JSONObject(sp(ctx).getString("playlists", "{}")!!) } catch (e: Exception) { JSONObject() }

    private fun save(ctx: Context, o: JSONObject) {
        sp(ctx).edit().putString("playlists", o.toString()).apply()
    }

    fun names(ctx: Context): List<String> {
        val keys = pl(ctx).keys()
        val l = ArrayList<String>()
        while (keys.hasNext()) l.add(keys.next())
        return l.sortedBy { it.lowercase() }
    }

    fun get(ctx: Context, name: String): List<String> {
        val a = pl(ctx).optJSONArray(name) ?: return emptyList()
        return (0 until a.length()).map { a.getString(it) }
    }

    fun set(ctx: Context, name: String, uris: List<String>) {
        val o = pl(ctx)
        val a = JSONArray()
        uris.forEach { a.put(it) }
        o.put(name, a)
        save(ctx, o)
    }

    fun add(ctx: Context, name: String, uris: List<String>) {
        val cur = get(ctx, name).toMutableList()
        uris.forEach { if (it !in cur) cur.add(it) }
        set(ctx, name, cur)
    }

    fun remove(ctx: Context, name: String, uri: String) {
        set(ctx, name, get(ctx, name).filter { it != uri })
    }

    fun delete(ctx: Context, name: String) {
        val o = pl(ctx)
        o.remove(name)
        save(ctx, o)
    }

    fun extras(ctx: Context): List<String> {
        val raw = sp(ctx).getString("extras", "[]") ?: "[]"
        return try {
            val a = JSONArray(raw)
            (0 until a.length()).map { a.getString(it) }
        } catch (e: Exception) { emptyList() }
    }

    fun addExtras(ctx: Context, uris: List<String>) {
        val cur = extras(ctx).toMutableList()
        uris.forEach { if (it !in cur) cur.add(it) }
        val a = JSONArray()
        cur.forEach { a.put(it) }
        sp(ctx).edit().putString("extras", a.toString()).apply()
    }
}
