package net.mabboud.hair_o_matic.audio_com;

import android.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Very basic modem tone processor.
 * Packet format
 * - packet start tone
 * - data blocks[]
 * - 4bit data id
 * - 16bit value
 * - packet end tone
 */
/**
 * note audio com is unfinished and has been abandoned in favor of blue tooth. tried using SoftModem lib but couldn't get it to work properly
 * audio com is currently using a dinky audio modem implementation I  made that is only capable of 40-60 baud
 */
public class ModemSignalProcessor {
    public interface PacketReceivedListener {
        void onDataReceived(int data, int id);
    }

    private static Logger log = LoggerFactory.getLogger(ModemSignalProcessor.class);

    static final int PACKET_START_TONE = 1500;
    static final int PACKET_END_TONE = 1000;
    static final int OFF_TONE = 100;
    static final int ON_TONE = 500;
    static final int SIGNAL_EPSILON = 30;

    private PacketReceivedListener packetListener;
    private List<Boolean> signalBits = new LinkedList<>();
    private float lastTone = 0;
    boolean startToneReceived;

    public ModemSignalProcessor() {
        packetListener = new PacketReceivedListener() {
            public void onDataReceived(int data, int id) {
                // empty impl for non null access
            }
        };
    }

    void toneDetected(float hz) {
        if (isSameAsLastTone(hz))
            return;

        if (isPacketStartTone(hz)) {
            startToneReceived = true;
            signalBits.clear();
            log.warn(String.format("Packet Start Tone: %s", hz));
        } else if (isPacketEndTone(hz)) {
            readPacket();

            signalBits.clear();
            startToneReceived = false;
        } else if (isOnTone(hz))
            signalBits.add(true);
        else if (isOffTone(hz))
            signalBits.add(false);

        lastTone = hz;
    }

    private void readPacket() {
        while (signalBits.size() >= 20) {
            List<Boolean> blockBits = signalBits.subList(0, 19);

            Boolean[] dataIdBinary = normalizeBits(blockBits.subList(0, 3));
            int dataId = booleansToInt(dataIdBinary);

            Boolean[] valueBinary = normalizeBits(blockBits.subList(3, 19));
            int value = booleansToInt(valueBinary);

            log.warn(String.format("Packet Received, ID:%s, Value:%s", dataId, value));
            packetListener.onDataReceived(value, dataId);

            signalBits = signalBits.subList(20, signalBits.size());
        }
    }

    private Boolean[] normalizeBits(List<Boolean> bits) {
        Collections.reverse(bits);
        return bits.toArray(new Boolean[bits.size()]);
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

    private String binaryToString(Boolean[] arr) {
        String binary = "";
        for (Boolean b : arr)
            binary += b ? "1" : "0";
        return binary;
    }
}
