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

public class BluetoothDeviceCom extends DeviceCom {
    private final static String BT_DEVICE_NAME = "Hair-o-matic";
    private static final String PAIRING_PIN = "1234";

    private final static String INC_CURRENT_COMMAND = "[increase_current]";
    private final static String DEC_CURRENT_COMMAND = "[decrease_current]";

    private static final String LOG_TAG = "Bluetooth Com";

    private BluetoothAdapter bluetoothAdapter;
    private BroadcastReceiver receiver;
    private Activity activity;
    private ConnectedThread connection;
    private ConnectThread connectWorker;

    public void incrementCurrent() {
        if (connection != null)
            connection.write(INC_CURRENT_COMMAND.getBytes());
        else
            errorStatusUpdate("Bluetooth connection not established yet");
    }

    public void decrementCurrent() {
        if (connection != null)
            connection.write(DEC_CURRENT_COMMAND.getBytes());
        else
            errorStatusUpdate("Bluetooth connection not established yet");
    }

    @Override
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
            return;
        }

        setupComplete();
    }

    public void setupComplete() {
        BluetoothDevice device = findPairedBlueToothDevice();
        if (device == null) {
            errorStatusUpdate("Could not find paired device.");
            return;
        }

        connectWorker = new ConnectThread(device);
        connectWorker.start();
    }

    public void reconnect() {
        close();
        setupComplete();
    }

    public void close() {
        if (connectWorker != null)
            connectWorker.cancel();

        if (connection != null)
            connection.cancel();
    }

    protected void errorStatusUpdate(String message) {
        DeviceStatus status = new DeviceStatus();
        status.message = message;
        statusListener.statusUpdated(status);
    }

    protected BluetoothDevice findPairedBlueToothDevice() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (isElectroDevice(device)) {
                    Log.w(LOG_TAG, "dname " + device.getName());
                    return device;
                }
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
        byte[] pinBytes = PAIRING_PIN.getBytes();
        try {
            Method m = device.getClass().getMethod("setPin", byte[].class);
            m.invoke(device, pinBytes);
            device.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(device, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isElectroDevice(BluetoothDevice device) {
        return device.getName().trim().equalsIgnoreCase(BT_DEVICE_NAME);
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        connection = new ConnectedThread(socket);
        connection.start();
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            this.device = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                UUID SERIAL_UUID = device.getUuids()[0].getUuid();
                tmp = device.createRfcommSocketToServiceRecord(SERIAL_UUID);
            } catch (IOException e) {
                Log.w(LOG_TAG, "error creating socket" + e);
            }
            socket = tmp;
        }

        public void run() {
            Log.d(LOG_TAG, "connecting");
            // Cancel discovery because it will slow down the connection
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                socket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    socket.close();
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }

                connectException.printStackTrace();
                Log.w(LOG_TAG, connectException.toString());
                Log.w(LOG_TAG, "connecting error");

                return;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(socket);
        }


        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        String data = "";

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    readJsonDataFromInput(buffer);
                } catch (IOException e) {
                    break;
                }
            }
        }

        private void readJsonDataFromInput(byte[] buffer) throws IOException {
            int bytesRead;
            bytesRead = mmInStream.read(buffer);
            byte[] rawData = Arrays.copyOfRange(buffer, 0, bytesRead);
            data += new String(rawData);

            if (!data.startsWith("{"))
                data = data.replaceAll("[^\\{]*\\{", "");

            // don't read data till we have a full json block
            if (dataHasJsonBlock(data))
                return;

            String jsonData = data;
            if (!data.endsWith("}"))
                jsonData = data.substring(0, data.indexOf('}') + 1);

            try {
                notifyDeviceStatusUpdated(jsonData);
            } catch (Exception ex) {
                data = "";
                Log.w(LOG_TAG, "Error corrupted json data. json: " + jsonData);
                return;
            }

            // reset data after reading json block
            data = data.substring(data.indexOf('}') + 1, data.length());
        }

        private void notifyDeviceStatusUpdated(String jsonData) {
            final DeviceStatus status = DeviceStatus.fromJson(jsonData);
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    statusListener.statusUpdated(status);
                }
            });
        }

        private boolean dataHasJsonBlock(String data) {
            return !data.contains("{") || !data.contains("}");
        }

        /* Call this from the main activity to send data to the remote device */
        void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
}

