package net.mabboud.hair_o_matic.audio_com;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ModemSignalProcessorTest {
    @Test
    public void givenPacket() throws Exception {
        ModemSignalProcessor processor = new ModemSignalProcessor(new AudioModem());
    }
}
