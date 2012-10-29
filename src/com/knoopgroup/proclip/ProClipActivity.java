package com.knoopgroup.proclip;

import java.io.IOException;
import java.nio.charset.Charset;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class ProClipActivity extends Activity {

	boolean mWriteMode;
	IntentFilter[] mWriteTagFilters;
	NfcAdapter mNfcAdapter;
	PendingIntent mNfcPendingIntent;
	Dialog mDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Button createTag = (Button) findViewById(R.id.create_tag_button);
		createTag.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startTagWrite();
			}
		});

		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

		mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
	}

	private void startTagWrite() {

		enableTagWriteMode();

		mDialog = new AlertDialog.Builder(ProClipActivity.this)
				.setTitle("Touch tag to write").create();
		mDialog.show();
	}
	
	private void stopTagWrite() {
		disableTagWriteMode();
		mDialog.dismiss();
	}

	private void enableTagWriteMode() {
		mWriteMode = true;
		IntentFilter tagDetected = new IntentFilter(
				NfcAdapter.ACTION_TAG_DISCOVERED);
		mWriteTagFilters = new IntentFilter[] { tagDetected };
		mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
				mWriteTagFilters, null);
	}
	
	private void disableTagWriteMode() {
		mWriteMode = false;
		mNfcAdapter.disableForegroundDispatch(this);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		// Tag writing mode
		if (mWriteMode
				&& NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
			Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			if (writeTag(detectedTag)) {
				Toast.makeText(this, "Success: Wrote placeid to nfc tag",
						Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(this, "Write failed", Toast.LENGTH_LONG).show();
			}
			
			stopTagWrite();
		}
	}

	public static NdefMessage getNefMessage() {
		String msg = "com.knoopgroup.proclip";
		byte[] textBytes = msg.getBytes();
		NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
				NdefRecord.RTD_TEXT, new byte[] {}, textBytes);
		return new NdefMessage(new NdefRecord[] { textRecord });
	}

	public boolean writeTag(Tag tag) {
		Ndef ndef = Ndef.get(tag);
		try {
			if (ndef != null) {
				ndef.connect();
				ndef.writeNdefMessage(getNefMessage());
			} else {
				NdefFormatable format = NdefFormatable.get(tag);
				if (format != null) {
					format.connect();
					format.format(getNefMessage());
				}
				else {
					return false;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (FormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		return true;
	}
}