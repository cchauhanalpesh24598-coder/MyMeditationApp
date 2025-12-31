package com.example.meditationcounter;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.content.Context;
import java.util.*;
import java.text.SimpleDateFormat;

public class StatsActivity extends Activity {
    private LinearLayout mainLayout, calendarGrid, hourlyGraphContainer;
    private HistoryManager historyManager;
    private TextView monthTitle;
    private Calendar currentCalendar;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private String[] hLabels = {"4-7 AM", "7-10 AM", "10-1 PM", "1-4 PM", "4-7 PM", "7-10 PM", "10-12"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        historyManager = new HistoryManager(this);
        currentCalendar = Calendar.getInstance();
        String mode = getIntent().getStringExtra("mode");

        ScrollView scroll = new ScrollView(this);
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(30, 40, 30, 40);
        scroll.addView(mainLayout);
        setContentView(scroll);

        if ("analysis".equals(mode)) {
            renderAnalysisPage();
        } else {
            renderHistoryPage();
        }
    }

    private void renderHistoryPage() {
        mainLayout.setBackgroundColor(Color.parseColor("#FFF9F0")); 
        setupCalendarUI();
        addTitle("DAILY HOURLY PROGRESS", Color.parseColor("#E67E22"));

        hourlyGraphContainer = new LinearLayout(this);
        hourlyGraphContainer.setOrientation(LinearLayout.VERTICAL);
        mainLayout.addView(hourlyGraphContainer);

        updateHourlyGraph(sdf.format(new Date()));
    }

    private void updateHourlyGraph(String date) {
        hourlyGraphContainer.removeAllViews();
        int[] hData = historyManager.getHourlyStatsForDate(date);

        // Daily graph ke liye wide=false
        AdvancedGraph newGraph = new AdvancedGraph(this, hData, hLabels, Color.parseColor("#E67E22"), false);
        hourlyGraphContainer.addView(newGraph);

        // Feature No. 6: AI Insights Card Look
        LinearLayout insightCard = new LinearLayout(this);
        insightCard.setOrientation(LinearLayout.VERTICAL);
        insightCard.setPadding(30, 30, 30, 30);
        insightCard.setTranslationY(20);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.WHITE);
        gd.setCornerRadius(20f);
        gd.setStroke(2, Color.parseColor("#E67E22"));
        insightCard.setBackground(gd);

        TextView title = new TextView(this);
        title.setText("✨ AI INSIGHTS");
        title.setTextColor(Color.parseColor("#E67E22"));
        title.setTypeface(null, Typeface.BOLD);
        insightCard.addView(title);

        TextView insightTv = new TextView(this);
        insightTv.setText(historyManager.getAIInsight()); 
        insightTv.setPadding(0, 10, 0, 10);
        insightTv.setTextColor(Color.DKGRAY);
        insightCard.addView(insightTv);

