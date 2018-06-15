package nl.codestone.bug109900820

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.*
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

    private lateinit var goButton: Button
    private lateinit var ssidInput: EditText
    private lateinit var macAddressInput: EditText
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        goButton = findViewById(R.id.go_button)
        goButton.setOnClickListener { onGoButtonClick() }

        ssidInput = findViewById(R.id.ssid_input)
        macAddressInput = findViewById(R.id.mac_address_input)
        statusText = findViewById(R.id.status_text)
    }

    private fun onGoButtonClick() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            // Work around for the fact that we get a security exception on Android 6.0 Marshmallow when calling connectivityManager.requestNetwork(...) later on
            // java.lang.SecurityException: com.xyz.app was not granted either of these permissions: android.permission.CHANGE_NETWORK_STATE, android.permission.WRITE_SETTINGS.
            // Even though we have "android.permission.CHANGE_NETWORK_STATE" in our manifest ¯\_(ツ)_/¯
            val goToSettings = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            goToSettings.data = Uri.parse("package:$packageName")
            startActivity(goToSettings)
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions()
        } else if (!hasEnabledLocationProviders()) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else {
            statusText.text = "We have everything we need to continue"
            attemptToConnectToHotspot(ssidInput.text.toString(), macAddressInput.text.toString())
        }
    }

    private fun attemptToConnectToHotspot(ssid: String, macAddress: String) {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        val netId = getNetworkId(ssid, macAddress, wifiManager)

        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                val networkInfo = connectivityManager.getNetworkInfo(network)
                Log.d(TAG, "networkInfo: $networkInfo")
                statusText.post { statusText.text = "networkInfo.extraInfo: ${networkInfo.extraInfo}" }
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities)= Unit
            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties)= Unit
            override fun onLosing(network: Network, maxMsToLive: Int)= Unit
            override fun onLost(network: Network)= Unit
        }

        wifiManager.enableNetwork(netId, true)
        Log.i(TAG, "connectivityManager.requestNetwork")
        connectivityManager.requestNetwork(networkRequest, networkCallback)
    }

    private fun getNetworkId(ssid: String, bssid: String, wifiManager: WifiManager): Int {
        for (wifiConfiguration in wifiManager.configuredNetworks) {
            if (wifiConfiguration.SSID == String.format("\"%s\"", ssid)) {
                return wifiConfiguration.networkId
            }
        }
        val wifiConfig = WifiConfiguration()
        wifiConfig.SSID = String.format("\"%s\"", ssid)
        wifiConfig.BSSID = bssid
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
        wifiConfig.priority = Integer.MAX_VALUE

        return wifiManager.addNetwork(wifiConfig)
    }

    private fun requestLocationPermissions() {
        requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), RC_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.isEmpty()) {
            Log.d(TAG, "Request was cancelled")
            return
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onGoButtonClick()
        }
    }

    private fun hasEnabledLocationProviders(): Boolean {
        val locationManager : LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.allProviders
        for (provider in providers) {
            if (provider == LocationManager.PASSIVE_PROVIDER) {
                continue
            }
            if (locationManager.isProviderEnabled(provider)) {
                return true
            }

        }
        return false
    }

    companion object {
        private const val RC_PERMISSIONS = 1
        private const val TAG = "MainActivity"
    }

}
