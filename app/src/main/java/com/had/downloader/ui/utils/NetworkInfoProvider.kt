package com.had.downloader.ui.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager

enum class ConnectionType { WIFI, MOBILE, ETHERNET, VPN, OTHER, NONE }

data class NetworkInfo(
    val type: ConnectionType,
    val carrierName: String? = null 
)

object NetworkInfoProvider {
    fun current(context: Context): NetworkInfo {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkInfo(ConnectionType.NONE)
        val network = cm.activeNetwork ?: return NetworkInfo(ConnectionType.NONE)
        val caps = cm.getNetworkCapabilities(network) ?: return NetworkInfo(ConnectionType.NONE)

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkInfo(ConnectionType.WIFI)
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkInfo(ConnectionType.ETHERNET)
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkInfo(ConnectionType.VPN)
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                NetworkInfo(ConnectionType.MOBILE, carrierName(context))
            else -> NetworkInfo(ConnectionType.OTHER)
        }
    }

    private fun carrierName(context: Context): String? {
        return runCatching {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            tm?.networkOperatorName?.takeIf { it.isNotBlank() }
                ?: tm?.simOperatorName?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
