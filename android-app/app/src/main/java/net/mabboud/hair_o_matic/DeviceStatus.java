package net.mabboud.hair_o_matic;

import com.google.gson.Gson;

public class DeviceStatus {
    public int timer = 0;
    public int current = 0;
    public double voltage = 0;
    public double resistance = 0;
    public int lifetimeKills = 0;
    public int sessionKills = 0;

    public String message = "";

    public static DeviceStatus fromJson(String json) {
        return new Gson().fromJson(json, DeviceStatus.class);
    }
}
