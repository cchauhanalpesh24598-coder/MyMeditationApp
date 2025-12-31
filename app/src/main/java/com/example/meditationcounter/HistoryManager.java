package com.example.meditationcounter;

import android.content.*;
import android.database.sqlite.*;
import android.database.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class HistoryManager {
    private SQLiteDatabase db;

    public HistoryManager(Context context) {
        db = context.openOrCreateDatabase("MantraDB", Context.MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS history (id INTEGER PRIMARY KEY AUTOINCREMENT, mantra TEXT, count INTEGER, date TEXT, time TEXT)");
    }

    public void recordClick(String mantra) {
        // Fix: Default() ko getDefault() kiya gaya
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        db.execSQL("INSERT INTO history (mantra, count, date, time) VALUES (?, 1, ?, ?)", new Object[]{mantra, date, time});
    }

    public Map<String, Integer> getHistoryForDate(String dateStr) {
        Map<String, Integer> stats = new HashMap<String, Integer>();
        Cursor cursor = db.rawQuery("SELECT mantra, SUM(count) FROM history WHERE date = ? GROUP BY mantra", new String[]{dateStr});
        if (cursor.moveToFirst()) {
            do {
                stats.put(cursor.getString(0), cursor.getInt(1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return stats;
    }

    public int[] getHourlyStatsForDate(String dateKey) {
		int[] hourly = new int[7]; 
		// Slots distribution:
		// 0: 4-7 AM, 1: 7-10 AM, 2: 10AM-1PM, 3: 1-4 PM, 4: 4-7 PM, 5: 7-10 PM, 6: 10PM-1AM

		Cursor cursor = db.rawQuery("SELECT time, count FROM history WHERE date = ?", new String[]{dateKey});
		if (cursor.moveToFirst()) {
			do {
				try {
					String timeVal = cursor.getString(0); // Format "HH:mm"
					if (timeVal != null && timeVal.contains(":")) {
						int hour = Integer.parseInt(timeVal.split(":")[0]);
						int slot = -1;

						// 4 AM se 10 PM (22:00) tak ka logic
						if (hour >= 4 && hour < 22) {
							slot = (hour - 4) / 3; 
						} 
						// Raat 10 PM (22:00) se lekar raat 1 AM (01:00) tak ka logic
						else if (hour >= 22 || hour < 1) {
							slot = 6; 
						}

						if (slot >= 0 && slot < 7) {
							hourly[slot] += cursor.getInt(1);
						}
					}
				} catch (Exception e) {}
			} while (cursor.moveToNext());
		}
		cursor.close();
		return hourly;
	}
	

    public int[] getMonthlyStats() {
        int[] daily = new int[31];
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        Cursor cursor = db.rawQuery("SELECT substr(date, 9, 2), SUM(count) FROM history WHERE date LIKE ? GROUP BY date", new String[]{currentMonth + "%"});
        if (cursor.moveToFirst()) {
            do {
                try {
                    int day = Integer.parseInt(cursor.getString(0)) - 1;
                    if (day >= 0 && day < 31) daily[day] = cursor.getInt(1);
                } catch (Exception e) {}
            } while (cursor.moveToNext());
        }
        cursor.close();
        return daily;
    }

    public int[] getYearlyStats() {
        int[] monthly = new int[12];
        String currentYear = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
        Cursor cursor = db.rawQuery("SELECT substr(date, 6, 2), SUM(count) FROM history WHERE date LIKE ? GROUP BY substr(date, 6, 2)", new String[]{currentYear + "%"});
        if (cursor.moveToFirst()) {
            do {
                try {
                    int mIdx = Integer.parseInt(cursor.getString(0)) - 1;
                    if (mIdx >= 0 && mIdx < 12) monthly[mIdx] = cursor.getInt(1);
                } catch (Exception e) {}
            } while (cursor.moveToNext());
        }
        cursor.close();
        return monthly;
    }

    // Requirement No. 6: AI Insight Logic
    public String getAIInsight() { // Iska naam getAIInsight rakha taaki StatsActivity se match kare
        Cursor cursor = db.rawQuery("SELECT substr(time, 1, 2) as hr, SUM(count) as total FROM history GROUP BY hr ORDER BY total DESC LIMIT 1", null);
        int bestHour = -1;
        if (cursor.moveToFirst()) {
            bestHour = cursor.getInt(0);
        }
        cursor.close();
        if (bestHour == -1) return "AI: Abhi data kam hai. Jap shuru karein!";
        return "AI Insight: Aapka sabse focused samay " + bestHour + ":00 baje hai.";
    }
}

