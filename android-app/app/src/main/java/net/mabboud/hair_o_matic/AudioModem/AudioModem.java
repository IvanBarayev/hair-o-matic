package net.mabboud.hair_o_matic.AudioModem;

import android.app.Activity;
import android.util.Log;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class AudioModem {
    private static final String LOG_TAG = "audio modem";

    private final Activity activity;
    private final ModemSignalProcessor signalProcessor;
    private PacketReceivedListener packetListener;

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

    private class ModemSignalProcessor {
        private static final int PACKET_START_TONE = 1500;
        private static final int PACKET_END_TONE = 1000;
        private static final int OFF_TONE = 100;
        private static final int ON_TONE = 500;

        private static final int signalEpsilon = 30;

        List<Boolean> signalBits = new LinkedList<>();
        float lastTone = 0;

        boolean startToneRecieved;
        void toneDetected(float hz) {
            if (isSameAsLastTone(hz))
                return;

            if (isPacketStartTone(hz)) {
                startToneRecieved = true;
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
                startToneRecieved = false;
            } else if (isOnTone(hz))
                signalBits.add(true);
            else if (isOffTone(hz))
                signalBits.add(false);

            lastTone = hz;
        }

        private boolean isSameAsLastTone(float hz) {
            return hz < (signalEpsilon + lastTone) && hz > (lastTone - signalEpsilon);
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
            return hz < (signalEpsilon + toneSameAs) && hz > (toneSameAs- signalEpsilon);
        }

        private int booleansToInt(Boolean[] arr) {
            int n = 0;
            for (Boolean b : arr)
                n = (n << 1) | (b ? 1 : 0);
            return n;
        }
    }

    public interface PacketReceivedListener {
        void onReceived(int packet);
    }
}
