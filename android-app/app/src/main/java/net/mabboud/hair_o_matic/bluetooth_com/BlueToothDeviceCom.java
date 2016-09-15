package net.mabboud.hair_o_matic.bluetooth_com;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import net.mabboud.hair_o_matic.DeviceCom;
import net.mabboud.hair_o_matic.DeviceStatus;
import net.mabboud.hair_o_matic.HomeActivity;
import net.mabboud.hair_o_matic.R;

import java.io.IOException;

public class BlueToothDeviceCom extends DeviceCom {
    public void incrementCurrent() {
    }

    public void decrementCurrent() {
    }

    public void initialize(Activity activity) {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            errorStatusUpdate(activity.getString(R.string.blue_tooth_unsupported));
            return;
        }


        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, HomeActivity.REQUEST_ENABLE_BT);
        }
    }

    protected void errorStatusUpdate(String message) {
        DeviceStatus status = new DeviceStatus();
        status.message = message;
        statusListener.statusUpdated(status);
    }
}
