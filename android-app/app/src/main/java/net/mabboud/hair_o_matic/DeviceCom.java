package net.mabboud.hair_o_matic;

public abstract class DeviceCom {
    protected DeviceStatusListener statusListener = new DeviceStatusListener() {
        public void statusUpdated(DeviceStatus status) {
            // empty impl to avoid nulls
        }
    };

    public abstract void incrementCurrent();
    public abstract void decrementCurrent();

    public void setStatusListener(DeviceStatusListener statusListener) {
        this.statusListener = statusListener;
    }

    public interface DeviceStatusListener {
        public void statusUpdated(DeviceStatus status);
    }
}
