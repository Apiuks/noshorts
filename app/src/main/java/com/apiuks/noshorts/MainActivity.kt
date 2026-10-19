package com.apiuks.noshorts

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var timeText: TextView
    private lateinit var hint: TextView
    private lateinit var permText: TextView
    private lateinit var input: EditText
    private lateinit var start: Button
    private lateinit var strict: Switch
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() { refresh(); handler.postDelayed(this, 1000) }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun bg(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun prefs() = getSharedPreferences("lock", Context.MODE_PRIVATE)
    private fun locked() = System.currentTimeMillis() < prefs().getLong("end", 0L)
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun serviceEnabled(): Boolean {
        val s = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return s.contains("$packageName/")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sv = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0F1115"))
            setOnApplyWindowInsetsListener { v, insets ->
                val top: Int
                val bottom: Int
                if (Build.VERSION.SDK_INT >= 30) {
                    val b = insets.getInsets(WindowInsets.Type.systemBars())
                    top = b.top
                    bottom = b.bottom
                } else {
                    top = insets.systemWindowInsetTop
                    bottom = insets.systemWindowInsetBottom
                }
                v.setPadding(0, top, 0, bottom)
                insets
            }
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(32), dp(24), dp(24))
        }
        sv.addView(root)

        val title = TextView(this).apply {
            text = "Shorts Kilidi"
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        permText = TextView(this).apply {
            textSize = 14f
            setPadding(0, dp(4), 0, dp(20))
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = bg(Color.parseColor("#1A1D24"), 20)
            setPadding(dp(20), dp(32), dp(20), dp(32))
        }
        timeText = TextView(this).apply {
            textSize = 44f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        hint = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#8A8F98"))
            setPadding(0, dp(8), 0, 0)
        }
        card.addView(timeText)
        card.addView(hint)

        input = EditText(this).apply {
            hint = "Kaç dakika?"
            setHintTextColor(Color.parseColor("#6B7078"))
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_NUMBER
            gravity = Gravity.CENTER
            textSize = 20f
            background = bg(Color.parseColor("#1A1D24"), 14)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val chips = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, dp(12))
        }
        listOf(30, 60, 120, 240).forEach { m ->
            val chip = Button(this).apply {
                text = if (m < 60) "$m dk" else "${m / 60} sa"
                isAllCaps = false
                setTextColor(Color.WHITE)
                background = bg(Color.parseColor("#262A33"), 12)
                setOnClickListener { input.setText(m.toString()) }
            }
            chips.addView(chip, LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                setMargins(dp(4), 0, dp(4), 0)
            })
        }

        strict = Switch(this).apply {
            text = "Sıkı mod"
            textSize = 16f
            setTextColor(Color.WHITE)
            isChecked = prefs().getBoolean("strict", false)
            setOnCheckedChangeListener { _, checked ->
                prefs().edit().putBoolean("strict", checked).apply()
            }
        }
        val strictInfo = TextView(this).apply {
            text = "Açıksa kilit süresince bu uygulamanın ayarları ve Erişilebilirlik listesi açılamaz. En fazla 24 saat."
            textSize = 13f
            setTextColor(Color.parseColor("#8A8F98"))
            setPadding(0, dp(4), 0, dp(16))
        }

        start = Button(this).apply {
            text = "Kilidi başlat"
            isAllCaps = false
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = bg(Color.parseColor("#E5484D"), 14)
            setOnClickListener { startLock() }
        }
        val access = Button(this).apply {
            text = "Erişilebilirlik iznini aç"
            isAllCaps = false
            setTextColor(Color.parseColor("#8AB4F8"))
            background = bg(Color.TRANSPARENT, 12)
            setOnClickListener { showDisclosure() }
        }

        root.addView(title)
        root.addView(permText)
        root.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(20) })
        root.addView(input, LinearLayout.LayoutParams(-1, dp(56)))
        root.addView(chips, LinearLayout.LayoutParams(-1, -2))
        root.addView(strict)
        root.addView(strictInfo)
        root.addView(start, LinearLayout.LayoutParams(-1, dp(56)))
        root.addView(access, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(12) })
        setContentView(sv)
    }

    private fun showDisclosure() {
        AlertDialog.Builder(this)
            .setTitle("Erişilebilirlik izni")
            .setMessage(
                "Shorts Kilidi, kilit aktifken YouTube'da Shorts ekranının açılıp açılmadığını anlamak için Android Erişilebilirlik Servisi'ni kullanır.\n\n" +
                "• YouTube ekranının yapısına bakar, yalnızca Shorts ekranı var mı diye kontrol eder.\n" +
                "• Ekrandaki yazı, mesaj, şifre gibi hiçbir içeriği kaydetmez, saklamaz ve kimseyle paylaşmaz.\n" +
                "• Shorts algılanırsa geri tuşuna basar.\n" +
                "• Sıkı mod açıksa, kilit süresince bu uygulamanın ayar sayfalarını da kapatır.\n\n" +
                "İzni istediğin zaman Ayarlar'dan kapatabilirsin (sıkı modlu kilit süresi hariç). Devam edersen Erişilebilirlik ayarları açılacak."
            )
            .setPositiveButton("Kabul ediyorum") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun startLock() {
        if (locked()) { toast("Kilit zaten aktif"); return }
        var min = input.text.toString().toLongOrNull() ?: 0L
        if (min <= 0) { toast("Dakika gir"); return }
        if (!serviceEnabled()) { showDisclosure(); return }
        if (strict.isChecked) {
            if (min > 1440) { min = 1440; input.setText("1440") }
            val m = min
            AlertDialog.Builder(this)
                .setTitle("Sıkı mod açık")
                .setMessage("$m dakika boyunca bu uygulamanın ayarlarına ve Erişilebilirlik listesine girilemeyecek. Süre dolmadan kapatamazsın. Onaylıyor musun?")
                .setPositiveButton("Onaylıyorum") { _, _ -> begin(m) }
                .setNegativeButton("Vazgeç", null)
                .show()
        } else {
            begin(min)
        }
    }

    private fun begin(min: Long) {
        prefs().edit().putLong("end", System.currentTimeMillis() + min * 60000).apply()
        refresh()
    }

    override fun onResume() { super.onResume(); handler.post(tick) }
    override fun onPause() { super.onPause(); handler.removeCallbacks(tick) }

    private fun refresh() {
        val on = serviceEnabled()
        permText.text = if (on) "Erişilebilirlik izni: açık" else "Erişilebilirlik izni: kapalı"
        permText.setTextColor(Color.parseColor(if (on) "#46A758" else "#E5484D"))
        val left = prefs().getLong("end", 0L) - System.currentTimeMillis()
        if (left > 0) {
            val s = left / 1000
            timeText.text = String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
            timeText.setTextColor(Color.parseColor("#E5484D"))
            hint.text = "Kilit aktif"
            start.alpha = 0.4f
            input.isEnabled = false
            strict.isEnabled = false
        } else {
            timeText.text = "Kilit kapalı"
            timeText.setTextColor(Color.parseColor("#46A758"))
            hint.text = "Süre seç ve başlat"
            start.alpha = 1f
            input.isEnabled = true
            strict.isEnabled = true
        }
    }
}
