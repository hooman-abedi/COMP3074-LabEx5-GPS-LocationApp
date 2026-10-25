package com.example.gpslocation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.gpslocation.databinding.ActivityMainBinding
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Shows the device's current latitude and longitude, read directly from the
 * GPS provider (LocationManager.GPS_PROVIDER).
 *
 * Permission flow:
 *  1. ACCESS_FINE_LOCATION + ACCESS_COARSE_LOCATION are declared in the manifest.
 *  2. At runtime the app checks for FINE permission and requests it if missing.
 *  3. If the user denies, a rationale is shown; if permanently denied, the user
 *     is sent to the app's settings page.
 *  4. If GPS is switched off, the user is sent to Location settings.
 */
class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var locationManager: LocationManager
    private var isListening = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        when {
            fineGranted -> startGpsUpdates()
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ->
                showRationale()
            else -> {
                // Denied with "Don't ask again", or only approximate location chosen
                binding.statusText.text = getString(R.string.status_permission_denied)
                showSettingsDialog()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        binding.refreshButton.setOnClickListener { checkPermissionAndStart() }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionAndStart()
    }

    override fun onPause() {
        super.onPause()
        stopGpsUpdates()
    }

    private fun hasFineLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun checkPermissionAndStart() {
        if (hasFineLocationPermission()) {
            startGpsUpdates()
        } else {
            binding.statusText.text = getString(R.string.status_requesting_permission)
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun startGpsUpdates() {
        if (!hasFineLocationPermission()) return

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            binding.statusText.text = getString(R.string.status_gps_off)
            showEnableGpsDialog()
            return
        }

        try {
            // Show the last fix the GPS knows about right away, if any
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?.let { showLocation(it, cached = true) }
                ?: run { binding.statusText.text = getString(R.string.status_waiting_fix) }

            if (!isListening) {
                // Request live updates from the GPS receiver: every 2 s or 1 m
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    this,
                    mainLooper
                )
                isListening = true
            }
        } catch (e: SecurityException) {
            binding.statusText.text = getString(R.string.status_permission_denied)
        }
    }

    private fun stopGpsUpdates() {
        if (isListening) {
            locationManager.removeUpdates(this)
            isListening = false
        }
    }

    // ---- LocationListener ----

    override fun onLocationChanged(location: Location) {
        showLocation(location, cached = false)
    }

    override fun onProviderDisabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER) {
            binding.statusText.text = getString(R.string.status_gps_off)
        }
    }

    override fun onProviderEnabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER) {
            binding.statusText.text = getString(R.string.status_waiting_fix)
        }
    }

    @Deprecated("Required for API < 29")
    @Suppress("DEPRECATION")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    // ---- UI ----

    private fun showLocation(location: Location, cached: Boolean) {
        binding.latitudeValue.text = String.format(Locale.US, "%.6f°", location.latitude)
        binding.longitudeValue.text = String.format(Locale.US, "%.6f°", location.longitude)
        binding.accuracyValue.text =
            if (location.hasAccuracy()) String.format(Locale.US, "± %.1f m", location.accuracy)
            else "—"
        binding.timeValue.text =
            DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(location.time))
        binding.statusText.text = getString(
            if (cached) R.string.status_last_known else R.string.status_live
        )
    }

    private fun showRationale() {
        AlertDialog.Builder(this)
            .setTitle(R.string.rationale_title)
            .setMessage(R.string.rationale_message)
            .setPositiveButton(R.string.try_again) { _, _ -> checkPermissionAndStart() }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                binding.statusText.text = getString(R.string.status_permission_denied)
            }
            .show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.rationale_title)
            .setMessage(R.string.settings_message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null)
                    )
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showEnableGpsDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.gps_off_title)
            .setMessage(R.string.gps_off_message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        private const val MIN_TIME_MS = 2_000L
        private const val MIN_DISTANCE_M = 1f
    }
}
