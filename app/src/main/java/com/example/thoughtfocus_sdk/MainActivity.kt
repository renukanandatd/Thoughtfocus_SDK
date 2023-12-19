package com.example.thoughtfocus_sdk

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bbpos.bbdevice.BBDeviceController
import com.google.android.material.navigation.NavigationView
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var showDeviceListButton: Button
    private lateinit var discoverDevicesButton: Button
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var pairedDevicesListView: ListView
    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private lateinit var pairedDevices : Set<BluetoothDevice>
    private lateinit var connectedDeviceTextView: TextView
    val bbDeviceController : BBDeviceController? = null

    lateinit var toggle: ActionBarDrawerToggle
    private lateinit var drawerLayout: DrawerLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

        toggle = ActionBarDrawerToggle(
            this, drawerLayout,
            R.string.open_nav,
            R.string.close_nav
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView:NavigationView = findViewById<NavigationView>(R.id.nav_view)

        connectedDeviceTextView = findViewById(R.id.textView)
        discoverDevicesButton = findViewById(R.id.discoverDevicesButton)
        pairedDevicesListView = ListView(this)
        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)


        navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_pairedDevice -> if (checkBluetoothPermission()) {
                    showPairedDevicesDialog()
                } else {
                    requestBluetoothPermission()
                }

            }
            true
        }

        discoverDevicesButton.setOnClickListener {
            if (checkBluetoothPermission()) {
                bbDeviceController?.disconnectBT()
            } else {
                requestBluetoothPermission()
            }
        }


        // Initialize BluetoothAdapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Check if Bluetooth is supported on the device
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            showBluetoothNotSupportedDialog()
        }

        // Register for Bluetooth device discovery broadcasts
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(deviceDiscoveryReceiver, filter)
    }

    private fun showPairedDevicesDialog() {
        // Get paired devices
        if (checkBluetoothPermission()) {
        pairedDevices = bluetoothAdapter.bondedDevices
        deviceListAdapter.clear()
        for (device in pairedDevices) {
            deviceListAdapter.add(device.name)
        }
        }

        // Create the dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Paired Bluetooth Devices")
            .setAdapter(deviceListAdapter, DialogInterface.OnClickListener { _, position ->
                bbDeviceController?.connectBT(pairedDevices.elementAtOrNull(position))
                connectToDevice(pairedDevices.elementAtOrNull(position))
            })
            .setNegativeButton("Cancel", null)
            .create()

        // Show the dialog
        dialog.show()
    }

    private fun startDeviceDiscovery() {
        // Clear the previous list
        deviceListAdapter.clear()

        // Register for device discovery broadcasts
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(deviceDiscoveryReceiver, filter)

        // Start Bluetooth discovery
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if(checkBluetoothPermission()) {
            bluetoothAdapter.startDiscovery()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Paired Bluetooth Devices")
            .setAdapter(deviceListAdapter, DialogInterface.OnClickListener { _, position ->
                bbDeviceController?.connectBT(pairedDevices.elementAtOrNull(position))
                connectToDevice(pairedDevices.elementAtOrNull(position))
            })
            .setNegativeButton("Cancel", null)
            .create()

        // Show the dialog
        dialog.show()
    }

    private fun showBluetoothNotSupportedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Bluetooth Not Supported")
            .setMessage("This device does not support Bluetooth.")
            .setPositiveButton("OK") { _, _ -> finish() }
            .show()
    }

    private val deviceDiscoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if(checkBluetoothPermission()) {
                if (BluetoothDevice.ACTION_FOUND == action) {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        deviceListAdapter.add(device.name)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(deviceDiscoveryReceiver)
    }

    fun checkBluetoothPermission(): Boolean {
        val bluetoothPermission = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.BLUETOOTH
        )
        val bluetoothAdminPermission = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.BLUETOOTH_ADMIN
        )
        return (bluetoothPermission == PackageManager.PERMISSION_GRANTED &&
                bluetoothAdminPermission == PackageManager.PERMISSION_GRANTED)
    }

 fun requestBluetoothPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN
            ),
            BLUETOOTH_PERMISSION_REQUEST_CODE
        )
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            BLUETOOTH_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    showPairedDevicesDialog()
                } else {
                    showPermissionDeniedDialog()
                }
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("Bluetooth permission is required to access paired devices.")
            .setPositiveButton("OK", DialogInterface.OnClickListener { _, _ ->
                finish()
            })
            .show()
    }

    companion object {
        private const val BLUETOOTH_PERMISSION_REQUEST_CODE = 1001
    }

    private fun connectToDevice(device: BluetoothDevice?) {
        if (device != null) {
            try {
                if (checkBluetoothPermission()) {
                    var connectedDeviceInfo = "Connected to: ${device.name} (${device.address})"
                    connectedDeviceTextView.text = connectedDeviceInfo
                    findViewById<TextView>(R.id.connectedDevice).text = connectedDeviceInfo
                    //connectedDeviceTextView.text = bbDeviceController?.getDeviceInfo().toString()
                }
                } catch (e: IOException) {
                    // Handle connection error
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to connect", Toast.LENGTH_SHORT).show()
                }
        }
        }
    }





