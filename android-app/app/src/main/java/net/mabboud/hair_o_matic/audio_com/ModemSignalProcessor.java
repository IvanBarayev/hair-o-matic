package net.mabboud.hair_o_matic.audio_com;

import android.util.Log;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

class ModemSignalProcessor {
    private static final String LOG_TAG = "audio modem";

    private static final int PACKET_START_TONE = 1500;
    private static final int PACKET_END_TONE = 1000;
    private static final int OFF_TONE = 100;
    private static final int ON_TONE = 500;
    private static final int SIGNAL_EPSILON = 30;

    private PacketReceivedListener packetListener;
    private List<Boolean> signalBits = new LinkedList<>();
    private float lastTone = 0;
    boolean startToneReceived;

    void toneDetected(float hz) {
        if (isSameAsLastTone(hz))
            return;

        if (isPacketStartTone(hz)) {
            startToneReceived = true;
            signalBits.clear();
            Log.w(LOG_TAG, String.format("Packet Start Tone: %s", hz));
        } else if (isPacketEndTone(hz)) {
            Collections.reverse(signalBits);
            Boolean[] arr = signalBits.toArray(new Boolean[signalBits.size()]);
            String binary = "";
            for (Boolean b : arr)
                binary += b ? "1" : "0";

            int number = booleansToInt(arr);
            Log.w(LOG_TAG, String.format("Packet Received!!: %s", number));
            Log.w(LOG_TAG, String.format("Binary: %s", binary));

            if (packetListener != null)
                packetListener.onReceived(number);

            signalBits.clear();
            startToneReceived = false;
        } else if (isOnTone(hz))
            signalBits.add(true);
        else if (isOffTone(hz))
            signalBits.add(false);

        lastTone = hz;
    }

    private boolean isSameAsLastTone(float hz) {
        return hz < (SIGNAL_EPSILON + lastTone) && hz > (lastTone - SIGNAL_EPSILON);
    }

    private boolean isPacketEndTone(float hz) {
        return isSameAsTone(hz, PACKET_END_TONE);
    }

    private boolean isPacketStartTone(float hz) {
        return isSameAsTone(hz, PACKET_START_TONE);
    }

    private boolean isOffTone(float hz) {
        return isSameAsTone(hz, OFF_TONE);
    }

    private boolean isOnTone(float hz) {
        return isSameAsTone(hz, ON_TONE);
    }

    private boolean isSameAsTone(float hz, float toneSameAs) {
        return hz < (SIGNAL_EPSILON + toneSameAs) && hz > (toneSameAs - SIGNAL_EPSILON);
    }

    private int booleansToInt(Boolean[] arr) {
        int n = 0;
        for (Boolean b : arr)
            n = (n << 1) | (b ? 1 : 0);
        return n;
    }

    public void setPacketListener(PacketReceivedListener packetListener) {
        this.packetListener = packetListener;
    }

    public interface PacketReceivedListener {
        void onReceived(int packet);
    }
}
