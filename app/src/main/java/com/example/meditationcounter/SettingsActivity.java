package com.example.meditationcounter;

import android.app.*;
import android.os.*;
import android.widget.*;
import android.view.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.content.*;
import android.net.Uri;
import android.os.Environment;
import android.media.MediaScannerConnection;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import com.example.meditationcounter.R;


public class SettingsActivity extends Activity {
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        prefs = getSharedPreferences("MantraPrefs", Context.MODE_PRIVATE);

        // PDF Export Button
        Button pdfBtn = (Button) findViewById(R.id.exportPdfBtn);
        if (pdfBtn != null) {
            pdfBtn.setOnClickListener(new View.OnClickListener() {
					@Override public void onClick(View v) { exportToPDF(); }
				});
        }

        // Backup Button
        Button backupBtn = (Button) findViewById(R.id.backupBtn);
        if (backupBtn != null) {
            backupBtn.setOnClickListener(new View.OnClickListener() {
					@Override public void onClick(View v) { createBackup(); }
				});
        }

        // --- Change Theme Option (Naya Code) ---
        Button changeThemeBtn = (Button) findViewById(R.id.changeThemeBtn);
        if (changeThemeBtn != null) {
            changeThemeBtn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showThemeSelectionDialog();
					}
				});
        }
    }

    private void showThemeSelectionDialog() {
        final String[] names = {"Original Default", "Royal Gold 3D", "Emerald Zen", "Mystic Purple", "Blood Energy"};

        new AlertDialog.Builder(this)
            .setTitle("Select Premium Theme")
            .setItems(names, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which) {
                    // Theme ID save karein (Default 1 hota hai)
                    prefs.edit().putInt("selected_theme_id", which + 1).apply();

                    Toast.makeText(SettingsActivity.this, "Theme Saved!", Toast.LENGTH_SHORT).show();

                    // MainActivity ko restart karein taaki theme apply ho jaye
                    Intent i = new Intent(SettingsActivity.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                }
            }).show();
    }

    private void exportToPDF() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 600, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(14);
        canvas.drawText("Mantra Counter Report", 20, 40, paint);
        canvas.drawText("Date: " + new SimpleDateFormat("dd-MM-yyyy").format(new Date()), 20, 70, paint);

        document.finishPage(page);

        File downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadFolder.exists()) downloadFolder.mkdirs();

        File pdfFile = new File(downloadFolder, "Mantra_Report_" + System.currentTimeMillis() + ".pdf");

        try {
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            MediaScannerConnection.scanFile(this, new String[]{pdfFile.getPath()}, null, null);
            Toast.makeText(this, "PDF Saved in Downloads!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Permission Denied! Check App Settings.", Toast.LENGTH_LONG).show();
        }
    }

    private void createBackup() {
        String data = prefs.getAll().toString();
        File downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File backupFile = new File(downloadFolder, "Mantra_Backup_Manual.txt");

        try {
            FileWriter writer = new FileWriter(backupFile);
            writer.append(data);
            writer.flush(); writer.close();

            MediaScannerConnection.scanFile(this, new String[]{backupFile.getPath()}, null, null);
            Toast.makeText(this, "Backup Saved in Downloads!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Backup Failed!", Toast.LENGTH_SHORT).show();
        }
    }
}


