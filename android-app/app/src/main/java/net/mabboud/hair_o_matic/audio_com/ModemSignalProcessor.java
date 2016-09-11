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
public class ModemSignalProcessor {
    public interface PacketReceivedListener {
        void onDataReceived(int data, int id);
        void onTextReceived(String text);
    }

    private static Logger log = LoggerFactory.getLogger(ModemSignalProcessor.class);

    static final int PACKET_START_TONE = 2000;
    static final int PACKET_END_TONE = 100;
    static final int SIGNAL_EPSILON = 40;

    private PacketReceivedListener packetListener;
    private List<Integer> signalWords = new LinkedList<>();
    private float lastTone = 0;
    boolean startToneReceived;

    public ModemSignalProcessor() {
    }

    void toneDetected(float hz) {
        Log.i("hi", String.format("Hz: %s", hz));

        if (isSameAsLastTone(hz))
            return;

        if (isPacketStartTone(hz)) {
            startToneReceived = true;
            signalWords.clear();

            log.warn(String.format("Packet Start Tone: %s", hz));
        } else if (isPacketEndTone(hz)) {
            if (startToneReceived)
                readPacket();
            else
                log.warn("Packet Dropped!! No start tone received");

            signalWords.clear();
            startToneReceived = false;
        } else if (hz > 200 && hz < 1800) {
            int word = Math.round((hz - 200) / 100);
            signalWords.add(word);
        }

        lastTone = hz;
    }

    private void readPacket() {
        if (signalWords.size() % 5 != 0)
            log.warn(String.format("Packet Dropped Incorrect Length!! L: %d", signalWords.size()));


        while (signalWords.size() >= 5) {
            List<Integer> blockBits = signalWords.subList(0, 5);

            Integer[] dataIdBinary = normalizeBits(blockBits.subList(0, 1));
            int dataId = dataIdBinary[0];

           Integer[] valueBinary = normalizeBits(blockBits.subList(1, 5));
            int value = booleansToInt(valueBinary);

            String valueBin = "";
            for (Integer b : signalWords)
                valueBin += b + ",";
            log.warn(String.format("Packet Received, ID:%s, Value:%s, Binary:%s", dataId, value, valueBin));


            packetListener.onDataReceived(value, dataId);

            signalWords = signalWords.subList(5, signalWords.size());
        }
    }

    private Integer[] normalizeBits(List<Integer> bits) {
//        Collections.reverse(bits);
        return bits.toArray(new Integer[bits.size()]);
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

    private boolean isSameAsTone(float hz, float toneSameAs) {
        return hz < (SIGNAL_EPSILON + toneSameAs) && hz > (toneSameAs - SIGNAL_EPSILON);
    }

    private int booleansToInt(Integer[] arr) {
        return  arr[0] & 0xFF |
                (arr[1] & 0xFF) << 4 |
                (arr[2] & 0xFF) << 8 |
                (arr[3] & 0xFF) << 12;
    }

    public void setPacketListener(PacketReceivedListener packetListener) {
        this.packetListener = packetListener;
    }

}
