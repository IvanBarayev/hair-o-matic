package net.mabboud.hair_o_matic.bluetooth_com;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import net.mabboud.hair_o_matic.DeviceCom;
import net.mabboud.hair_o_matic.DeviceStatus;
import net.mabboud.hair_o_matic.HomeActivity;
import net.mabboud.hair_o_matic.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public class BlueToothDeviceCom extends DeviceCom {
    final static String BT_DEVICE_NAME = "HairOmatic";
    final static String INC_CURRENT_COMMAND = "[increase_current]";
    final static String DEC_CURRENT_COMMAND = "[decrease_current]";

    private static final String LOG_TAG = "BlueTooth Com";
    private static final UUID MY_UUID = UUID.fromString("1247e4e8-7ae4-11e6-8b77-86f30ca893d3");

    private BluetoothAdapter bluetoothAdapter;
    private BroadcastReceiver receiver;
    private Activity activity;

    public void incrementCurrent() {
    }

    public void decrementCurrent() {
    }

    public void initialize(Activity activity) {
        this.activity = activity;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            errorStatusUpdate(activity.getString(R.string.blue_tooth_unsupported_error_message));
            return;
        }


        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, HomeActivity.REQUEST_ENABLE_BT);
        }
    }

    protected void errorStatusUpdate(String message) {
        DeviceStatus status = new DeviceStatus();
        status.message = message;
        statusListener.statusUpdated(status);
    }

    protected BluetoothDevice findPairedBlueToothDevice() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                if (isElectroDevice(device))
                    return device;
            }
        }

        return null;
    }

    protected BluetoothDevice discoverDevice() {
        // Create a BroadcastReceiver for ACTION_FOUND
        receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (isElectroDevice(device)) {
                        deviceFound(device);
                    }
                }
            }

            private void deviceFound(BluetoothDevice device) {

            }
        };

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        activity.registerReceiver(receiver, filter); // Don't forget to unregister during onDestroy

        return null;
    }

    protected void setPairingPin(BluetoothDevice device) {
        byte[] pinBytes = "1234".getBytes();
        try {
            Method m = device.getClass().getMethod("setPin", byte[].class);
            m.invoke(device, pinBytes);
            Log.d(LOG_TAG, "Success to add the PIN.");
            try {
                device.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(device, true);
                Log.d(LOG_TAG, "Success to setPairingConfirmation.");
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isElectroDevice(BluetoothDevice device) {
        return device.getName().trim().equalsIgnoreCase(BT_DEVICE_NAME);
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }


        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        ConnectedThread thread = new ConnectedThread(socket);
        thread.start();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytesRead; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    bytesRead = mmInStream.read(buffer);
                    byte[] rawData = Arrays.copyOfRange(buffer, 0, bytesRead);
                    String data = new String (rawData);

                    DeviceStatus status = new DeviceStatus();
                    status.message = data;

                    statusListener.statusUpdated(status);
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}

