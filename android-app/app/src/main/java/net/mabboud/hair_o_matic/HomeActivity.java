package net.mabboud.hair_o_matic;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import net.mabboud.android_tone_player.ContinuousBuzzer;
import net.mabboud.android_tone_player.OneTimeBuzzer;
import net.mabboud.android_tone_player.TonePlayer;
import net.mabboud.hair_o_matic.audio_com.AudioDeviceCom;
import net.mabboud.hair_o_matic.bluetooth_com.BluetoothDeviceCom;

import java.util.Locale;

public class HomeActivity extends AppCompatActivity implements DeviceCom.DeviceStatusListener {
    public final static int REQUEST_ENABLE_BT = 420;

    private static final String LOG_TAG = "home";
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 235;
    private static final int MY_PERMISSIONS_REQUEST_MODIFY_AUDIO_SETTINGS = 183;

    private DeviceCom deviceCom;
    private Locale locale = Locale.getDefault();
    private ContinuousBuzzer tonePlayer = new ContinuousBuzzer();
    private boolean buzzerEnabled;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initDeviceCom();

        tonePlayer.setPausePeriodSeconds(5);
        tonePlayer.setPauseTimeInMs(1000);

        Button addCurrentBtn = (Button) findViewById(R.id.plusCurrentButton);
        addCurrentBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                deviceCom.incrementCurrent();
            }
        });

        Button minusCurrentBtn = (Button) findViewById(R.id.minusCurrentButton);
        minusCurrentBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                deviceCom.decrementCurrent();
            }
        });

        Button reconnectBtn = (Button) findViewById(R.id.reconnectBtn);
        reconnectBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                deviceCom.reconnect();
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        loadSettings();
    }

    private void loadSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        buzzerEnabled = prefs.getBoolean(getString(R.string.key_enable_buzzer), true);

        int volume = prefs.getInt(getString(R.string.key_buzzer_volume), 50);
        tonePlayer.setVolume(volume);
    }

    public void initDeviceCom() {
        // deviceCom = createModemCom();
        deviceCom = createBluetoothCom();

        deviceCom.setStatusListener(this);
        deviceCom.initialize(this);
    }

    public DeviceCom createBluetoothCom() {
        BluetoothDeviceCom com = new BluetoothDeviceCom();
        return com;
    }

    public DeviceCom createModemCom() {
        requestRecordingPermission();
        return new AudioDeviceCom(this);
    }

    public void statusUpdated(DeviceStatus status) {
        toggleBuzzer(status);

        TextView voltField = (TextView) findViewById(R.id.voltTextField);
        voltField.setText(String.format(locale, "%.2fV", status.voltage));

        TextView currentField = (TextView) findViewById(R.id.currentTextField);
        currentField.setText(String.format(locale, "%dμA", status.current));

        TextView timeField = (TextView) findViewById(R.id.timeTextField);
        timeField.setText(String.format(locale, "%ds", status.timer));

        TextView resistanceField = (TextView) findViewById(R.id.resistanceTextField);
        resistanceField.setText(String.format(locale, "%.2fΩ", status.resistance));
        
        TextView sessionCountField = (TextView) findViewById(R.id.sessionCountTextField);
        sessionCountField.setText(String.format(locale, "%d", status.sessionKills));

        TextView lifeTimeCountField = (TextView) findViewById(R.id.lifeTimeCountTextField);
        lifeTimeCountField.setText(String.format(locale, "%d", status.lifetimeKills));

        TextView messageField = (TextView) findViewById(R.id.messageTextView);
        messageField.setText(String.format("%s\n%s", messageField.getText(), status.message));

        ScrollView sv = (ScrollView) findViewById(R.id.messageScrollView);
        sv.scrollTo(0, sv.getBottom());
    }

    private void toggleBuzzer(DeviceStatus status) {
        if ((status.timer > 0 || status.voltage > 1.0) && buzzerEnabled)
            tonePlayer.play();
        else
            tonePlayer.stop();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    deviceCom.setupComplete();
                } else {
                    // User did not enable Bluetooth or an error occurred
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestRecordingPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED)
            requestModifyAudioSettingsPermission();

        else {
            Log.i(LOG_TAG, "request recording permission");

            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestModifyAudioSettingsPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.MODIFY_AUDIO_SETTINGS);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED)
            Log.d(LOG_TAG,"");
        else
            requestPermissions(new String[]{Manifest.permission.MODIFY_AUDIO_SETTINGS},
                    MY_PERMISSIONS_REQUEST_MODIFY_AUDIO_SETTINGS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.i(LOG_TAG, String.format("permission result in: %s", requestCode));

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // if recording permission was granted, also request permission to modify audio settings
                    requestModifyAudioSettingsPermission();
                    Log.i(LOG_TAG, "permission granted");
                } else
                    Log.i(LOG_TAG, "permission denied");

                return;
            }

            case MY_PERMISSIONS_REQUEST_MODIFY_AUDIO_SETTINGS: {
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
//                    modem.setSampleRate();
            }
        }
    }

    protected void onPause() {
        super.onPause();
        deviceCom.close();
    }

    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        deviceCom.close();
    }

    protected void onStop() {
        super.onStop();
        deviceCom.close();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                this.startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
