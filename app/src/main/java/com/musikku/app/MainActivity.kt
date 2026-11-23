package com.musikku.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private class Row(
        val title: String,
        val sub: String,
        val onClick: () -> Unit,
        val onLong: (() -> Unit)? = null
    )

    private var allSongs: List<Song> = emptyList()
    private var tab = 0                 // 0 = semua lagu, 1 = playlist
    private var openPlaylist: String? = null
    private val rows = ArrayList<Row>()

    private lateinit var list: ListView
    private lateinit var adapter: BaseAdapter
    private lateinit var header: TextView
    private lateinit var npTitle: TextView
    private lateinit var npTime: TextView
    private lateinit var btnPlay: Button
    private lateinit var seek: SeekBar

    private val handler = Handler(Looper.getMainLooper())
    private var dragging = false

    private val playerListener: () -> Unit = { updateNow() }

    private val ticker = object : Runnable {
        override fun run() {
            if (!dragging) {
                val d = Player.duration
                seek.max = if (d > 0) d else 1
                seek.progress = Player.position
                npTime.text = fmt(Player.position) + " / " + fmt(d)
            }
            handler.postDelayed(this, 500)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun fmt(ms: Int): String {
        val s = ms / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }

    // ---------------------------------------------------------------- lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()

        val perms = if (Build.VERSION.SDK_INT >= 33)
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        else
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        val missing = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) loadSongs() else requestPermissions(missing.toTypedArray(), 1)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        loadSongs()
    }

    override fun onResume() {
        super.onResume()
        Player.listeners.add(playerListener)
        updateNow()
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        Player.listeners.remove(playerListener)
        handler.removeCallbacks(ticker)
    }

    override fun onBackPressed() {
        if (tab == 1 && openPlaylist != null) {
            openPlaylist = null
            refresh()
        } else {
            super.onBackPressed()
        }
    }

    // ---------------------------------------------------------------- UI

    private fun buildUi() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        fun tabBtn(label: String, action: () -> Unit) {
            val b = Button(this)
            b.text = label
            b.setOnClickListener { action() }
            bar.addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        tabBtn("Lagu") { tab = 0; refresh() }
        tabBtn("Playlist") { tab = 1; openPlaylist = null; refresh() }
        tabBtn("+ File") { pickFiles() }
        root.addView(bar)

        header = TextView(this)
        header.textSize = 13f
        header.setTextColor(Color.DKGRAY)
        header.setPadding(dp(16), dp(6), dp(16), dp(6))
        root.addView(header)

        list = ListView(this)
        adapter = object : BaseAdapter() {
            override fun getCount() = rows.size
            override fun getItem(position: Int): Any = rows[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val r = rows[position]
                val box = (convertView as? LinearLayout) ?: LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(16), dp(10), dp(16), dp(10))
                    val t = TextView(this@MainActivity)
                    t.textSize = 16f
                    t.setTextColor(Color.BLACK)
                    t.maxLines = 1
                    t.ellipsize = TextUtils.TruncateAt.END
                    addView(t)
                    val s = TextView(this@MainActivity)
                    s.textSize = 12f
                    s.setTextColor(Color.GRAY)
                    s.maxLines = 1
                    addView(s)
                }
                (box.getChildAt(0) as TextView).text = r.title
                val sub = box.getChildAt(1) as TextView
                sub.text = r.sub
                sub.visibility = if (r.sub.isEmpty()) View.GONE else View.VISIBLE
                return box
            }
        }
        list.adapter = adapter
        list.setOnItemClickListener { _, _, pos, _ -> rows.getOrNull(pos)?.onClick?.invoke() }
        list.setOnItemLongClickListener { _, _, pos, _ ->
            val f = rows.getOrNull(pos)?.onLong
            if (f != null) { f(); true } else false
        }
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // panel sedang diputar
        val np = LinearLayout(this)
        np.orientation = LinearLayout.VERTICAL
        np.setBackgroundColor(Color.parseColor("#EEEEEE"))
        np.setPadding(dp(16), dp(8), dp(16), dp(8))

        npTitle = TextView(this)
        npTitle.textSize = 15f
        npTitle.setTextColor(Color.BLACK)
        npTitle.maxLines = 1
        npTitle.ellipsize = TextUtils.TruncateAt.END
        np.addView(npTitle)

        seek = SeekBar(this)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) npTime.text = fmt(p) + " / " + fmt(Player.duration)
            }
            override fun onStartTrackingTouch(s: SeekBar?) { dragging = true }
            override fun onStopTrackingTouch(s: SeekBar?) {
                dragging = false
                Player.seekTo(s?.progress ?: 0)
            }
        })
        np.addView(seek)

        npTime = TextView(this)
        npTime.textSize = 12f
        npTime.gravity = Gravity.END
        npTime.text = "0:00 / 0:00"
        np.addView(npTime)

        val ctl = LinearLayout(this)
        ctl.orientation = LinearLayout.HORIZONTAL
        ctl.gravity = Gravity.CENTER
        fun ctlBtn(label: String, action: () -> Unit): Button {
            val b = Button(this)
            b.text = label
            b.textSize = 20f
            b.setOnClickListener { action() }
            ctl.addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            return b
        }
        ctlBtn("⏮") { Player.prev(this); startPlaybackService() }
        btnPlay = ctlBtn("▶") { Player.toggle(this); startPlaybackService() }
        ctlBtn("⏭") { Player.next(this); startPlaybackService() }
        np.addView(ctl)

        root.addView(np)
        setContentView(root)
    }

    private fun updateNow() {
        val s = Player.current
        npTitle.text = if (s == null) "Belum ada lagu diputar" else s.title + " — " + s.artist
        btnPlay.text = if (Player.isPlaying) "⏸" else "▶"
    }

    private fun startPlaybackService() {
        if (Player.current != null) {
            startForegroundService(Intent(this, PlaybackService::class.java))
        }
    }

    private fun playList(songs: List<Song>, i: Int) {
        Player.play(this, songs, i)
        startPlaybackService()
    }

    // ---------------------------------------------------------------- data

    private fun loadSongs() {
        Thread {
            val l = Library.load(this)
            runOnUiThread {
                allSongs = l
                refresh()
            }
        }.start()
    }

    private fun songsOf(name: String): List<Song> {
        val map = allSongs.associateBy { it.uri }
        return Store.get(this, name).mapNotNull { map[it] }
    }

    private fun refresh() {
        rows.clear()
        val pl = openPlaylist
        if (tab == 0) {
            header.text = "Semua lagu (${allSongs.size})  •  tekan lama = tambah ke playlist"
            allSongs.forEachIndexed { i, s ->
                rows.add(Row(s.title, s.artist, { playList(allSongs, i) }, { addToPlaylistDialog(s) }))
            }
            if (allSongs.isEmpty()) {
                rows.add(Row("Belum ada lagu", "Izinkan akses audio, atau tekan \"+ File\" untuk memilih manual", { }))
            }
        } else if (pl == null) {
            header.text = "Playlist  •  tekan lama = hapus playlist"
            rows.add(Row("+ Buat playlist baru", "", { newPlaylistDialog(null) }))
            for (n in Store.names(this)) {
                val count = Store.get(this, n).size
                rows.add(Row(n, "$count lagu", { openPlaylist = n; refresh() }, { deletePlaylistDialog(n) }))
            }
        } else {
            val ps = songsOf(pl)
            header.text = "Playlist: $pl (${ps.size} lagu)  •  tekan lama = hapus lagu"
            rows.add(Row("+ Tambah / pilih lagu", "", { pickSongsDialog(pl) }))
            if (ps.isNotEmpty()) rows.add(Row("▶ Putar semua", "", { playList(ps, 0) }))
            ps.forEachIndexed { i, s ->
                rows.add(Row(s.title, s.artist, { playList(ps, i) }, { confirmRemove(pl, s) }))
            }
        }
        adapter.notifyDataSetChanged()
    }

    // ---------------------------------------------------------------- dialogs

    private fun addToPlaylistDialog(song: Song) {
        val names = Store.names(this)
        val items = (names + "+ Playlist baru").toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Tambah ke playlist")
            .setItems(items) { _, which ->
                if (which < names.size) {
                    Store.add(this, names[which], listOf(song.uri))
                    Toast.makeText(this, "Ditambahkan ke " + names[which], Toast.LENGTH_SHORT).show()
                } else {
                    newPlaylistDialog(song)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun newPlaylistDialog(first: Song?) {
        val input = EditText(this)
        input.hint = "Nama playlist"
        input.setSingleLine()
        AlertDialog.Builder(this)
            .setTitle("Playlist baru")
            .setView(input)
            .setPositiveButton("Buat") { _, _ ->
                val n = input.text.toString().trim()
                if (n.isNotEmpty()) {
                    if (n !in Store.names(this)) Store.set(this, n, emptyList())
                    if (first != null) Store.add(this, n, listOf(first.uri))
                    refresh()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deletePlaylistDialog(name: String) {
        AlertDialog.Builder(this)
            .setTitle("Hapus playlist \"$name\"?")
            .setPositiveButton("Hapus") { _, _ -> Store.delete(this, name); refresh() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun confirmRemove(name: String, s: Song) {
        AlertDialog.Builder(this)
            .setTitle("Hapus \"${s.title}\" dari playlist?")
            .setPositiveButton("Hapus") { _, _ -> Store.remove(this, name, s.uri); refresh() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun pickSongsDialog(name: String) {
        if (allSongs.isEmpty()) {
            Toast.makeText(this, "Belum ada lagu", Toast.LENGTH_SHORT).show()
            return
        }
        val existing = Store.get(this, name)
        val labels = allSongs.map { it.title + " — " + it.artist }.toTypedArray()
        val checked = BooleanArray(allSongs.size) { allSongs[it].uri in existing }
        AlertDialog.Builder(this)
            .setTitle("Pilih lagu")
            .setMultiChoiceItems(labels, checked) { _, i, c -> checked[i] = c }
            .setPositiveButton("Simpan") { _, _ ->
                val sel = allSongs.filterIndexed { i, _ -> checked[i] }.map { it.uri }
                val merged = existing.filter { it in sel } + sel.filter { it !in existing }
                Store.set(this, name, merged)
                refresh()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ---------------------------------------------------------------- pilih file manual

    private fun pickFiles() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "audio/*"
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        startActivityForResult(i, 42)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 42 || resultCode != RESULT_OK || data == null) return
        val uris = ArrayList<Uri>()
        val cd = data.clipData
        if (cd != null) {
            for (k in 0 until cd.itemCount) uris.add(cd.getItemAt(k).uri)
        } else {
            data.data?.let { uris.add(it) }
        }
        val strs = ArrayList<String>()
        for (u in uris) {
            try {
                contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { }
            strs.add(u.toString())
        }
        Store.addExtras(this, strs)
        loadSongs()
    }
}
