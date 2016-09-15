package net.mabboud.hair_o_matic.audio_com;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static net.mabboud.hair_o_matic.audio_com.AudioDeviceCom.CURRENT_DATA_ID;
import static net.mabboud.hair_o_matic.audio_com.AudioDeviceCom.VOLTAGE_DATA_ID;
import static net.mabboud.hair_o_matic.audio_com.ModemSignalProcessor.*;

public class ModemSignalProcessorTest {
    private HashMap<Integer, Integer> receivedData;
    private PacketReceivedListener listener;
    private ModemSignalProcessor processor;

    @Before
    public void setup() {
        receivedData = new HashMap<>();
        listener = new PacketReceivedListener() {
            public void onDataReceived(int data, int id) {
                receivedData.put(id, data);
            }
        };

        processor = new ModemSignalProcessor();
        processor.setPacketListener(listener);
    }

    @Test
    public void givenPacket_withOneBlock_thenProcessorReceivesBlock() throws Exception {
        Map<Integer, Integer> dataBlocks = new HashMap<>();
        dataBlocks.put(CURRENT_DATA_ID, 600);

        List<Integer> tones = createPacket(dataBlocks);
        for (Integer toneOn : tones)
            processor.toneDetected(toneOn);

        int receivedValue = receivedData.get(CURRENT_DATA_ID);
        Assert.assertEquals(600, receivedValue);
    }

    @Test
    public void givenPacket_withTwoBlocks_thenProcessorReceivesBlocks() throws Exception {
        Map<Integer, Integer> dataBlocks = new HashMap<>();
        dataBlocks.put(CURRENT_DATA_ID, 600);
        dataBlocks.put(VOLTAGE_DATA_ID, 5);

        List<Integer> tones = createPacket(dataBlocks);
        for (Integer toneOn : tones)
            processor.toneDetected(toneOn);

        Assert.assertEquals(600, (int) receivedData.get(CURRENT_DATA_ID));
        Assert.assertEquals(5, (int) receivedData.get(VOLTAGE_DATA_ID));
    }

    @Test
    public void givenPacket_withNoBlocks_thenNoExceptionThrown() throws Exception {
        Map<Integer, Integer> dataBlocks = new HashMap<>();

        List<Integer> tones = createPacket(dataBlocks);
        for (Integer toneOn : tones)
            processor.toneDetected(toneOn);
    }

    @Test
    public void givenPacket_withWrongSizeBlock_thenBlockNotRead_AndNoExceptionThrown() throws Exception {
        List<Integer> tones = new LinkedList<>();
        tones.add(PACKET_START_TONE);
        tones.add(OFF_TONE);
        tones.add(ON_TONE);
        tones.add(PACKET_END_TONE);

        for (Integer toneOn : tones)
            processor.toneDetected(toneOn);

        Assert.assertEquals(0, receivedData.size());
    }

    List<Integer> createPacket(Map<Integer, Integer> dataBlocks) {
        List<Integer> packetTones = new LinkedList<>();
        packetTones.add(PACKET_START_TONE);

        for (Map.Entry<Integer, Integer> entryOn : dataBlocks.entrySet()) {
            for (char c : idToBinary(entryOn.getKey())) {
                packetTones.add(c == '1' ? ON_TONE : OFF_TONE);
                packetTones.add(0);
            }

            for (char c : valueToBinary(entryOn.getValue())) {
                packetTones.add(c == '1' ? ON_TONE : OFF_TONE);
                packetTones.add(0);
            }
        }

        packetTones.add(PACKET_END_TONE);
        return packetTones;
    }

    private char[] idToBinary(Integer id) {
        String idBinary = Integer.toBinaryString(id);
        if (idBinary.length() < 4)
            idBinary = StringUtils.repeat("0", 4 - idBinary.length() + 1) + idBinary;

        idBinary = StringUtils.reverse(idBinary);
        idBinary = idBinary.substring(0, 3);
        return idBinary.toCharArray();
    }

    private char[] valueToBinary(Integer value) {
        String valueBinary = Integer.toBinaryString(value);

        if (valueBinary.length() < 16)
            valueBinary = StringUtils.repeat("0", 16 - valueBinary.length() + 1) + valueBinary;

        valueBinary = StringUtils.reverse(valueBinary);
        return valueBinary.toCharArray();
    }
}
