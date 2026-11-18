package com.matrix.launcher

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal data class NetworkState(val connected: Boolean = false, val ethernet: Boolean = false)
internal data class HomeNavigationRequest(val page: String = "home", val sequence: Int = 0, val focusReset: Int = 0)

/** Event-driven, registered only while the activity is started; no network polling or location grant. */
internal fun networkStates(context: Context) = callbackFlow {
    val manager = context.getSystemService(ConnectivityManager::class.java)
    fun update() {
        val capabilities = manager?.getNetworkCapabilities(manager.activeNetwork)
        val ethernet = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        val wifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        trySend(NetworkState(ethernet || wifi, ethernet))
    }
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = update()
        override fun onLost(network: Network) = update()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = update()
    }
    var registered = false
    try { manager?.registerDefaultNetworkCallback(callback); registered = manager != null; update() }
    catch (denied: SecurityException) { trySend(NetworkState()) }
    awaitClose { if (registered) manager?.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()
