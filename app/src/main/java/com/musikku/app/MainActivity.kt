package com.musikku.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
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
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private class Row(
        val title: String,
        val sub: String,
        val seed: String,
        val content: Int,
        val onClick: () -> Unit,
        val onLong: (() -> Unit)? = null
    )

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val INK = Color.parseColor("#1F1F1F")
    private val GRAY = Color.parseColor("#8E8E93")
    private val CREAM = Color.parseColor("#FBF8F0")

    private var allSongs: List<Song> = emptyList()
    private var screen = 0                 // 0 home, 1 lagu, 2 playlist
    private var openPlaylist: String? = null
    private val rows = ArrayList<Row>()

    private lateinit var root: FrameLayout
    private lateinit var homeScroll: ScrollView
    private lateinit var homeBox: LinearLayout
    private lateinit var listScreen: LinearLayout
    private lateinit var listTitle: TextView
    private lateinit var listBack: IconView
    private lateinit var listView: ListView
    private lateinit var adapter: BaseAdapter

    private val tabViews = ArrayList<IconView>()
    private val tabTypes = intArrayOf(Icons.HOME, Icons.MUSIC, Icons.LIST)

    private lateinit var mini: LinearLayout
    private lateinit var miniArt: ArtView
    private lateinit var miniTitle: TextView
    private lateinit var miniSub: TextView
    private lateinit var miniPlay: IconView

    private lateinit var np: FrameLayout
    private lateinit var turntable: TurntableView
    private lateinit var npArtist: TextView
    private lateinit var npSong: TextView
    private lateinit var npCurrent: TextView
    private lateinit var npRemain: TextView
    private lateinit var npSeek: ThinSeek
    private lateinit var npPlay: IconView
    private lateinit var npShuffle: IconView
    private lateinit var npRepeat: IconView
    private lateinit var npNext: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val playerListener: () -> Unit = { updateNow() }

    private val ticker = object : Runnable {
        override fun run() {
            if (Player.current != null) {
                val d = Player.duration
                val pos = Player.position
                val f = if (d > 0) pos.toFloat() / d else 0f
                npSeek.setProg(f)
                if (!npSeek.dragging) {
                    npCurrent.text = fmt(pos)
                    npRemain.text = "-" + fmt(maxOf(d - pos, 0))
                }
                turntable.setProgress(f)
            }
            handler.postDelayed(this, 400)
        }
    }

    // ------------------------------------------------------------ helpers

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun fmt(ms: Int): String {
        val s = ms / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }

    private fun tv(text: String, sp: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = text
        t.textSize = sp
        t.setTextColor(color)
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        t.maxLines = 1
        t.ellipsize = TextUtils.TruncateAt.END
        return t
    }

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(radiusDp).toFloat()
        return g
    }

    private fun oval(color: Int): GradientDrawable {
        val g = GradientDrawable()
        g.shape = GradientDrawable.OVAL
        g.setColor(color)
        return g
    }

    private fun icon(type: Int, tint: Int, scale: Float = 0.6f) = IconView(this, type, tint, scale)

    private fun art(seed: String, content: Int, label: String, radiusDp: Float): ArtView {
        val a = ArtView(this)
        a.radiusDp = radiusDp
        a.seed = seed
        a.label = label
        a.content = content
        return a
    }

    // ------------------------------------------------------------ lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        buildUi()
        showScreen(0)

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
        turntable.setPlaying(false)
    }

    override fun onBackPressed() {
        if (np.visibility == View.VISIBLE) {
            hideNowPlaying()
        } else if (screen == 2 && openPlaylist != null) {
            openPlaylist = null
            buildList()
        } else if (screen != 0) {
            showScreen(0)
        } else {
            super.onBackPressed()
        }
    }

    // ------------------------------------------------------------ build UI

    private fun buildUi() {
        root = FrameLayout(this)
        root.setBackgroundColor(Color.WHITE)

        // ---- HOME
        homeScroll = ScrollView(this)
        homeScroll.isVerticalScrollBarEnabled = false
        homeBox = LinearLayout(this)
        homeBox.orientation = LinearLayout.VERTICAL
        homeBox.setPadding(0, 0, 0, dp(170))
        homeScroll.addView(homeBox, ViewGroup.LayoutParams(MATCH, WRAP))
        root.addView(homeScroll, FrameLayout.LayoutParams(MATCH, MATCH))

        // ---- LIST SCREEN (lagu / playlist)
        listScreen = LinearLayout(this)
        listScreen.orientation = LinearLayout.VERTICAL
        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        head.setPadding(dp(20), dp(16), dp(20), dp(10))
        listBack = icon(Icons.BACK, INK, 0.6f)
        listBack.background = oval(Color.parseColor("#F2F2F2"))
        listBack.visibility = View.GONE
        listBack.setOnClickListener { openPlaylist = null; buildList() }
        val bl = LinearLayout.LayoutParams(dp(40), dp(40))
        bl.marginEnd = dp(12)
        head.addView(listBack, bl)
        listTitle = tv("", 34f, INK, true)
        head.addView(listTitle, LinearLayout.LayoutParams(0, WRAP, 1f))
        listScreen.addView(head, LinearLayout.LayoutParams(MATCH, WRAP))

        listView = ListView(this)
        listView.divider = null
        listView.dividerHeight = 0
        listView.setSelector(ColorDrawable(Color.TRANSPARENT))
        listView.isVerticalScrollBarEnabled = false
        listView.clipToPadding = false
        listView.setPadding(0, 0, 0, dp(170))
        adapter = object : BaseAdapter() {
            override fun getCount() = rows.size
            override fun getItem(position: Int): Any = rows[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val r = rows[position]
                val v: LinearLayout = (convertView as? LinearLayout) ?: LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(20), dp(8), dp(20), dp(8))
                    addView(art("", ArtView.LETTER, "", 14f), LinearLayout.LayoutParams(dp(52), dp(52)))
                    val col = LinearLayout(this@MainActivity)
                    col.orientation = LinearLayout.VERTICAL
                    col.setPadding(dp(14), 0, 0, 0)
                    col.addView(tv("", 16f, INK, true))
                    col.addView(tv("", 13f, GRAY))
                    addView(col, LinearLayout.LayoutParams(0, WRAP, 1f))
                }
                val a = v.getChildAt(0) as ArtView
                a.seed = r.seed
                a.label = r.title.take(1).uppercase()
                a.content = r.content
                val col = v.getChildAt(1) as LinearLayout
                (col.getChildAt(0) as TextView).text = r.title
                val sub = col.getChildAt(1) as TextView
                sub.text = r.sub
                sub.visibility = if (r.sub.isEmpty()) View.GONE else View.VISIBLE
                return v
            }
        }
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, pos, _ -> rows.getOrNull(pos)?.onClick?.invoke() }
        listView.setOnItemLongClickListener { _, _, pos, _ ->
            val f = rows.getOrNull(pos)?.onLong
            if (f != null) { f(); true } else false
        }
        listScreen.addView(listView, LinearLayout.LayoutParams(MATCH, 0, 1f))
        root.addView(listScreen, FrameLayout.LayoutParams(MATCH, MATCH))

        // ---- BOTTOM: mini player + tab bar mengambang
        val bottom = LinearLayout(this)
        bottom.orientation = LinearLayout.VERTICAL
        bottom.gravity = Gravity.CENTER_HORIZONTAL
        bottom.clipChildren = false
        bottom.setPadding(0, 0, 0, dp(14))

        mini = LinearLayout(this)
        mini.orientation = LinearLayout.HORIZONTAL
        mini.gravity = Gravity.CENTER_VERTICAL
        mini.setPadding(dp(10), dp(10), dp(12), dp(10))
        mini.background = rounded(Color.parseColor("#151515"), 30)
        mini.elevation = dp(6).toFloat()
        mini.visibility = View.GONE
        mini.setOnClickListener { showNowPlaying() }
        miniArt = art("", ArtView.LETTER, "", 22f)
        mini.addView(miniArt, LinearLayout.LayoutParams(dp(44), dp(44)))
        val mcol = LinearLayout(this)
        mcol.orientation = LinearLayout.VERTICAL
        mcol.setPadding(dp(12), 0, dp(8), 0)
        miniTitle = tv("", 15f, Color.WHITE, true)
        miniSub = tv("", 12f, Color.parseColor("#A0A0A0"))
        mcol.addView(miniTitle)
        mcol.addView(miniSub)
        mini.addView(mcol, LinearLayout.LayoutParams(0, WRAP, 1f))
        miniPlay = icon(Icons.PLAY, INK, 0.5f)
        miniPlay.background = oval(CREAM)
        miniPlay.setOnClickListener { Player.toggle(this); startPlaybackService() }
        mini.addView(miniPlay, LinearLayout.LayoutParams(dp(44), dp(44)))
        val mlp = LinearLayout.LayoutParams(MATCH, WRAP)
        mlp.setMargins(dp(16), 0, dp(16), dp(10))
        bottom.addView(mini, mlp)

        val tabBar = LinearLayout(this)
        tabBar.orientation = LinearLayout.HORIZONTAL
        tabBar.setPadding(dp(8), dp(6), dp(8), dp(6))
        val tg = rounded(Color.WHITE, 32)
        tg.setStroke(dp(1), Color.parseColor("#E8E8E8"))
        tabBar.background = tg
        tabBar.elevation = dp(8).toFloat()
        for (k in 0..2) {
            val iv = icon(tabTypes[k], GRAY, 0.5f)
            iv.setOnClickListener {
                if (k == 2) openPlaylist = null
                showScreen(k)
            }
            tabViews.add(iv)
            val lp = LinearLayout.LayoutParams(dp(60), dp(48))
            lp.setMargins(dp(2), 0, dp(2), 0)
            tabBar.addView(iv, lp)
        }
        bottom.addView(tabBar, LinearLayout.LayoutParams(WRAP, WRAP))
        root.addView(bottom, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM))

        buildNowPlaying()
        setContentView(root)
    }

    private fun buildNowPlaying() {
        np = FrameLayout(this)
        np.setBackgroundColor(CREAM)
        np.isClickable = true
        np.visibility = View.GONE

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(20), dp(12), dp(20), 0)

        val bar = FrameLayout(this)
        val back = icon(Icons.BACK, Color.WHITE, 0.6f)
        back.background = oval(Color.parseColor("#9E9E9E"))
        back.setOnClickListener { hideNowPlaying() }
        bar.addView(back, FrameLayout.LayoutParams(dp(40), dp(40), Gravity.START or Gravity.CENTER_VERTICAL))
        bar.addView(tv("Now Playing", 17f, INK), FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
        val more = icon(Icons.DOTS, Color.WHITE, 0.6f)
        more.background = oval(Color.parseColor("#9E9E9E"))
        more.setOnClickListener { moreMenu() }
        bar.addView(more, FrameLayout.LayoutParams(dp(40), dp(40), Gravity.END or Gravity.CENTER_VERTICAL))
        col.addView(bar, LinearLayout.LayoutParams(MATCH, dp(48)))

        val dm = resources.displayMetrics
        val side = minOf(dm.widthPixels - dp(40), (dm.heightPixels * 0.42f).toInt())
        turntable = TurntableView(this)
        col.addView(turntable, LinearLayout.LayoutParams(MATCH, side))

        npArtist = tv("", 15f, Color.parseColor("#7A7A7A"))
        npArtist.gravity = Gravity.CENTER
        npArtist.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        val alp = LinearLayout.LayoutParams(MATCH, WRAP)
        alp.topMargin = dp(6)
        col.addView(npArtist, alp)

        npSong = tv("", 24f, INK, true)
        npSong.gravity = Gravity.CENTER
        npSong.maxLines = 2
        col.addView(npSong, LinearLayout.LayoutParams(MATCH, WRAP))

        npSeek = ThinSeek(this)
        npSeek.onDrag = { f ->
            val d = Player.duration
            val pos = (f * d).toInt()
            npCurrent.text = fmt(pos)
            npRemain.text = "-" + fmt(maxOf(d - pos, 0))
        }
        npSeek.onSeek = { f -> Player.seekTo((f * Player.duration).toInt()) }
        val slp = LinearLayout.LayoutParams(MATCH, dp(28))
        slp.topMargin = dp(16)
        col.addView(npSeek, slp)

        val times = LinearLayout(this)
        times.orientation = LinearLayout.HORIZONTAL
        npCurrent = tv("0:00", 12f, GRAY)
        npRemain = tv("-0:00", 12f, GRAY)
        npRemain.gravity = Gravity.END
        times.addView(npCurrent, LinearLayout.LayoutParams(0, WRAP, 1f))
        times.addView(npRemain, LinearLayout.LayoutParams(0, WRAP, 1f))
        col.addView(times, LinearLayout.LayoutParams(MATCH, WRAP))

        val ctl = LinearLayout(this)
        ctl.orientation = LinearLayout.HORIZONTAL
        ctl.gravity = Gravity.CENTER_VERTICAL
        fun cb(type: Int, scale: Float, action: () -> Unit): IconView {
            val v = icon(type, INK, scale)
            v.setOnClickListener { action() }
            ctl.addView(v, LinearLayout.LayoutParams(0, dp(64), 1f))
            return v
        }
        npShuffle = cb(Icons.SHUFFLE, 0.5f) { Player.shuffle = !Player.shuffle; updateNow() }
        cb(Icons.PREV, 0.55f) { Player.prev(this); startPlaybackService() }
        npPlay = cb(Icons.PAUSE, 0.75f) { Player.toggle(this); startPlaybackService() }
        cb(Icons.NEXT, 0.55f) { Player.next(this); startPlaybackService() }
        npRepeat = cb(Icons.REPEAT, 0.5f) { Player.repeatOne = !Player.repeatOne; updateNow() }
        val clp = LinearLayout.LayoutParams(MATCH, WRAP)
        clp.topMargin = dp(6)
        col.addView(ctl, clp)

        np.addView(col, FrameLayout.LayoutParams(MATCH, MATCH))

        // sheet gelap di bawah
        val sheet = LinearLayout(this)
        sheet.orientation = LinearLayout.HORIZONTAL
        sheet.gravity = Gravity.CENTER_VERTICAL
        sheet.setPadding(dp(24), dp(8), dp(20), dp(8))
        val g = GradientDrawable()
        g.setColor(Color.parseColor("#141414"))
        val r = dp(30).toFloat()
        g.cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        sheet.background = g
        val sc = LinearLayout(this)
        sc.orientation = LinearLayout.VERTICAL
        sc.addView(tv("Berikutnya", 11f, Color.parseColor("#9A9A9A")))
        npNext = tv("", 14f, Color.WHITE, true)
        sc.addView(npNext)
        sheet.addView(sc, LinearLayout.LayoutParams(0, WRAP, 1f))
        val btn = tv("PLAYLIST", 12f, INK, true)
        btn.setPadding(dp(18), dp(10), dp(18), dp(10))
        btn.background = rounded(CREAM, 20)
        btn.setOnClickListener { Player.current?.let { addToPlaylistDialog(it) } }
        sheet.addView(btn, LinearLayout.LayoutParams(WRAP, WRAP))
        np.addView(sheet, FrameLayout.LayoutParams(MATCH, dp(84), Gravity.BOTTOM))

        root.addView(np, FrameLayout.LayoutParams(MATCH, MATCH))
    }

    // ------------------------------------------------------------ screens

    private fun showScreen(i: Int) {
        screen = i
        homeScroll.visibility = if (i == 0) View.VISIBLE else View.GONE
        listScreen.visibility = if (i == 0) View.GONE else View.VISIBLE
        for (k in tabViews.indices) {
            val on = k == i
            tabViews[k].update(tabTypes[k], if (on) INK else GRAY)
            tabViews[k].background = if (on) rounded(Color.parseColor("#EFEFEF"), 24) else null
        }
        if (i == 0) buildHome() else buildList()
    }

    private fun refreshAll() {
        if (screen == 0) buildHome() else buildList()
    }

    private fun section(title: String, count: Int, onMore: (() -> Unit)?) {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.setPadding(dp(20), dp(26), dp(20), dp(10))
        r.addView(tv(title, 20f, INK, true), LinearLayout.LayoutParams(0, WRAP, 1f))
        r.addView(tv("$count", 16f, GRAY))
        r.addView(icon(Icons.CHEVRON, GRAY, 0.9f), LinearLayout.LayoutParams(dp(20), dp(20)))
        if (onMore != null) r.setOnClickListener { onMore() }
        homeBox.addView(r)
    }

    private fun songStrip(list: List<Song>) {
        val sw = resources.displayMetrics.widthPixels
        val cw = (sw * 0.29f).toInt()
        val chh = (cw * 1.38f).toInt()
        val hs = HorizontalScrollView(this)
        hs.isHorizontalScrollBarEnabled = false
        hs.clipToPadding = false
        hs.setPadding(dp(20), 0, dp(20), 0)
        val strip = LinearLayout(this)
        strip.orientation = LinearLayout.HORIZONTAL
        list.forEachIndexed { i, s ->
            val c = LinearLayout(this)
            c.orientation = LinearLayout.VERTICAL
            c.addView(art(s.title, ArtView.LETTER, s.title.take(1).uppercase(), 18f), LinearLayout.LayoutParams(cw, chh))
            val t = tv(s.title, 14f, INK, true)
            t.setPadding(0, dp(8), 0, 0)
            c.addView(t, LinearLayout.LayoutParams(cw, WRAP))
            c.addView(tv(s.artist, 13f, GRAY), LinearLayout.LayoutParams(cw, WRAP))
            c.setOnClickListener { playList(list, i) }
            c.setOnLongClickListener { addToPlaylistDialog(s); true }
            val lp = LinearLayout.LayoutParams(WRAP, WRAP)
            lp.marginEnd = dp(12)
            strip.addView(c, lp)
        }
        hs.addView(strip)
        homeBox.addView(hs)
    }

    private fun newPlaylistCard(): View {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.gravity = Gravity.CENTER
        c.background = rounded(Color.parseColor("#F2F2F2"), 28)
        c.addView(icon(Icons.PLUS, INK, 0.9f), LinearLayout.LayoutParams(dp(56), dp(56)))
        val t = tv("Playlist baru", 15f, INK, true)
        t.setPadding(0, dp(6), 0, 0)
        c.addView(t, LinearLayout.LayoutParams(WRAP, WRAP))
        c.setOnClickListener { newPlaylistDialog(null) }
        return c
    }

    private fun playlistCard(name: String): View {
        val n = Store.get(this, name).size
        val f = FrameLayout(this)
        f.addView(art(name, ArtView.NONE, "", 28f), FrameLayout.LayoutParams(MATCH, MATCH))
        val badge = tv("Playlist", 12f, GRAY)
        badge.setPadding(dp(10), dp(4), dp(10), dp(4))
        badge.background = rounded(Color.argb(225, 255, 255, 255), 12)
        val bp = FrameLayout.LayoutParams(WRAP, WRAP)
        bp.setMargins(dp(14), dp(14), 0, 0)
        f.addView(badge, bp)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.addView(art(name + "·", ArtView.NOTE, "", 10f), LinearLayout.LayoutParams(dp(44), dp(44)))
        val tc = LinearLayout(this)
        tc.orientation = LinearLayout.VERTICAL
        tc.setPadding(dp(10), 0, 0, 0)
        tc.addView(tv(name, 16f, Color.WHITE, true))
        tc.addView(tv("$n lagu", 13f, Color.argb(190, 255, 255, 255)))
        row.addView(tc, LinearLayout.LayoutParams(0, WRAP, 1f))
        val rp = FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM)
        rp.setMargins(dp(14), 0, dp(14), dp(16))
        f.addView(row, rp)

        f.setOnClickListener { openPlaylist = name; showScreen(2) }
        f.setOnLongClickListener { deletePlaylistDialog(name); true }
        return f
    }

    private fun buildHome() {
        homeBox.removeAllViews()

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.setPadding(dp(20), dp(16), dp(20), dp(14))
        top.addView(tv("Home", 34f, INK, true), LinearLayout.LayoutParams(0, WRAP, 1f))
        val add = icon(Icons.PLUS, INK, 0.5f)
        add.background = oval(Color.parseColor("#F2F2F2"))
        add.setOnClickListener { pickFiles() }
        top.addView(add, LinearLayout.LayoutParams(dp(44), dp(44)))
        homeBox.addView(top)

        // kartu playlist besar
        val sw = resources.displayMetrics.widthPixels
        val cw = (sw * 0.52f).toInt()
        val ch = (cw * 1.45f).toInt()
        val hs = HorizontalScrollView(this)
        hs.isHorizontalScrollBarEnabled = false
        hs.clipToPadding = false
        hs.setPadding(dp(20), 0, dp(20), 0)
        val strip = LinearLayout(this)
        strip.orientation = LinearLayout.HORIZONTAL
        val first = LinearLayout.LayoutParams(cw, ch)
        first.marginEnd = dp(12)
        strip.addView(newPlaylistCard(), first)
        for (n in Store.names(this)) {
            val lp = LinearLayout.LayoutParams(cw, ch)
            lp.marginEnd = dp(12)
            strip.addView(playlistCard(n), lp)
        }
        hs.addView(strip)
        homeBox.addView(hs)

        val map = allSongs.associateBy { it.uri }
        val recent = Store.recents(this).mapNotNull { map[it] }
        if (recent.isNotEmpty()) {
            section("Terakhir diputar", recent.size, null)
            songStrip(recent)
        }

        section("Semua lagu", allSongs.size) { showScreen(1) }
        if (allSongs.isEmpty()) {
            val e = tv("Belum ada lagu. Izinkan akses audio, atau tekan tombol + di kanan atas untuk memilih file.", 14f, GRAY)
            e.maxLines = 4
            e.setPadding(dp(20), 0, dp(20), 0)
            homeBox.addView(e)
        } else {
            songStrip(allSongs.take(20))
        }
    }

    private fun buildList() {
        rows.clear()
        val pl = openPlaylist
        if (screen == 1) {
            listTitle.text = "Lagu"
            listBack.visibility = View.GONE
            allSongs.forEachIndexed { i, s ->
                rows.add(Row(s.title, s.artist, s.title, ArtView.LETTER, { playList(allSongs, i) }, { addToPlaylistDialog(s) }))
            }
            if (allSongs.isEmpty()) {
                rows.add(Row("Belum ada lagu", "Izinkan akses audio, atau pilih file manual", "kosong", ArtView.PLUS, { pickFiles() }))
            }
        } else if (pl == null) {
            listTitle.text = "Playlist"
            listBack.visibility = View.GONE
            rows.add(Row("Playlist baru", "Buat playlist kosong", "baru", ArtView.PLUS, { newPlaylistDialog(null) }))
            for (n in Store.names(this)) {
                val c = Store.get(this, n).size
                rows.add(Row(n, "$c lagu", n, ArtView.NOTE, { openPlaylist = n; buildList() }, { deletePlaylistDialog(n) }))
            }
        } else {
            listTitle.text = pl
            listBack.visibility = View.VISIBLE
            val ps = songsOf(pl)
            rows.add(Row("Tambah / pilih lagu", "Centang lagu untuk playlist ini", "tambah", ArtView.PLUS, { pickSongsDialog(pl) }))
            if (ps.isNotEmpty()) {
                rows.add(Row("Putar semua", "${ps.size} lagu", "putar", ArtView.PLAY, { playList(ps, 0) }))
            }
            ps.forEachIndexed { i, s ->
                rows.add(Row(s.title, s.artist, s.title, ArtView.LETTER, { playList(ps, i) }, { confirmRemove(pl, s) }))
            }
        }
        adapter.notifyDataSetChanged()
    }

    // ------------------------------------------------------------ now playing

    private fun updateNow() {
        val s = Player.current
        val playing = Player.isPlaying
        if (s == null) {
            mini.visibility = View.GONE
        } else {
            mini.visibility = View.VISIBLE
            miniArt.seed = s.title
            miniArt.label = s.title.take(1).uppercase()
            miniTitle.text = s.title
            miniSub.text = s.artist
            miniPlay.update(if (playing) Icons.PAUSE else Icons.PLAY, INK)
        }
        npArtist.text = s?.artist ?: ""
        npSong.text = s?.title ?: "Belum ada lagu"
        turntable.seed = s?.title ?: "Musikku"
        turntable.label = s?.title?.take(1)?.uppercase() ?: "M"
        turntable.setPlaying(playing)
        npPlay.update(if (playing) Icons.PAUSE else Icons.PLAY, INK)
        val off = Color.parseColor("#B5B1A8")
        npShuffle.update(Icons.SHUFFLE, if (Player.shuffle) INK else off)
        npRepeat.update(if (Player.repeatOne) Icons.REPEAT_ONE else Icons.REPEAT, if (Player.repeatOne) INK else off)
        npNext.text = if (Player.shuffle) "Acak" else (Player.peekNext()?.title ?: "-")
    }

    private fun showNowPlaying() {
        if (Player.current == null) return
        np.visibility = View.VISIBLE
        np.translationY = root.height.toFloat()
        np.animate().translationY(0f).setDuration(300).start()
        updateNow()
    }

    private fun hideNowPlaying() {
        if (np.visibility != View.VISIBLE) return
        np.animate().translationY(root.height.toFloat()).setDuration(250)
            .withEndAction { np.visibility = View.GONE }.start()
    }

    private fun moreMenu() {
        AlertDialog.Builder(this)
            .setItems(arrayOf("Tambah ke playlist", "Hentikan musik")) { _, w ->
                if (w == 0) {
                    Player.current?.let { addToPlaylistDialog(it) }
                } else {
                    hideNowPlaying()
                    Player.stop()
                    startService(Intent(this, PlaybackService::class.java).setAction("STOP"))
                }
            }
            .show()
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

    // ------------------------------------------------------------ data

    private fun loadSongs() {
        Thread {
            val l = Library.load(this)
            runOnUiThread {
                allSongs = l
                refreshAll()
            }
        }.start()
    }

    private fun songsOf(name: String): List<Song> {
        val map = allSongs.associateBy { it.uri }
        return Store.get(this, name).mapNotNull { map[it] }
    }

    // ------------------------------------------------------------ dialogs

    private fun addToPlaylistDialog(song: Song) {
        val names = Store.names(this)
        val items = (names + "+ Playlist baru").toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Tambah ke playlist")
            .setItems(items) { _, which ->
                if (which < names.size) {
                    Store.add(this, names[which], listOf(song.uri))
                    Toast.makeText(this, "Ditambahkan ke " + names[which], Toast.LENGTH_SHORT).show()
                    refreshAll()
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
                    refreshAll()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deletePlaylistDialog(name: String) {
        AlertDialog.Builder(this)
            .setTitle("Hapus playlist \"$name\"?")
            .setPositiveButton("Hapus") { _, _ -> Store.delete(this, name); refreshAll() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun confirmRemove(name: String, s: Song) {
        AlertDialog.Builder(this)
            .setTitle("Hapus \"${s.title}\" dari playlist?")
            .setPositiveButton("Hapus") { _, _ -> Store.remove(this, name, s.uri); refreshAll() }
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
                refreshAll()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ------------------------------------------------------------ pilih file manual

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
