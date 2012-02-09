package com.knoopgroup.proclip;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.widget.Toast;

public class ProClipActivity extends Activity {
	
	boolean mWriteMode;
	IntentFilter[] mWriteTagFilters;
	NfcAdapter mNfcAdapter;
	PendingIntent mNfcPendingIntent;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        
        mNfcPendingIntent = PendingIntent.getActivity(this, 0,
    		    new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }
    
    private void startTagWrite() {
    	 
    	enableTagWriteMode();
    	 
    	new AlertDialog.Builder(ProClipActivity.this).setTitle("Touch tag to write").create().show();
    }
    
    private void enableTagWriteMode() {
        mWriteMode = true;
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        mWriteTagFilters = new IntentFilter[] { tagDetected };
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        // Tag writing mode
        if (mWriteMode && NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (writeTag(getPlaceidAsNdef(123l), detectedTag)) {
                Toast.makeText(this, "Success: Wrote placeid to nfc tag", Toast.LENGTH_LONG)
                    .show();
            } else {
                Toast.makeText(this, "Write failed", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    /*
    * Converts a Long into a NdefMessage in application/vnd.facebook.places MIMEtype.
    *
    * for writing Places
    */
    public static NdefMessage getPlaceidAsNdef(Long id) {
        String msg = ((Long) id).toString();
        byte[] textBytes = msg.getBytes();
        NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
            "application/vnd.facebook.places".getBytes(), new byte[] {}, textBytes);
        return new NdefMessage(new NdefRecord[] { textRecord });
    }
    
    /*
    * Writes an NdefMessage to a NFC tag
    */
    public static boolean writeTag(NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    return false;
                }
                if (ndef.getMaxSize() < size) {
                    return false;
                }
                ndef.writeNdefMessage(message);
                return true;
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }
}