        hourlyGraphContainer.addView(insightCard);
    }

    private void setupCalendarUI() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, 0, 0, 40);

        TextView prev = new TextView(this); prev.setText("<  "); prev.setTextSize(24); prev.setTextColor(Color.parseColor("#E67E22"));
        prev.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					currentCalendar.add(Calendar.MONTH, -1);
					renderCalendar();
				}
			});

        monthTitle = new TextView(this); monthTitle.setTextSize(20); monthTitle.setTypeface(null, Typeface.BOLD); monthTitle.setTextColor(Color.parseColor("#E67E22"));

        TextView next = new TextView(this); next.setText("  > "); next.setTextSize(24); next.setTextColor(Color.parseColor("#E67E22"));
        next.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					currentCalendar.add(Calendar.MONTH, 1);
					renderCalendar();
				}
			});

        header.addView(prev); header.addView(monthTitle); header.addView(next);
        mainLayout.addView(header);

        calendarGrid = new LinearLayout(this);
        calendarGrid.setOrientation(LinearLayout.VERTICAL);
        mainLayout.addView(calendarGrid);
        renderCalendar();
    }

    private void renderCalendar() {
        calendarGrid.removeAllViews();
        monthTitle.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentCalendar.getTime()));
        Calendar temp = (Calendar) currentCalendar.clone();
        temp.set(Calendar.DAY_OF_MONTH, 1);
        int firstDay = temp.get(Calendar.DAY_OF_WEEK) - 1;
        int maxDays = temp.getActualMaximum(Calendar.DAY_OF_MONTH);
        final String todayStr = sdf.format(new Date());
        int dayCounter = 1;

        for (int r = 0; r < 6; r++) {
            LinearLayout row = new LinearLayout(this);
            for (int c = 0; c < 7; c++) {
                FrameLayout cell = new FrameLayout(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 120, 1f); lp.setMargins(6, 6, 6, 6);
                if (!((r == 0 && c < firstDay) || dayCounter > maxDays)) {
                    temp.set(Calendar.DAY_OF_MONTH, dayCounter);
                    final String dKey = sdf.format(temp.getTime());
                    GradientDrawable gd = new GradientDrawable(); gd.setCornerRadius(15f);
                    if (dKey.equals(todayStr)) { gd.setColor(Color.parseColor("#E67E22")); }
                    else { gd.setColor(Color.WHITE); gd.setStroke(2, Color.LTGRAY); }
                    cell.setBackground(gd);
                    TextView tv = new TextView(this); tv.setText(String.valueOf(dayCounter)); tv.setGravity(Gravity.CENTER);
                    tv.setTextColor(dKey.equals(todayStr) ? Color.WHITE : Color.BLACK);
                    cell.addView(tv);

                    cell.setOnClickListener(new View.OnClickListener() {
							@Override public void onClick(View v) { showDayDetails(dKey); }
						});
                    dayCounter++;
                }
                row.addView(cell, lp);
            }
            calendarGrid.addView(row);
            if (dayCounter > maxDays) break;
        }
    }

    private void showDayDetails(String date) {
        Map<String, Integer> mantraData = historyManager.getHistoryForDate(date);
        StringBuilder sb = new StringBuilder(); int total = 0;
        if (mantraData != null && !mantraData.isEmpty()) {
            for (Map.Entry<String, Integer> entry : mantraData.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                total += entry.getValue();
            }
            sb.append("\n----------------\nTotal: ").append(total);
        } else { sb.append("No records found."); }
        new AlertDialog.Builder(this).setTitle("Date: " + date).setMessage(sb.toString()).setPositiveButton("OK", null).show();
        if (hourlyGraphContainer != null) updateHourlyGraph(date);
    }

    private void renderAnalysisPage() {
        mainLayout.setBackgroundColor(Color.parseColor("#121212")); 
        addTitle("MONTHLY PROGRESS (1-31)", Color.parseColor("#76FF03"));
        int[] mData = historyManager.getMonthlyStats();
        String[] dLabels = new String[31];
        for(int i=0; i<31; i++) dLabels[i] = String.valueOf(i+1);
        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        // Monthly graph ke liye wide=true
        hScroll.addView(new AdvancedGraph(this, mData, dLabels, Color.parseColor("#76FF03"), true));
        mainLayout.addView(hScroll);

        addTitle("YEARLY OVERVIEW", Color.parseColor("#FFEA00"));
        int[] yData = historyManager.getYearlyStats();
        String[] moLabels = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        mainLayout.addView(new AdvancedGraph(this, yData, moLabels, Color.parseColor("#FFEA00"), false));
    }

    private void addTitle(String t, int col) {
        TextView tv = new TextView(this); tv.setText(t);
        tv.setTextColor(col); tv.setTextSize(14); tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(10, 50, 0, 20); mainLayout.addView(tv);
    }

    private static class AdvancedGraph extends View {
        private int[] data; private String[] labels; private int color; private Paint p; private boolean wide;

        public AdvancedGraph(Context c, int[] d, String[] l, int col, boolean w) {
            super(c); this.data = d; this.labels = l; this.color = col; this.wide = w; 
            this.p = new Paint(Paint.ANTI_ALIAS_FLAG);
        }

        @Override protected void onMeasure(int w, int h) {
            int width = wide ? 2800 : MeasureSpec.getSize(w);
            setMeasuredDimension(width, 600);
        }

        @Override protected void onDraw(Canvas canvas) {
            float pL = 80, pB = 60, pT = 100, gW = getWidth() - pL - 50, gH = getHeight() - pB - pT;
            int max = 0; 
            for (int d : data) if (d > max) max = d; 
            if (max == 0) max = 100;

            float step = gW / labels.length;
            for (int i = 0; i < data.length && i < labels.length; i++) {
                float barH = (data[i] / (float) max) * gH;
                float left = pL + (i * step) + (step * 0.2f);
                float right = left + (step * 0.8f);
                float top = (gH + pT) - barH;

                // 1. Draw Bar
                p.setColor(color); 
                p.setAlpha(data[i] == 0 ? 40 : 255);
                canvas.drawRoundRect(new RectF(left, top, right, gH + pT), 12, 12, p);

                // 2. FIX: Show Count on Top of Bar (Dande ke upar)
                if (data[i] > 0) {
                    p.setAlpha(255);
                    p.setColor(wide ? Color.WHITE : Color.parseColor("GRAY")); // Dark color for Daily white background
                    p.setTextSize(24); 
                    p.setTypeface(Typeface.DEFAULT_BOLD);
                    p.setTextAlign(Paint.Align.CENTER);
                    // Dande ke thoda upar text draw karna
                    canvas.drawText(String.valueOf(data[i]), (left + right) / 2, top - 15, p);
                }

                // 3. X-Axis Labels (Time/Date)
                p.setColor(wide ? Color.WHITE : Color.GRAY);
                p.setTextSize(wide ? 22 : 20);
                p.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(labels[i], (left + right) / 2, getHeight() - 10, p);
            }
        }
    }
}

