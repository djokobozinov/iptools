package io.bozinov.iptools.ui

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import androidx.appcompat.app.AppCompatActivity
import io.bozinov.iptools.R
import io.bozinov.iptools.connections.ApiCalls
import io.bozinov.iptools.models.IPDetails
import kotlinx.android.synthetic.main.content_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*


/**
 * Created by djokob on 5/23/17.
 *
 */
class MainActivity : AppCompatActivity() {
    private var wifiManager: WifiManager? = null
    private var dbm: String? = null
    private var strengthInPercentage: String? = null
    private var speed: String? = null
    private var ssid: String? = null
    private var internalip: String? = null
    private var mask = 0
    private var gateway = 0
    private var dns1 = 0
    private var dns2 = 0
    private var leaseduration = 0
    private var localhost: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        getIpDetails()
    }

    private val ipCallback: Callback<IPDetails> = object : Callback<IPDetails> {
        override fun onResponse(call: Call<IPDetails>?, response: Response<IPDetails>?) {

            val ipDetails = response?.body()

            ipmainTV?.text = ipDetails?.ip
            countryTV?.text = ipDetails?.country
            regionTV?.text = ipDetails?.region
            cityTV?.text = ipDetails?.city
            val coords = ipDetails?.coords?.split(',')
            if (coords != null) {
                latTV?.text = coords!![0]
                lonTV?.text = coords!![0]
            }
            asnTV?.text = ipDetails?.asn
        }

        override fun onFailure(call: Call<IPDetails>?, t: Throwable?) {

        }
    }

    @SuppressLint("SetTextI18n")
    private fun getIpDetails() {

        getOnlineInfo()
        getAdditionalInfo()

        setUI()
    }

    private fun getAdditionalInfo() {

        if (wifiManager == null) {
            return
        }

        if (wifiManager!!.isWifiEnabled) {
            val wifiInfo = wifiManager!!.connectionInfo
            val dhcpInfo = wifiManager!!.dhcpInfo
            mask = dhcpInfo.netmask
            gateway = dhcpInfo.gateway
            dns1 = dhcpInfo.dns1
            dns2 = dhcpInfo.dns2
            leaseduration = dhcpInfo.leaseDuration
            //get local
            val thread = Thread(Runnable {
                try {
                    localhost = InetAddress.getLocalHost().hostAddress
                    localhostTV.text = localhost
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
            thread.start()
            //---
            if (wifiInfo != null) {
                dbm = wifiInfo.rssi.toString()
                speed = wifiInfo.linkSpeed.toString()
                ssid = wifiInfo.ssid.toString()
                @Suppress("DEPRECATION")
                internalip = Formatter.formatIpAddress(wifiInfo.ipAddress)
            }
            strengthInPercentage = WifiManager.calculateSignalLevel(wifiInfo.rssi, 100).toString()
        }
    }

    private fun setUI() {
        signalTV.text = "$strengthInPercentage%($dbm dBm)"
        speedTV.text = speed + "Mbps"
        ssidTV.text = ssid
        internalipTV.text = internalip
        macaddressTV.text = getMacAddress()
        broadcastTV.text = getBroadcastAddress().hostAddress
        maskTV.text = intToIP(mask)
        gatewayTV.text = intToIP(gateway)
        dns1TV.text = intToIP(dns1)
        dns2TV.text = intToIP(dns2)
        leasedurationTV.text = leaseduration.toString()
    }

    private fun getOnlineInfo() {
        val retrofit = Retrofit.Builder().baseUrl(getString(R.string.base_url)).addConverterFactory(GsonConverterFactory.create()).build()
        val ipDetailsService: ApiCalls.IPDetailsService? = retrofit.create(ApiCalls.IPDetailsService::class.java)
        val call = ipDetailsService?.getIPDetails()
        call?.enqueue(ipCallback)
    }

    private fun getMacAddress(): String {
        try {
            val all = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (nif in all) {
                if (!nif.name.equals("wlan0", ignoreCase = true)) continue

                val macBytes = nif.hardwareAddress ?: return ""

                val res1 = StringBuilder()
                for (b in macBytes) {
                    res1.append(String.format("%02X:", b))
                }

                if (res1.isNotEmpty()) {
                    res1.deleteCharAt(res1.length - 1)
                }
                return res1.toString()
            }
        } catch (ex: Exception) {
        }

        return "02:00:00:00:00:00"
    }

    @Throws(IOException::class)
    private fun getBroadcastAddress(): InetAddress {
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifi.dhcpInfo
        val broadcast = dhcp.ipAddress and dhcp.netmask or dhcp.netmask.inv()
        val quads = ByteArray(4)
        for (k in 0..3)
            quads[k] = (broadcast shr k * 8).toByte()
        return InetAddress.getByAddress(quads)
    }

    private fun intToIP(ipAddress: Int): String {
        return String.format("%d.%d.%d.%d", ipAddress and 0xff,
                ipAddress shr 8 and 0xff, ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff)
    }
}
