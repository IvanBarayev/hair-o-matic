package net.mabboud.hair_o_matic.audio_com;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import net.mabboud.hair_o_matic.DeviceCom;

public class AudioDeviceCom implements DeviceCom {
    public static final int VOLTAGE_DATA_ID = 1;
    public static final int RESISTENCE_DATA_ID = 2;
    public static final int CURRENT_DATA_ID = 3;
    public static final int ACTIVE_TIME_DATA_ID = 4;

    public void incrementCurrent() {
        send("11111".getBytes());
    }

    public void decrementCurrent() {
        send("11111".getBytes());
    }

    public void send(byte[] bytes_pkg) {
        int bufsize = AudioTrack.getMinBufferSize(8000,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        AudioTrack trackplayer = new AudioTrack(AudioManager.STREAM_MUSIC,
                8000, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufsize,
                AudioTrack.MODE_STREAM);

        trackplayer.play();
        trackplayer.write(bytes_pkg, 0, bytes_pkg.length);
    }
}
