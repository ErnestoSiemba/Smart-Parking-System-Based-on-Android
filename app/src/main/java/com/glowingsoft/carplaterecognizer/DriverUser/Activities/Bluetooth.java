package com.glowingsoft.carplaterecognizer.DriverUser.Activities;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.glowingsoft.carplaterecognizer.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Bluetooth extends AppCompatActivity {

    ListView listView;
    ImageView backImageView, settingsImageView, toastIcon;
    TextView messageText, connectionStatusText;
    Button checkoutButton;
    private BluetoothDevice desiredBluetoothDevice;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> deviceListAdapter;
    private ArrayList<String> bondedDevicesList;
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_ACCESS_BLUETOOTH_PERMISSION = 2;
    private boolean isConnected = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        initComponents();
        attachListeners();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            showToast("Bluetooth permission denied");
        } else {
//            updateBluetoothStatus();

        }
    }

    private void updateBluetoothStatus() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            showToast("Bluetooth permission denied");
        } else {
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            if (bondedDevices != null && bondedDevices.size() > 0) {
                for (BluetoothDevice device : bondedDevices) {
                    String deviceAddress = device.getAddress();
                    connectionStatusText.setText("Connected To: " + device.getName());
                    String desiredDeviceAddress = "41:42:87:63:C3:CB";
                    checkoutButton.setOnClickListener(view-> {
                        if (deviceAddress.equals(desiredDeviceAddress)) {
                            showToast("Connected to the desired bluetooth device. Checkout Success");
                        } else {
                            showToast("Desired bluetooth device not connected!. Checkout Failed");
                        }
                    });

                }
            } else {
                showToast("No bonded devices found!");
            }
        }
    }

    private BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    handleBluetoothStateChanged(state);
                } else if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    handleBluetoothDeviceConnected(device);
                } else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    handleBluetoothDeviceDisconnected(device);
                }
            }
        }
    };

    private void registerBluetoothStateReceiver() {
        // Register the bluetooth state change and connection status receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(bluetoothStateReceiver, filter);
    }

    private void handleBluetoothStateChanged(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_OFF:
                connectionStatusText.setVisibility(View.VISIBLE);
                connectionStatusText.setText("Bluetooth OFF");
                break;

            case BluetoothAdapter.STATE_TURNING_ON:
                connectionStatusText.setVisibility(View.VISIBLE);
                connectionStatusText.setText("Turning Bluetooth ON");
                break;

            case BluetoothAdapter.STATE_ON:
                connectionStatusText.setVisibility(View.VISIBLE);
                connectionStatusText.setText("Bluetooth ON");
                break;


            case BluetoothAdapter.STATE_TURNING_OFF:
                connectionStatusText.setVisibility(View.VISIBLE);
                connectionStatusText.setText("Turning Bluetooth OFF");
                break;
        }
    }

    private void handleBluetoothDeviceConnected(BluetoothDevice device) {
        connectionStatusText.setVisibility(View.VISIBLE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            showToast("Bluetooth Permission Denied");
            return;
        } else {
            connectionStatusText.setText("Connected To: " + device.getName());
            String deviceAddress = device.getAddress();
            String desiredDeviceAddress = "41:42:87:63:C3:CB";
            checkoutButton.setOnClickListener(view-> {
                if (deviceAddress.equals(desiredDeviceAddress)) {
                    showToast("Connected to the desired bluetooth device. Checkout Success");
                } else {
                    showToast("Desired bluetooth device not connected!. Checkout Failed");
                }
            });
        }
    }

    private void handleBluetoothDeviceDisconnected(BluetoothDevice device) {
        connectionStatusText.setVisibility(View.VISIBLE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            showToast("Bluetooth Permission Denied");
            return;
        } else {
            connectionStatusText.setText("Disconnected From: " + device.getName());
        }
    }

    private void initComponents() {
        backImageView = findViewById(R.id.back_button);
        listView = findViewById(R.id.list_view);
        settingsImageView = findViewById(R.id.settings_button);
        settingsImageView.setVisibility(View.GONE);
        checkoutButton = findViewById(R.id.checkout_button);

        connectionStatusText = findViewById(R.id.connection_status_text);


        bondedDevicesList = new ArrayList<>();
        deviceListAdapter = new ArrayAdapter<>(Bluetooth.this, R.layout.list_item_device, R.id.device_name_text, bondedDevicesList);
        listView.setAdapter(deviceListAdapter);


        // get the default bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // check if the device supports bluetooth
        if (bluetoothAdapter == null) {
            showToast("Bluetooth is not supported on this device.");
            return;
        } else {
            // check bluetooth permissions
            checkBluetoothPermissions();
        }
    }



    private void checkBluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            // Bluetooth permissions are granted
            setupBluetooth();

            // Register the bluetooth receiver
            registerBluetoothStateReceiver();
        } else {
            // Request Bluetooth permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_ACCESS_BLUETOOTH_PERMISSION);
        }
    }

    private void setupBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            // Request to enable Bluetooth
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            // Bluetooth is enabled, show bonded devices
            showBondedDevices();
        }
    }

    private void showBondedDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // Bluetooth connect permission denied, show a message to the user
            showToast("Bluetooth enable permission denied");
        } else {
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            if (bondedDevices != null && bondedDevices.size() > 0) {
                for (BluetoothDevice device : bondedDevices) {
                    String deviceName = device.getName();
                    String deviceAddress = device.getAddress();
                    bondedDevicesList.add(deviceName + "\n" + deviceAddress);
                }
                deviceListAdapter.notifyDataSetChanged();
            } else {
                showToast("No bonded devices found!");
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ACCESS_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Bluetooth permissions are granted
                setupBluetooth();
            } else {
                showToast("Bluetooth permissions are required to view bonded devices.");
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                // Bluetooth is enabled
                showBondedDevices();
            } else {
                showToast("Bluetooth needs to be enabled to view bonded devices");
                finish();
            }
        }
    }




    private void attachListeners() {
        backImageView.setOnClickListener(view-> {
            onBackPressed();
        });

        checkoutButton.setOnClickListener(view-> {

        });
    }

    private void showToast(String message) {
        // inflate the custom toast layout
        View toastView = getLayoutInflater().inflate(R.layout.custom_toast_layout, null);

        // set the message text
        messageText = toastView.findViewById(R.id.toast_text);
        messageText.setText(message);

        // set the app icon
        toastIcon = toastView.findViewById(R.id.toast_icon);
        toastIcon.setImageResource(R.drawable.app_logo);

        // create the toast and set its custom layout
        Toast toast = new Toast(Bluetooth.this);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(toastView);

        // set the toast position
        toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 30);

        // show the toast
        toast.show();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}