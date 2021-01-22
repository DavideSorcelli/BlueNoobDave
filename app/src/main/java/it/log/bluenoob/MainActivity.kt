package it.log.bluenoob

import android.Manifest
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.reflect.Method


const val TAG = "wake_up"
private const val VEHICLE_PREFIX = "IVECO_"
private const val VEHICLE_NAME = "IVECO_30766"
private const val SCAN_PERIOD: Long = 60 * 1000
private const val MANUFACTURER_ID: Int = 0xFFFF

class MainActivity : AppCompatActivity() {

    private val bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
    private val handler = Handler()
    private var lastBleScanCallback: BleScanCallback? = null
    lateinit var runnable: MyRunnable

    private var targetDevice: BluetoothDevice? = null


    inner class MyRunnable: Runnable {
        override fun run() {
            log("TIMEOUT")
            bluetoothLeScanner?.stopScan(lastBleScanCallback)
            lastBleScanCallback = null
        }
    }


    private fun scanLeDevice() {

        // Starto il runnable che interromperà la scan
        handler.postDelayed(MyRunnable().also { runnable = it }, SCAN_PERIOD)

        val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .build()

        val charset = Charsets.US_ASCII
        val manufacturerData  = VEHICLE_NAME.split("_")[1].toByteArray(charset)
        val scanFilters = ScanFilter.Builder().setManufacturerData(
                MANUFACTURER_ID,
                manufacturerData
        ).build()

        bluetoothLeScanner.startScan(
                listOf(scanFilters),
                scanSettings,
                BleScanCallback().also { lastBleScanCallback = it }
        )

        log("START SCAN")

    }


    inner class BleScanCallback(): ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)


            log("onScanResult")

            //if (alreadyRun) return


            val manufacturerSpec = result?.scanRecord
                    ?.getManufacturerSpecificData(MANUFACTURER_ID)
                    ?: return

            val vin = manufacturerSpec.toString(Charsets.US_ASCII)

            log("Found vin $vin ---- ${result.device.address}")
            bluetoothLeScanner?.stopScan(lastBleScanCallback)
            handler.removeCallbacks(runnable)
            log("STOP SCAN")

            Thread.sleep(2000)

            targetDevice = result.device!!
            connectUsingDevice()
            //alreadyRun = true

        }

        override fun onScanFailed(errorCode: Int) {
            log("SCAN FAILED!!!")
        }
    }

    inner class MyBluetoothGattCallback: BluetoothGattCallback() {
        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status)
            log("onPhyUpdate")
        }

        override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyRead(gatt, txPhy, rxPhy, status)
            log("onPhyRead")
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            log("onConnectionStateChange")

            var disconnected = false

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    log("Connected to GATT server!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1")
//                    Log.i(TAG, "Attempting to start service discovery: " +
//                            bluetoothGatt?.discoverServices())
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    log("STATE_DISCONNECTED")

                    disconnected = true
                    log("RETRYING CONNECT")
                    connectUsingDevice()

//                    intentAction = ACTION_GATT_DISCONNECTED
//                    connectionState = STATE_DISCONNECTED
//                    Log.i(TAG, "Disconnected from GATT server.")
//                    broadcastUpdate(intentAction)
                }
                else -> {
                    log("?????????????????? state ??")
                }
            }

            if (disconnected.not()) {
                gatt?.close()
                gatt?.disconnect()
            }

        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            log("onServicesDiscovered")
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            log("onCharacteristicRead")
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            log("onCharacteristicWrite")
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            log("onCharacteristicChanged")
        }

        override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorRead(gatt, descriptor, status)
            log("onDescriptorRead")
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            log("onDescriptorWrite")
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
            super.onReliableWriteCompleted(gatt, status)
            log("onReliableWriteCompleted")
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            log("onReadRemoteRssi")
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            log("onMtuChanged")
        }
    }


    fun connectUsingDevice() {

        log("CONNECT GATT")
        val gattDevice = targetDevice!!.connectGatt(
                this,
                false,
                MyBluetoothGattCallback(),
                TRANSPORT_LE
        )

        val refreshedCache = refreshDeviceCache(gattDevice)
        log("Devices cache refreshed: $refreshedCache")
    }

    fun log(msg: String) = Log.d(TAG, msg)

    private fun refreshDeviceCache(gatt: BluetoothGatt): Boolean {
        try {
            val localMethod: Method = gatt.javaClass.getMethod("refresh")
            return (localMethod.invoke(gatt) as Boolean)
        } catch (localException: Exception) {
            Log.e(TAG, "An exception occurred while refreshing device: ${localException.message}")
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            GlobalScope.launch {  scanLeDevice() }
        }
    }

    override fun onResume() {
        super.onResume()
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION).apply {
            if  (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }

        val request = permissionsBuilder(*permissions.toTypedArray()).build()
        request.send()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}