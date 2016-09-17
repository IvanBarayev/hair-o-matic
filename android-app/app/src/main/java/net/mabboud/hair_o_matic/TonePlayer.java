package net.mabboud.hair_o_matic;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class TonePlayer {
    public static final int TONE_PAUSE_TIME = 5;
    private final double toneFreqInHz;
    private AudioTrack audioTrack;

    boolean isPlaying = false;

    public TonePlayer(double toneFreqInHz) {
        this.toneFreqInHz = toneFreqInHz;
    }

    public void play() {
        if (isPlaying)
            return;

        stop();

        isPlaying = true;
        asyncPlayTrack(toneFreqInHz);
    }

    public void stop() {
        isPlaying = false;
        if (audioTrack == null)
            return;

        tryStopPlayer();
    }

    private void asyncPlayTrack(final double toneFreqInHz) {
        new Thread(new Runnable() {
            public void run() {

                while (isPlaying) {
                    // will pause every x seconds so can know how long insertion has been without looking
                    // at the timer
                    playTone(toneFreqInHz, TONE_PAUSE_TIME);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void tryStopPlayer() {
        isPlaying = false;
        try {
            // pause() appears to be more snappy in audio cutoff than stop()
            audioTrack.stop();
            audioTrack.flush();
            audioTrack.release();
            audioTrack = null;
        } catch (IllegalStateException e) {
            // Calling from multiple threads exception, doesn't matter, so just eat it in the rare event it occurs.
            // AudioTrack appears fine for multiple thread usage otherwise.
        }
    }

    // below from http://stackoverflow.com/questions/2413426/playing-an-arbitrary-tone-with-android
    private void playTone(double freqInHz, double seconds) {
        int sampleRate = 8000;

        double dnumSamples = seconds * sampleRate;
        dnumSamples = Math.ceil(dnumSamples);
        int numSamples = (int) dnumSamples;
        double sample[] = new double[numSamples];
        byte generatedSnd[] = new byte[2 * numSamples];

        // Fill the sample array
        for (int i = 0; i < numSamples; ++i)
            sample[i] = Math.sin(freqInHz * 2 * Math.PI * i / (sampleRate));

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalized.
        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        int i = 0;

        // Amplitude ramp as a percent of sample count
        int ramp = numSamples / 20;

        // Ramp amplitude up (to avoid clicks)
        for (i = 0; i < ramp; ++i) {
            double dVal = sample[i];
            // Ramp up to maximum
            final short val = (short) ((dVal * 32767 * i / ramp));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }


        // Max amplitude for most of the samples
        for (i = i; i < numSamples - ramp; ++i) {
            double dVal = sample[i];
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        // Ramp amplitude down
        for (i = i; i < numSamples; ++i) {
            double dVal = sample[i];
            // Ramp down to zero
            final short val = (short) ((dVal * 32767 * (numSamples - i) / ramp));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        try {
            int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize,
                    AudioTrack.MODE_STREAM);
            audioTrack.play();
            audioTrack.write(generatedSnd, 0, generatedSnd.length);
        } catch (Exception e) {
            Log.e("tone player", e.toString());
        }
        if (audioTrack != null) audioTrack.release();
    }

}
