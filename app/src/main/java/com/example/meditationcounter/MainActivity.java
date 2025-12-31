package com.example.meditationcounter;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.provider.Settings;
import android.bluetooth.*;
import android.net.Uri; 
import android.hardware.*; 
import java.util.*;
import java.text.SimpleDateFormat;
import com.example.meditationcounter.R;

public class MainActivity extends Activity implements SensorEventListener {
    private int count = 0;
    private TextView counterText, malaText, bluetoothBatteryText;
    private SharedPreferences sharedPreferences;
    private HistoryManager historyManager;
    private String currentMantra = "Mantra 1";
    private Spinner mantraSpinner;
    private BluetoothAdapter mBluetoothAdapter;

    private Button counterBtn; 
    private boolean isCounterEnabled = false;

    private WindowManager windowManager;
    private View floatingView, removeView;
    private TextView floatingText;
    private BroadcastReceiver countReceiver;
    private ProgressBar pinkRing, blueRing;
    private FrameLayout ringContainer; 

    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private Vibrator vibrator; // Naya: Vibrator variable

    private int currentThemeId = 1;
    private boolean isUltraSaverActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            sharedPreferences = getSharedPreferences("MantraPrefs", Context.MODE_PRIVATE);
            currentThemeId = sharedPreferences.getInt("selected_theme_id", 1);
            setContentView(R.layout.main);

