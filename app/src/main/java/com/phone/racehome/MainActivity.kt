package com.phone.racehome

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.phone.racehome.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        binding.btnAccessibility.setOnClickListener { openAccessibilitySettings() }
        binding.btnDefaultHome.setOnClickListener { openDefaultHomeSettings() }
        binding.btnBattery.setOnClickListener { requestBatteryIgnore() }
        binding.btnArm.setOnClickListener { arm() }
        binding.btnDisarm.setOnClickListener { disarm() }
        binding.btnScan.setOnClickListener { scanPackages() }
        binding.btnOpenTargets.setOnClickListener { openAllTargets() }

        if (intent?.getBooleanExtra(EXTRA_FROM_BOOT, false) == true && Prefs.isArmed(this)) {
            Toast.makeText(this, "Race Home armed after boot", Toast.LENGTH_SHORT).show()
            KeepAliveService.start(this)
        }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        if (Prefs.isArmed(this)) {
            KeepAliveService.start(this)
        }
    }

    private fun refreshStatus() {
        val a11y = isAccessibilityEnabled()
        val armed = Prefs.isArmed(this)
        binding.statusText.text = buildString {
            appendLine("Armed: ${if (armed) "YES" else "no"}")
            appendLine("Accessibility: ${if (a11y) "ON" else "OFF ← turn this on"}")
            appendLine("Default home: set via button 2 (picker)")
            appendLine()
            appendLine("Tip: when Android asks for Home launcher,")
            appendLine("choose Race Home → Always.")
        }
    }

    private fun arm() {
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "Enable Accessibility first (button 1)", Toast.LENGTH_LONG).show()
            openAccessibilitySettings()
            return
        }
        Prefs.setArmed(this, true)
        KeepAliveService.start(this)
        Toast.makeText(this, "ARMED — guard running", Toast.LENGTH_SHORT).show()
        refreshStatus()
    }

    private fun disarm() {
        Prefs.setArmed(this, false)
        KeepAliveService.stop(this)
        Toast.makeText(this, "Disarmed", Toast.LENGTH_SHORT).show()
        refreshStatus()
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        Toast.makeText(
            this,
            "Find “Race Home” → enable it",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun openDefaultHomeSettings() {
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (_: Exception) {
            // Fallback: trigger home so chooser may appear
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            startActivity(home)
        }
    }

    private fun requestBatteryIgnore() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            Toast.makeText(this, "Already unrestricted", Toast.LENGTH_SHORT).show()
            return
        }
        val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(i)
    }

    private fun scanPackages() {
        val pm = packageManager
        val installed = pm.getInstalledPackages(0)
        val hits = installed.map { it.packageName }.filter { Blocklist.isBlocked(it) }.sorted()
        binding.scanResult.text = if (hits.isEmpty()) {
            "No keyword matches found (or QUERY_ALL_PACKAGES limited).\nStill opens known list: ${Blocklist.PACKAGES}"
        } else {
            "Suspect packages:\n" + hits.joinToString("\n")
        }
    }

    private fun openAllTargets() {
        val pm = packageManager
        val installed = try {
            pm.getInstalledPackages(0).map { it.packageName }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
        val targets = (Blocklist.PACKAGES + installed.filter { Blocklist.isBlocked(it) })
            .distinct()
            .filter { pkg ->
                try {
                    pm.getApplicationInfo(pkg, 0)
                    true
                } catch (_: Exception) {
                    false
                }
            }

        if (targets.isEmpty()) {
            Toast.makeText(this, "No targets installed / visible", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(
            this,
            "Opening App Info for ${targets.size} apps. Use Force stop / Disable.",
            Toast.LENGTH_LONG
        ).show()

        // Open first; user can press button again for next after back
        var idx = 0
        fun openNext() {
            if (idx >= targets.size) return
            val pkg = targets[idx++]
            val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$pkg")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(i)
        }
        openNext()
        // Stash remaining for repeated taps via scan result text
        binding.scanResult.text =
            "Opened App Info. Tap button again for next.\nTargets:\n" + targets.joinToString("\n")
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(this, GuardAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    companion object {
        const val EXTRA_FROM_BOOT = "from_boot"
    }
}
