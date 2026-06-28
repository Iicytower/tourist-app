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
import kotlin.coroutines.resumeWithException

class AndroidLocationService(
    private val context: Context
) : LocationService {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override fun hasLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    override suspend fun getCurrentLocation(): Result<Location> = runCatching {
        if (!hasLocationPermission()) {
            throw SecurityException("Brak uprawnien do lokalizacji. Zezwol aplikacji na dostep do GPS.")
        }
        val lastKnown = tryGetLastKnown()
        if (lastKnown != null) return@runCatching lastKnown

        withTimeoutOrNull(15_000L) {
            runCatching { requestFreshLocation() }.getOrNull()
        } ?: throw Exception("Nie udalo sie pobrac lokalizacji. Sprawdz czy GPS jest wlaczony.")
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
                continuation.resumeWithException(Exception("GPS i siec sa niedostepne na tym urzadzeniu"))
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
