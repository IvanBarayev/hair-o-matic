package net.mabboud.hair_o_matic.audio_com;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import net.mabboud.hair_o_matic.DeviceCom;
import net.mabboud.hair_o_matic.DeviceStatus;
import net.mabboud.hair_o_matic.HomeActivity;

/**
 * note audio com is unfinished and has been abandoned in favor of blue tooth. tried using SoftModem lib but couldn't get it to work properly
 * audio com is currently using a dinky audio modem implementation I  made that is only capable of 40-60 baud
 */
public class AudioDeviceCom extends DeviceCom implements ModemSignalProcessor.PacketReceivedListener {
    public static final int VOLTAGE_DATA_ID = 1;
    public static final int RESISTENCE_DATA_ID = 2;
    public static final int CURRENT_DATA_ID = 3;
    public static final int ACTIVE_TIME_DATA_ID = 4;
    private final AudioModem modem;

    public AudioDeviceCom(Activity activity) {
        modem = new AudioModem(activity);
        modem.setPacketListener(this);
        modem.listen();
    }

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

    public void onDataReceived(int data, int id) {
        DeviceStatus status = new DeviceStatus();
        status.message = data + " " + id;
        statusListener.statusUpdated(status);
    }
}
