package net.mabboud.hair_o_matic.audio_com;

import android.app.Activity;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

import java.util.ArrayList;

import static net.mabboud.hair_o_matic.audio_com.ModemSignalProcessor.*;

public class AudioModem {
    private static final String LOG_TAG = "audio modem";

    private final Activity activity;
    private final ModemSignalProcessor signalProcessor;

    private Thread recordThread;
    private AudioDispatcher dispatcher;
    private int sampleRate;
    private int bufferRate = 4096;

    public AudioModem(Activity activity) {
        this.activity = activity;
        signalProcessor = new ModemSignalProcessor();
    }

    public void listen() {
        while (dispatcher == null) {
            try {
                dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferRate, 0);
            } catch (Exception exception) {
                ArrayList<Integer> testSampleRates = SampleRateDetector.getAllSupportedSampleRates();
                for (Integer testSampleRate : testSampleRates) {
                    try {
                        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(testSampleRate, bufferRate, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        PitchDetectionHandler pdh = new PitchDetectionHandler() {
            public void handlePitch(PitchDetectionResult result, AudioEvent e) {
                final float pitchInHz = result.getPitch();
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        signalProcessor.toneDetected(pitchInHz);
                    }
                });
            }
        };

        AudioProcessor p = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, sampleRate, bufferRate, pdh);

        dispatcher.addAudioProcessor(p);
        recordThread = new Thread(dispatcher, "Audio Dispatcher");
        recordThread.start();
    }

    public void setSampleRate() {
        this.sampleRate = SampleRateDetector.getMaxSupportedSampleRate();
    }

    public void setPacketListener(PacketReceivedListener packetListener) {
        signalProcessor.setPacketListener(packetListener);
    }
}
