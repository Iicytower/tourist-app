package com.iicytower.wanderlist.data.local.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import com.iicytower.wanderlist.domain.model.Location
import com.iicytower.wanderlist.domain.repository.LocationService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class AndroidLocationService(
    private val context: Context
) : LocationService {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override fun hasLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    override suspend fun getCurrentLocation(): Result<Location> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Brak uprawnień do lokalizacji"))
        }

        val lastKnown = tryGetLastKnown()
        if (lastKnown != null) return Result.success(lastKnown)

        val fresh = withTimeoutOrNull(15_000L) { requestFreshLocation() }
        return fresh?.let { Result.success(it) }
            ?: Result.failure(Exception("Nie udało się pobrać lokalizacji (timeout)"))
    }

    @Suppress("MissingPermission")
    private fun tryGetLastKnown(): Location? {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val now = System.currentTimeMillis()
        return providers.mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.filter { loc -> now - loc.time < 60_000L }
            .maxByOrNull { it.accuracy }
            ?.let { Location(it.latitude, it.longitude) }
    }

    @Suppress("MissingPermission")
    private suspend fun requestFreshLocation(): Location =
        suspendCancellableCoroutine { continuation ->
            var listener: LocationListener? = null
            listener = LocationListener { androidLoc ->
                continuation.resume(Location(androidLoc.latitude, androidLoc.longitude))
                listener?.let {
                    runCatching { locationManager.removeUpdates(it) }
                }
            }

            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter { locationManager.isProviderEnabled(it) }

            if (providers.isEmpty()) {
                continuation.cancel(Exception("Żaden provider GPS nie jest aktywny"))
                return@suspendCancellableCoroutine
            }

            providers.forEach { provider ->
                runCatching {
                    locationManager.requestLocationUpdates(provider, 0L, 0f, listener)
                }
            }

            continuation.invokeOnCancellation {
                runCatching { locationManager.removeUpdates(listener) }
            }
        }
}
