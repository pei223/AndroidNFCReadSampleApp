package com.android.p2app.nfcsample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;


import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private final String AUTO_URL_OPEN_KEY = "AUTO_URL_OPEN";
    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.w("onCreate");
        super.onCreate(savedInstanceState);
        pref = getPreferences(Context.MODE_PRIVATE);
        setContentView(R.layout.activity_main);
        setupViews();
        onNewIntent(getIntent());
    }

    private void setupViews() {
        Button openUri = findViewById(R.id.open_uri);
        openUri.setOnClickListener(view -> {
            TextView uriView = findViewById(R.id.uri);
            String url = (String) uriView.getText();
            launchBrowser(url);
        });

        Switch autoUrlOpenMode = findViewById(R.id.auto_url_open_mode);
        autoUrlOpenMode.setChecked(getAutoUrlOpenMode());
        autoUrlOpenMode.setOnClickListener(view -> {
            setAutoUrlOpenMode(((Switch) view).isChecked());
        });
    }


    @Override
    protected void onNewIntent(Intent intent) {
        // launchModeをsingleTopにしているためonNewIntent契機でも起動される
        Timber.w("onNewIntent", intent.getAction());
        super.onNewIntent(intent);
        if (!NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            return;
        }
        Parcelable[] rawMessages =
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMessages == null) {
            Timber.w("No rawMessages");
            return;
        }
        NdefMessage[] messages = new NdefMessage[rawMessages.length];

        String text = "";
        String uri = "";
        for (int i = 0; i < rawMessages.length; i++) {
            messages[i] = (NdefMessage) rawMessages[i];
            for (NdefRecord record : messages[i].getRecords()) {
                if (record.getTnf() != NdefRecord.TNF_WELL_KNOWN) {
                    Timber.w("Unknown TNF", record.getTnf());
                    continue;
                }

                String recordType = new String(record.getType());
                if (recordType.equals("T")) {
                    text = new String(record.getPayload());
                    continue;
                }
                if (recordType.equals("U")) {
                    uri = new String(record.getPayload());
                    continue;
                }
                Timber.w("Unknown recordType", recordType);
            }
        }
        if (getAutoUrlOpenMode()) {
            launchBrowser("https://" + uri.trim());
            return;
        }
        showResult(text, uri);
    }

    private void launchBrowser(String url) {
        if (url.isEmpty()) {
            return;
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private boolean getAutoUrlOpenMode() {
        return pref.getBoolean(AUTO_URL_OPEN_KEY, false);
    }

    private void setAutoUrlOpenMode(boolean v) {
        pref.edit().putBoolean(AUTO_URL_OPEN_KEY, v).apply();
    }

    private void showResult(String text, String uri) {
        findViewById(R.id.read_result).setVisibility(View.VISIBLE);
        TextView textView = findViewById(R.id.text);
        TextView uriView = findViewById(R.id.uri);
        textView.setText(text);
        uriView.setText("https://" + uri.trim());
    }
}