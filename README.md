# Bug109900820
This is a sample project for [Bug 109900820](https://issuetracker.google.com/issues/109900820)

Note: You need to know the SSID and the mac address of the network you're trying to connect to in this sample project. You can retrieve those values with e.g. the [Bonjour Browser](https://hobbyistsoftware.com/bonjourbrowser)

# Problem description
In my app I'm trying to connect to a hotspot network and sending some wifi-configuration to a device on that hotspot network. I use the following to connect to the hotspot

    NetworkRequest networkRequest = new NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build();

Then in the callback I fetch the NetworkInfo from the available network using the connectivity manager (initialized earlier):

    ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
    
    @Override
    public void onAvailable(Network network) {
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
        (...)

At this point `networkInfo.getExtraInfo()` is returning `null` on Android P Beta, where it is returning the SSID on previous versions of Android. I wonder if this is a bug, or if it is intentional that we can’t get that extra (SSID) info as of Android P. I’ve got the `ACCESS_NETWORK_STATE` permission, so that’s not the issue. And again, it’s returning the extra info (SSID) fine for older Android versions.

# Steps to reproduce
Enter the SSID and the mac address in the appropriate input fields and press the "Go!" button.

**Note**: There's a work around for the fact that we get a security exception on Android 6.0 Marshmallow when calling `connectivityManager.requestNetwork(...)` 

    java.lang.SecurityException: com.xyz.app was not granted either of these permissions: android.permission.CHANGE_NETWORK_STATE, android.permission.WRITE_SETTINGS.

Even though we have `android.permission.CHANGE_NETWORK_STATE` in our manifest `¯\_(ツ)_/¯`

# Sample output
In the project I included a log statement which outputs the retrieved networkInfo object in the `onAvailable` method. On Android Marshmallow the extra info is the SSID I wanted to connect to 'NODE-0066':

    06-15 14:19:49.475 27358-27358/nl.codestone.bug109900820 I/MainActivity: connectivityManager.requestNetwork
    06-15 14:19:49.484 27358-27509/nl.codestone.bug109900820 D/MainActivity: networkInfo: [type: WIFI[], state: CONNECTED/CONNECTED, reason: (unspecified), extra: "NODE-0066", roaming: false, failover: false, isAvailable: true]

On Android P Beta the extra info is 'null':

    06-15 14:21:47.373 18649-18649/nl.codestone.bug109900820 I/MainActivity: connectivityManager.requestNetwork
    06-15 14:21:47.380 18649-19256/nl.codestone.bug109900820 D/MainActivity: networkInfo: [type: WIFI[], state: CONNECTED/CONNECTED, reason: (unspecified), extra: (none), failover: false, available: true, roaming: false]
