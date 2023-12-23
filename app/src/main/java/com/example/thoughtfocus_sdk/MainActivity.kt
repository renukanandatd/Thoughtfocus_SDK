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
import android.util.Log
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
import com.bbpos.bbdevice.CAPK
import com.example.thoughtfocusmainsdk.MainSDKClass
import com.google.android.material.navigation.NavigationView
import java.io.IOException
import java.util.Arrays
import java.util.Hashtable

class MainActivity : AppCompatActivity() {

    private lateinit var discoverDevicesButton: Button
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var pairedDevicesListView: ListView
    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private lateinit var pairedDevices : Set<BluetoothDevice>
    private lateinit var statusTextView: TextView
    var bbDeviceController : BBDeviceController? = null
    var mainSDKClass : MainSDKClass? = null
    var uid: String? = null
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

        statusTextView = findViewById(R.id.textView)
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

        mainSDKClass = MainSDKClass()
        // Create the dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Paired Bluetooth Devices")
            .setAdapter(deviceListAdapter, DialogInterface.OnClickListener { _, position ->
                pairedDevices.elementAtOrNull(position)?.let { bbDeviceController?.connectBT(it) }
                bbDeviceController?.getDeviceInfo()
                Log.e("Bluetooth connected info",bbDeviceController?.getDeviceInfo().toString())
                //BBDeviceControllerListener.onBTConnected(pairedDevices.elementAtOrNull(position))

              //bbDeviceController?.connectBT(pairedDevices.elementAtOrNull(position))
                connectToDevice(pairedDevices.elementAtOrNull(position))
            })
            .setNegativeButton("Cancel", null)
            .create()

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
            .setTitle("Discover Bluetooth Devices")
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
                    findViewById<TextView>(R.id.connectedDevice).text = connectedDeviceInfo.toString()
                    //bbDeviceControllerListener?.onReturnDeviceInfo()

                }
                } catch (e: IOException) {
                    // Handle connection error
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to connect", Toast.LENGTH_SHORT).show()
                }
        }
        }

    private  var MyBBDeviceControllerListener : BBDeviceController.BBDeviceControllerListener = object :
    BBDeviceController.BBDeviceControllerListener{

       val mainActivity : MainActivity? = null

        override fun onWaitingForCard(p0: BBDeviceController.CheckCardMode?) {
            TODO("Not yet implemented")
        }

        override fun onWaitingReprintOrPrintNext() {
            TODO("Not yet implemented")
        }

        override fun onBTReturnScanResults(p0: MutableList<BluetoothDevice>?) {
            TODO("Not yet implemented")
        }

        override fun onBTScanTimeout() {
            TODO("Not yet implemented")
        }

        override fun onBTScanStopped() {
            TODO("Not yet implemented")
        }

        override fun onBTConnected(p0: BluetoothDevice?) {
            //statusEditText.setText(getString(R.string.bluetooth_connected) + ": " + bluetoothDevice.getAddress())
            //sessionData.reset()
            mainActivity?.bbDeviceController?.getDeviceInfo()
        }

        override fun onBTDisconnected() {
            TODO("Not yet implemented")
        }

        override fun onBTRequestPairing() {
            TODO("Not yet implemented")
        }

        override fun onUsbConnected() {
            TODO("Not yet implemented")
        }

        override fun onUsbDisconnected() {
            TODO("Not yet implemented")
        }

        override fun onSerialConnected() {
            TODO("Not yet implemented")
        }

        override fun onSerialDisconnected() {
            TODO("Not yet implemented")
        }

        override fun onReturnCheckCardResult(
            p0: BBDeviceController.CheckCardResult?,
            p1: Hashtable<String, String>?
        ) {
            TODO("Not yet implemented")
        }

        override fun onReturnCancelCheckCardResult(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onReturnDeviceInfo(deviceInfoData: Hashtable<String, String> ) {
            if (deviceInfoData != null) {
                mainActivity?.uid = deviceInfoData.get("uid")
            }
            val productId: String? = deviceInfoData?.get("productID")
            //sessionData.setProductId(productId)

            var content = ""
            val keys: Array<String>? = deviceInfoData?.keys?.toTypedArray()
            Arrays.sort(keys)
            if (keys != null) {
                for (key in keys) {
                    content += "\n$key : "
                    val obj: String? = deviceInfoData.get(key)
                    content += obj as String
                    if ((key as String).equals("vendorID", ignoreCase = true)) {
                        try {
                            val vendorID: String = deviceInfoData.get("vendorID")!!
                            var vendorIDAscii = ""
                            if (vendorID != null && vendorID != "") {
                                if (!vendorID.substring(0, 2).equals("00", ignoreCase = true)) {
                                   // vendorIDAscii = Utils.hexString2AsciiString(vendorID)
                                    content += """
        ${key as String} (ASCII) : $vendorID"""
                                }
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
            }
            mainActivity?.statusTextView?.setText(content)
        }

        override fun onReturnTransactionResult(p0: BBDeviceController.TransactionResult?) {
            TODO("Not yet implemented")
        }

        override fun onReturnBatchData(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun onReturnReversalData(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun onReturnAmountConfirmResult(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onReturnPinEntryResult(
            p0: BBDeviceController.PinEntryResult?,
            p1: Hashtable<String, String>?
        ) {
            TODO("Not yet implemented")
        }

        override fun onReturnPrintResult(p0: BBDeviceController.PrintResult?) {
            TODO("Not yet implemented")
        }

        override fun onReturnAccountSelectionResult(
            p0: BBDeviceController.AccountSelectionResult?,
            p1: Int
        ) {
            TODO("Not yet implemented")
        }

        override fun onReturnAmount(
            p0: BBDeviceController.AmountInputResult?,
            p1: Hashtable<String, String>?
        ) {
            TODO("Not yet implemented")
        }

        override fun onReturnUpdateAIDResult(p0: Hashtable<String, Any>?) {
            TODO("Not yet implemented")
        }

        override fun onReturnUpdateTerminalSettingResult(p0: BBDeviceController.TerminalSettingStatus?) {
            TODO("Not yet implemented")
        }

        override fun onReturnUpdateTerminalSettingsResult(p0: Hashtable<String, BBDeviceController.TerminalSettingStatus>?) {
            TODO("Not yet implemented")
        }

        override fun onReturnUpdateDisplayStringResult(p0: Boolean, p1: String?) {
            TODO("Not yet implemented")
        }

        override fun onReturnReadDisplayStringResult(p0: Boolean, p1: Hashtable<String, String>?) {
            TODO("Not yet implemented")
        }

        override fun onReturnReadDisplaySettingsResult(p0: Boolean, p1: Hashtable<String, Any>?) {
            TODO("Not yet implemented")
        }

        override fun onReturnReadAIDResult(p0: Hashtable<String, Any>?) {
            TODO("Not yet implemented")
        }

        override fun onReturnReadTerminalSettingResult(p0: Hashtable<String, Any>?) {
            TODO("Not yet implemented")
        }

        override fun onReturnEnableAccountSelectionResult(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onReturnEnableInputAmountResult(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onReturnEnableBluetoothResult(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onReturnCAPKList(p0: MutableList<CAPK>?) {
            TODO("Not yet implemented")
        }

        override fun onReturnCAPKDetail(p0: CAPK?) {
            TODO("Not yet implemented")
        }

        override fun onReturnCAPKLocation(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun onReturnUpdateCAPKResult(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onReturnRemoveCAPKResult(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onReturnEmvReportList(p0: Hashtable<String, String>?) {
            TODO("Not yet implemented")
        }

        override fun onReturnEmvReport(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun onReturnDisableAccountSelectionResult(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onReturnDisableInputAmountResult(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onReturnDisableBluetoothResult(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onReturnPhoneNumber(p0: BBDeviceController.PhoneEntryResult?, p1: String?) {
            TODO("Not yet implemented")
        }

        override fun onReturnEmvCardDataResult(p0: Boolean, p1: String?) {
            TODO("Not yet implemented")
        }

        override fun onReturnEmvCardNumber(p0: Boolean, p1: String?) {
            TODO("Not yet implemented")
        }

        override fun onReturnEncryptPinResult(p0: Boolean, p1: Hashtable<String, String>?) {
            TODO("Not yet implemented")
        }

        override fun onReturnEncryptDataResult(p0: Boolean, p1: Hashtable<String, String>?) {
            TODO("Not yet implemented")
        }

        override fun onReturnInjectSessionKeyResult(p0: Boolean, p1: Hashtable<String, String>?) {
            TODO("Not yet implemented")
        }

        override fun onReturnPowerOnIccResult(p0: Boolean, p1: String?, p2: String?, p3: Int) {
            TODO("Not yet implemented")
        }

        override fun onReturnPowerOffIccResult(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onReturnApduResult(p0: Boolean, p1: Hashtable<String, Any>?) {
            TODO("Not yet implemented")
        }

        override fun onRequestSelectApplication(p0: ArrayList<String>?) {
            TODO("Not yet implemented")
        }

        override fun onRequestSelectAccountType() {
            TODO("Not yet implemented")
        }

        override fun onRequestSetAmount() {
            TODO("Not yet implemented")
        }

        override fun onRequestOtherAmount(p0: BBDeviceController.AmountInputType?) {
            TODO("Not yet implemented")
        }

        override fun onRequestPinEntry(p0: BBDeviceController.PinEntrySource?) {
            TODO("Not yet implemented")
        }

        override fun onRequestManualPanEntry(p0: BBDeviceController.ManualPanEntryType?) {
            TODO("Not yet implemented")
        }

        override fun onReturnSetPinPadButtonsResult(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onReturnSetPinPadOrientationResult(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onReturnAccessiblePINPadTouchEvent(p0: BBDeviceController.PinPadTouchEvent?) {
            TODO("Not yet implemented")
        }

        override fun onReturnUpdateDisplaySettingsProgress(p0: Double) {
            TODO("Not yet implemented")
        }

        override fun onReturnUpdateDisplaySettingsResult(p0: Boolean, p1: String?) {
            TODO("Not yet implemented")
        }

        override fun onRequestOnlineProcess(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun onRequestTerminalTime() {
            TODO("Not yet implemented")
        }

        override fun onRequestDisplayText(p0: BBDeviceController.DisplayText?, p1: String?) {
            TODO("Not yet implemented")
        }

        override fun onRequestDisplayAsterisk(p0: String?, p1: Int) {
            TODO("Not yet implemented")
        }

        override fun onRequestDisplayLEDIndicator(p0: BBDeviceController.ContactlessStatus?) {
            TODO("Not yet implemented")
        }

        override fun onRequestProduceAudioTone(p0: BBDeviceController.ContactlessStatusTone?) {
            TODO("Not yet implemented")
        }

        override fun onRequestClearDisplay() {
            TODO("Not yet implemented")
        }

        override fun onRequestFinalConfirm() {
            TODO("Not yet implemented")
        }

        override fun onRequestAmountConfirm(p0: Hashtable<String, String>?) {
            TODO("Not yet implemented")
        }

        override fun onRequestPrintData(p0: Int, p1: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onPrintDataCancelled() {
            TODO("Not yet implemented")
        }

        override fun onPrintDataEnd() {
            TODO("Not yet implemented")
        }

        override fun onBatteryLow(p0: BBDeviceController.BatteryStatus?) {
            TODO("Not yet implemented")
        }

        override fun onError(p0: BBDeviceController.Error?, p1: String?) {
            TODO("Not yet implemented")
        }

        override fun onSessionInitialized() {
            TODO("Not yet implemented")
        }

        override fun onSessionError(p0: BBDeviceController.SessionError?, p1: String?) {
            TODO("Not yet implemented")
        }

        override fun onReturnDebugLog(p0: Hashtable<String, Any>?) {
            TODO("Not yet implemented")
        }

        override fun onDeviceHere(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onPowerDown() {
            TODO("Not yet implemented")
        }

        override fun onPowerButtonPressed() {
            TODO("Not yet implemented")
        }

        override fun onPowerConnected(
            p0: BBDeviceController.PowerSource?,
            p1: BBDeviceController.BatteryStatus?
        ) {
            TODO("Not yet implemented")
        }

        override fun onPowerDisconnected(p0: BBDeviceController.PowerSource?) {
            TODO("Not yet implemented")
        }

        override fun onDeviceReset(p0: Boolean, p1: BBDeviceController.DeviceResetReason?) {
            TODO("Not yet implemented")
        }

        override fun onDeviceResetAlert(p0: Int) {
            TODO("Not yet implemented")
        }

        override fun onEnterStandbyMode() {
            TODO("Not yet implemented")
        }

        override fun onReturnWatchdogTimerReset() {
            TODO("Not yet implemented")
        }

        override fun onReturnNfcDataExchangeResult(p0: Boolean, p1: Hashtable<String, String>?) {
            TODO("Not yet implemented")
        }

        override fun onReturnNfcDetectCardResult(
            p0: BBDeviceController.NfcDetectCardResult?,
            p1: Hashtable<String, Any>?
        ) {
            TODO("Not yet implemented")
        }

        override fun onReturnControlLEDResult(p0: Boolean, p1: String?) {
            TODO("Not yet implemented")
        }

        override fun onReturnVasResult(
            p0: BBDeviceController.VASResult?,
            p1: Hashtable<String, Any>?
        ) {
            TODO("Not yet implemented")
        }

        override fun onRequestStartEmv() {
            TODO("Not yet implemented")
        }

        override fun onDeviceDisplayingPrompt() {
            TODO("Not yet implemented")
        }

        override fun onRequestKeypadResponse() {
            TODO("Not yet implemented")
        }

        override fun onReturnDisplayPromptResult(p0: BBDeviceController.DisplayPromptResult?) {
            TODO("Not yet implemented")
        }

        override fun onReturnFunctionKey(p0: BBDeviceController.FunctionKey?) {
            TODO("Not yet implemented")
        }

        override fun onHardwareFunctionalTestResult(p0: Int, p1: Int, p2: String?) {
            TODO("Not yet implemented")
        }

        override fun onBarcodeReaderConnected() {
            TODO("Not yet implemented")
        }

        override fun onBarcodeReaderDisconnected() {
            TODO("Not yet implemented")
        }

        override fun onReturnBarcode(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun onRequestVirtuCryptPEDIResponse(p0: Boolean, p1: Hashtable<String, String>?) {
            TODO("Not yet implemented")
        }

        override fun onReturnVirtuCryptPEDICommandResult(
            p0: Boolean,
            p1: Hashtable<String, String>?
        ) {
            TODO("Not yet implemented")
        }

        override fun onRequestVirtuCryptPEDKResponse(p0: Boolean, p1: Hashtable<String, String>?) {
            TODO("Not yet implemented")
        }

        override fun onReturnVirtuCryptPEDKCommandResult(
            p0: Boolean,
            p1: Hashtable<String, String>?
        ) {
            TODO("Not yet implemented")
        }
    }
    }