            // Vibrator Initialize kiya
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
            }

            counterText = (TextView) findViewById(R.id.counterText);
            malaText = (TextView) findViewById(R.id.malaText);
            bluetoothBatteryText = (TextView) findViewById(R.id.bluetoothBatteryText);
            mantraSpinner = (Spinner) findViewById(R.id.mantraSpinner);
            counterBtn = (Button) findViewById(R.id.counterSwitch); 
            pinkRing = (ProgressBar) findViewById(R.id.pinkRing);
            blueRing = (ProgressBar) findViewById(R.id.blueRing);
            ringContainer = (FrameLayout) findViewById(R.id.ringContainer); 

            historyManager = new HistoryManager(this);
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

            currentMantra = sharedPreferences.getString("current_mantra_name", "Mantra 1");
            checkAndResetDailyCounter();
            count = sharedPreferences.getInt(currentMantra + "_count", 0);

            applyExtremeTheme(currentThemeId);
            updateUI();
            setupButtons();
            setupMantraSpinner(); 
            setupBubbleButton(); 

            countReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if ("COUNT_INCREMENTED".equals(intent.getAction())) {
                        incrementAndAutoSave();
                    }
                }
            };
            registerReceiver(countReceiver, new IntentFilter("COUNT_INCREMENTED"));

            isCounterEnabled = sharedPreferences.getBoolean("switch_state", false);
            updateCounterButtonStyle(isCounterEnabled);

            if (counterBtn != null) {
                counterBtn.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							isCounterEnabled = !isCounterEnabled;
							sharedPreferences.edit().putBoolean("switch_state", isCounterEnabled).apply();
							vibrate(50); // Direct vibration call
							updateCounterButtonStyle(isCounterEnabled);
							showAIInsight();
							if (isCounterEnabled) startJapService(); else stopJapService();
						}
					});
                if (isCounterEnabled) startJapService();
            }
            getBluetoothBattery();
        } catch (Exception e) {
            Toast.makeText(this, "Init Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method direct vibration ke liye
    private void vibrate(int duration) {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(duration);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isCounterEnabled && event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] < proximitySensor.getMaximumRange()) {
                applyBatterySaver(true);
            } else {
                if (!isUltraSaverActive) applyBatterySaver(false);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void applyBatterySaver(boolean enable) {
        try {
            boolean canWrite = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                canWrite = Settings.System.canWrite(this);
            }
            if (canWrite) {
                if (enable) {
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 1);
                } else {
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
                }
            }
            Window window = getWindow();
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.screenBrightness = enable ? 0.01f : -1f;
            window.setAttributes(lp);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAIInsight() {
        if (!isCounterEnabled) return;
        String insight = "Focus set. ";
        if (count > 500) insight += "Excellent consistency today!";
        else if (count > 108) insight += "You're doing great, keep going.";
        else insight += "Peace begins with the first step.";
        Toast.makeText(this, "AI Insight: " + insight, Toast.LENGTH_SHORT).show();
    }

    private void applyExtremeTheme(int themeId) {
        String primary = "#3ABFF8"; 
        String bg = "#0B0E14";
        switch (themeId) {
            case 2: primary = "#FBBF24"; bg = "#1A1A1A"; break;
            case 3: primary = "#10B981"; bg = "#062019"; break;
            case 4: primary = "#A855F7"; bg = "#1A0B2E"; break;
            case 5: primary = "#EF4444"; bg = "#2D0505"; break;
        }
        getWindow().getDecorView().setBackgroundColor(Color.parseColor(bg));
        if (counterText != null) {
            counterText.setTextColor(Color.parseColor(primary));
            counterText.setShadowLayer(25, 0, 10, Color.parseColor(primary));
        }
        if (blueRing != null) blueRing.getProgressDrawable().setColorFilter(Color.parseColor(primary), PorterDuff.Mode.SRC_IN);
        refreshExtremeButtons(primary);
    }

    private void refreshExtremeButtons(String color) {
        apply3DGradient(findViewById(R.id.analysisBtn), color);
        apply3DGradient(findViewById(R.id.historyBtn), color);
        apply3DGradient(findViewById(R.id.floatingModeBtn), color);
        apply3DGradient(findViewById(R.id.resetBtn), color);
        apply3DGradient(findViewById(R.id.settingsBtn), color);

        View batteryBtn = findViewById(R.id.batterySaverBtn);
        if (batteryBtn != null) {
            GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] { Color.parseColor("#4DFFFFFF"), Color.parseColor("#10FFFFFF") });
            gd.setCornerRadius(20f);
            gd.setStroke(3, Color.parseColor(color));
            batteryBtn.setBackground(gd);
        }
    }

    private void apply3DGradient(final View v, final String color) {
        if (v == null) return;
        v.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent event) {
					switch (event.getAction()) {
						case MotionEvent.ACTION_DOWN:
							view.setScaleX(0.92f); view.setScaleY(0.92f); view.setAlpha(0.8f);
							break;
						case MotionEvent.ACTION_UP:
						case MotionEvent.ACTION_CANCEL:
							view.setScaleX(1.0f); view.setScaleY(1.0f); view.setAlpha(1.0f);
							break;
					}
					return false; 
				}
			});
    }

    private void setupBubbleButton() {
        if (counterText != null) {
            counterText.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						vibrate(40); // Direct vibration on bubble touch
						incrementAndAutoSave();
					}
				});
            counterText.setOnTouchListener(new View.OnTouchListener() {
					@Override
					public boolean onTouch(View view, MotionEvent event) {
						switch (event.getAction()) {
							case MotionEvent.ACTION_DOWN:
								view.setScaleX(0.92f); view.setScaleY(0.92f);
								break;
							case MotionEvent.ACTION_UP:
							case MotionEvent.ACTION_CANCEL:
								view.setScaleX(1.0f); view.setScaleY(1.0f);
								break;
						}
						return false;
					}
				});
        }
    }

    private void startSatisfyingRipple() {
        if (ringContainer == null) return;
        final View ripple = new View(this);
        int size = 400; 
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size, Gravity.CENTER);
        ripple.setLayoutParams(params);
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(Color.parseColor("#403ABFF8")); 
        shape.setStroke(2, Color.parseColor("#3ABFF8"));
        ripple.setBackground(shape);
        ringContainer.addView(ripple, 0); 
        ripple.animate().scaleX(2.5f).scaleY(2.5f).alpha(0f).setDuration(600).withEndAction(new Runnable() {
                @Override public void run() { ringContainer.removeView(ripple); }
            }).start();
    }

    private void updateCounterButtonStyle(boolean isOn) {
        if (counterBtn == null) return;
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(30f); 
        if (isOn) {
            counterBtn.setText("ON");
            counterBtn.setTextColor(Color.WHITE);
            gd.setColor(Color.parseColor("#1B9C42")); 
            gd.setStroke(6, Color.parseColor("#14532D")); 
        } else {
            counterBtn.setText("OFF");
            counterBtn.setTextColor(Color.parseColor("#94A3B8")); 
            gd.setColor(Color.parseColor("#1E293B")); 
            gd.setStroke(4, Color.parseColor("#334155")); 
        }
        if (Build.VERSION.SDK_INT >= 16) counterBtn.setBackground(gd); else counterBtn.setBackgroundDrawable(gd);
    }

    private void setupButtons() {
        View batteryBtn = findViewById(R.id.batterySaverBtn);
        if (batteryBtn != null) {
            batteryBtn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						vibrate(50);
						isUltraSaverActive = !isUltraSaverActive;
						applyBatterySaver(isUltraSaverActive);
						Toast.makeText(MainActivity.this, "Ultra Battery Saver: " + (isUltraSaverActive ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
					}
				});
        }
        findViewById(R.id.settingsBtn).setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					vibrate(50);
					try { startActivity(new Intent(MainActivity.this, SettingsActivity.class)); } 
                    catch (Exception e) { Toast.makeText(MainActivity.this, "Settings error", Toast.LENGTH_SHORT).show(); }
				}
			});
        findViewById(R.id.resetBtn).setOnClickListener(new View.OnClickListener() { 
				@Override public void onClick(View v) { vibrate(100); resetCount(); } 
			});
        findViewById(R.id.renameMantraBtn).setOnClickListener(new View.OnClickListener() { 
				@Override public void onClick(View v) { vibrate(50); showRenameDialog(); } 
			});
        findViewById(R.id.analysisBtn).setOnClickListener(new View.OnClickListener() { 
				@Override public void onClick(View v) { vibrate(50); startActivity(new Intent(MainActivity.this, StatsActivity.class).putExtra("mode", "analysis")); } 
			});
        findViewById(R.id.historyBtn).setOnClickListener(new View.OnClickListener() { 
				@Override public void onClick(View v) { vibrate(50); startActivity(new Intent(MainActivity.this, StatsActivity.class).putExtra("mode", "calendar")); } 
			});
        findViewById(R.id.floatingModeBtn).setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					vibrate(50);
					if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(MainActivity.this)) {
						startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
					} else { if (floatingView == null) showFloatingBubble(); else hideFloatingBubble(); }
				}
			});
    }

    private void setupMantraSpinner() {
        final String s1 = sharedPreferences.getString("slot_0", "Mantra 1");
        final String s2 = sharedPreferences.getString("slot_1", "Mantra 2");
        final String s3 = sharedPreferences.getString("slot_2", "Mantra 3");
        final String s4 = sharedPreferences.getString("slot_3", "Mantra 4");
        final String[] items = {s1, s2, s3, s4};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            @Override public View getView(int p, View c, ViewGroup pg) {
                View v = super.getView(p, c, pg);
                ((TextView) v).setTextColor(Color.WHITE);
                ((TextView) v).setTypeface(null, Typeface.BOLD);
                return v;
            }
            @Override public View getDropDownView(int p, View c, ViewGroup pg) {
                View v = super.getDropDownView(p, c, pg);
                v.setBackgroundColor(Color.parseColor("#1E293B"));
                ((TextView) v).setTextColor(Color.WHITE);
                return v;
            }
        };
        mantraSpinner.setAdapter(adapter);
        for(int i=0; i<items.length; i++) { if(items[i].equals(currentMantra)) mantraSpinner.setSelection(i); }
        mantraSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
					currentMantra = p.getItemAtPosition(pos).toString();
					sharedPreferences.edit().putString("current_mantra_name", currentMantra).apply();
					count = sharedPreferences.getInt(currentMantra + "_count", 0);
					updateUI();
				}
				@Override public void onNothingSelected(AdapterView<?> p) {}
			});
    }

    public synchronized void incrementAndAutoSave() {
        count++; updateUI();
        sharedPreferences.edit().putInt(currentMantra + "_count", count).apply();
        runOnUiThread(new Runnable() { @Override public void run() { startSatisfyingRipple(); } });
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        sharedPreferences.edit().putString("last_reset_date", todayDate).apply();
        new Thread(new Runnable() { @Override public void run() { historyManager.recordClick(currentMantra); } }).start();
    }

    private void updateUI() {
        runOnUiThread(new Runnable() {
				@Override public void run() {
					if (counterText != null) counterText.setText(String.valueOf(count));
					if (malaText != null) malaText.setText("Mala: " + (count / 108));
					if (floatingText != null) floatingText.setText(String.valueOf(count));
					if(blueRing != null) blueRing.setProgress(count % 108);
					if(pinkRing != null) pinkRing.setProgress(count % 1000);
				}
			});
    }

    private void resetCount() {
        count = 0; sharedPreferences.edit().putInt(currentMantra + "_count", 0).apply();
        updateUI(); Toast.makeText(this, "Reset Done", Toast.LENGTH_SHORT).show();
    }

    private void showRenameDialog() {
        final int pos = mantraSpinner.getSelectedItemPosition();
        final EditText input = new EditText(this);
        input.setText(currentMantra);
        input.setPadding(50, 40, 50, 40);
        new AlertDialog.Builder(this).setTitle("Rename Mantra").setView(input)
            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        int savedCount = sharedPreferences.getInt(currentMantra + "_count", 0);
                        sharedPreferences.edit().putString("slot_" + pos, newName).putString("current_mantra_name", newName).putInt(newName + "_count", savedCount).apply();
                        currentMantra = newName; setupMantraSpinner(); updateUI();
                    }
                }
            }).setNegativeButton("Cancel", null).show();
    }

    private void getBluetoothBattery() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            if (bluetoothBatteryText != null) bluetoothBatteryText.setText("BT: Off"); return;
        }
        mBluetoothAdapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
				@Override public void onServiceConnected(int profile, BluetoothProfile proxy) {
					List<BluetoothDevice> devices = proxy.getConnectedDevices();
					if (!devices.isEmpty()) {
						final BluetoothDevice device = devices.get(0);
						int batteryLevel = -1;
						try { java.lang.reflect.Method method = device.getClass().getMethod("getBatteryLevel"); batteryLevel = (int) method.invoke(device); } catch (Exception e) { batteryLevel = -1; }
						final int level = batteryLevel; final String name = device.getName();
						runOnUiThread(new Runnable() { @Override public void run() { if (bluetoothBatteryText != null) bluetoothBatteryText.setText("BT: " + name + (level != -1 ? " (" + level + "%)" : "")); } });
					}
					mBluetoothAdapter.closeProfileProxy(profile, proxy);
				}
				@Override public void onServiceDisconnected(int profile) {}
			}, BluetoothProfile.HEADSET);
    }

    public void showFloatingBubble() {
        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            int type = (Build.VERSION.SDK_INT >= 26) ? 2038 : 2002;
            removeView = new FrameLayout(this);
            GradientDrawable rc = new GradientDrawable(); rc.setShape(GradientDrawable.OVAL); rc.setColor(Color.parseColor("#99FF0000"));
            removeView.setBackground(rc);
            TextView cross = new TextView(this); cross.setText("✕"); cross.setTextColor(Color.WHITE); cross.setGravity(Gravity.CENTER);
            ((FrameLayout) removeView).addView(cross); removeView.setVisibility(View.GONE);
            final WindowManager.LayoutParams rLp = new WindowManager.LayoutParams(180, 180, type, 8, -3);
            rLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL; rLp.y = 120;
            windowManager.addView(removeView, rLp);
            floatingView = new FrameLayout(this);
            GradientDrawable orb = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] { Color.parseColor("#CC2196F3"), Color.parseColor("#FF1565C0") });
            orb.setShape(GradientDrawable.OVAL); orb.setStroke(3, Color.BLACK);
            floatingView.setBackground(orb);
            floatingText = new TextView(this); floatingText.setText(String.valueOf(count));
            floatingText.setTextColor(Color.WHITE); floatingText.setGravity(Gravity.CENTER);
            ((FrameLayout) floatingView).addView(floatingText);
            final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(180, 180, type, 8, -3);
            lp.gravity = Gravity.TOP | Gravity.LEFT; lp.x = 200; lp.y = 200;
            windowManager.addView(floatingView, lp);
            floatingView.setOnTouchListener(new View.OnTouchListener() {
					private int iX, iY; private float iTX, iTY; private long sT; private boolean isM = false;
					@Override public boolean onTouch(View v, MotionEvent e) {
						switch (e.getAction()) {
							case MotionEvent.ACTION_DOWN: sT = System.currentTimeMillis(); iX = lp.x; iY = lp.y; iTX = e.getRawX(); iTY = e.getRawY(); return true;
							case MotionEvent.ACTION_MOVE:
								int dX = (int)(e.getRawX()-iTX); int dY = (int)(e.getRawY()-iTY);
								if (Math.abs(dX)>15 || Math.abs(dY)>15) { isM = true; removeView.setVisibility(View.VISIBLE); lp.x = iX + dX; lp.y = iY + dY; windowManager.updateViewLayout(floatingView, lp); } return true;
							case MotionEvent.ACTION_UP:
								removeView.setVisibility(View.GONE);
								if (!isM && (System.currentTimeMillis()-sT < 300)) { 
                                    vibrate(40); // Bubble touch vibration
                                    incrementAndAutoSave(); 
                                }
								else if (isM && lp.y > getResources().getDisplayMetrics().heightPixels - 450) { hideFloatingBubble(); }
								isM = false; return true;
						} return false;
					}
				});
            moveTaskToBack(true);
        } catch (Exception e) {}
    }

    public void hideFloatingBubble() {
        if (windowManager != null) {
            if (floatingView != null) { try { windowManager.removeView(floatingView); } catch(Exception e){} floatingView = null; }
            if (removeView != null) { try { windowManager.removeView(removeView); } catch(Exception e){} removeView = null; }
        }
    }

    private void checkAndResetDailyCounter() {
        String lastSavedDate = sharedPreferences.getString("last_reset_date", "");
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        if (!todayDate.equals(lastSavedDate)) {
            count = 0; sharedPreferences.edit().putInt(currentMantra + "_count", 0).putString("last_reset_date", todayDate).apply();
            updateUI();
        }
    }

    private void startJapService() { Intent i = new Intent(this, BackgroundService.class); if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i); }
    private void stopJapService() { stopService(new Intent(this, BackgroundService.class)); }

    @Override protected void onResume() { 
        super.onResume(); 
        checkAndResetDailyCounter(); 
        if (proximitySensor != null) {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override protected void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    @Override protected void onDestroy() { 
        super.onDestroy(); 
        try { unregisterReceiver(countReceiver); } catch (Exception e) {} 
    }
}


