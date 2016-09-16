package net.mabboud.hair_o_matic;

import android.app.Activity;

public abstract class DeviceCom {
    protected DeviceStatusListener statusListener = new DeviceStatusListener() {
        public void statusUpdated(DeviceStatus status) {
            // empty impl to avoid nulls
        }
    };

    public abstract void incrementCurrent();

    public abstract void decrementCurrent();

    public void close() {
    }

    public void setStatusListener(DeviceStatusListener statusListener) {
        this.statusListener = statusListener;
    }

    public void initialize(Activity activity) {
    }

    public void setupComplete() {
    }

    public void reconnect() {
    }

    public interface DeviceStatusListener {
        public void statusUpdated(DeviceStatus status);
    }
}
