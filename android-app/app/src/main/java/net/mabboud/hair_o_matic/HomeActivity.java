package net.mabboud.hair_o_matic;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import net.mabboud.hair_o_matic.audio_com.AudioModem;

public class HomeActivity extends AppCompatActivity {
    private static final String LOG_TAG = "home";
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 235;
    private static final int MY_PERMISSIONS_REQUEST_MODIFY_AUDIO_SETTINGS = 183;

    private AudioModem modem = new AudioModem(this);;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initModem();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    public void initModem() {
        requestRecordingPermission();
        modem.listen();
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
            modem.setSampleRate();
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
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    modem.setSampleRate();
            }
        }
    }

}
