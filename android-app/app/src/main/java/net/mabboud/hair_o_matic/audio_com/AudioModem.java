package net.mabboud.hair_o_matic.audio_com;

import android.annotation.TargetApi;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.widget.ScrollView;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import bg.cytec.android.fskmodem.FSKConfig;
import bg.cytec.android.fskmodem.FSKDecoder;
import bg.cytec.android.fskmodem.FSKEncoder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static net.mabboud.hair_o_matic.audio_com.ModemSignalProcessor.*;

public class AudioModem {
    private static final String LOG_TAG = "audio modem";

    private final Activity activity;
    private final ModemSignalProcessor signalProcessor;

    private Thread recordThread;
    private AudioDispatcher dispatcher;
    private int sampleRate;
    private int bufferRate = 4024;

    protected FSKConfig mConfig;
    protected FSKEncoder mEncoder;
    protected FSKDecoder mDecoder;
    protected int mBufferSize = 0;
    protected AudioTrack mAudioTrack;
    protected AudioRecord mRecorder;
    private PacketReceivedListener recievedListener;

    public AudioModem(Activity activity) {
        this.activity = activity;
        signalProcessor = new ModemSignalProcessor();
    }


    public void listen() {

        /// INIT FSK CONFIG
        try {
            mConfig = new FSKConfig(FSKConfig.SAMPLE_RATE_44100, FSKConfig.PCM_16BIT, FSKConfig.CHANNELS_MONO, FSKConfig.SOFT_MODEM_MODE_3, FSKConfig.THRESHOLD_20P);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        /// INIT FSK DECODER

        mDecoder = new FSKDecoder(mConfig, new FSKDecoder.FSKDecoderCallback() {

            @TargetApi(Build.VERSION_CODES.KITKAT)
            @Override
            public void decoded(byte[] newData) {

                final String text = new String(newData, StandardCharsets.US_ASCII);

                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        recievedListener.onTextReceived(text);
                    }
                });
            }
        });


        ///

        //make sure that the settings of the recorder match the settings of the decoder
        //most devices cant record anything but 44100 samples in 16bit PCM format...
        mBufferSize = AudioRecord.getMinBufferSize(FSKConfig.SAMPLE_RATE_44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        //scale up the buffer... reading larger amounts of data
        //minimizes the chance of missing data because of thread priority
        mBufferSize *= 10;

        //again, make sure the recorder settings match the decoder settings
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, FSKConfig.SAMPLE_RATE_44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, mBufferSize);

        if (mRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
            mRecorder.startRecording();

            //start a thread to read the audio data
            Thread thread = new Thread(mRecordFeed);
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.start();
        }
        else {
            Log.i("FSKDecoder", "Please check the recorder settings, something is wrong!");
        }
    }
    protected Runnable mRecordFeed = new Runnable() {

        @Override
        public void run() {

            while (mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {

                short[] data = new short[mBufferSize/2]; //the buffer size is in bytes

                // gets the audio output from microphone to short array samples
                mRecorder.read(data, 0, mBufferSize/2);

                mDecoder.appendSignal(data);
            }
        }
    };
    public void setSampleRate() {
        sampleRate = SampleRateDetector.getMaxSupportedSampleRate();
    }

    public void setPacketListener(PacketReceivedListener packetListener) {
        recievedListener = packetListener;
    }

    public void stop() {
        mDecoder.stop();
//        mEncoder.stop();

        if (mRecorder != null && mRecorder.getState() == AudioRecord.STATE_INITIALIZED)
        {
            mRecorder.stop();
            mRecorder.release();
        }

//        if (mAudioTrack != null && mAudioTrack.getPlayState() == AudioTrack.STATE_INITIALIZED)
//        {
////            mAudioTrack.stop();
////            mAudioTrack.release();
//        }
    }

}
