package com.example;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MapCanvasView extends View {

    private Paint roadPaint;
    private Paint housePaint;
    private Paint routePaint;
    private Paint textPaint;
    private Paint markerPaint;
    private Paint backgroundPaint;

    // Map elements state
    private String selectedBlock = "F"; 
    private String selectedHouseNum = "05"; 
    private float courierX = 350f;
    private float courierY = 680f;

    // House Coordinates Model
    private static class House {
        String label;
        String block;
        float x;
        float y;
        boolean isSelected;
        
        House(String block, String label, float x, float y) {
            this.block = block;
            this.label = label;
            this.x = x;
            this.y = y;
            this.isSelected = false;
        }
    }

    private List<House> housesList = new ArrayList<>();

    public MapCanvasView(Context context) {
        super(context);
        init();
    }

    public MapCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Initialize paint systems
        roadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        roadPaint.setColor(Color.parseColor("#E0E6ED"));
        roadPaint.setStyle(Paint.Style.STROKE);
        roadPaint.setStrokeWidth(50f);
        roadPaint.setStrokeCap(Paint.Cap.ROUND);

        housePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        housePaint.setStyle(Paint.Style.FILL);

        routePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        routePaint.setColor(Color.parseColor("#FF5722")); // neon route
        routePaint.setStyle(Paint.Style.STROKE);
        routePaint.setStrokeWidth(12f);
        routePaint.setStrokeCap(Paint.Cap.ROUND);
        routePaint.setStrokeJoin(Paint.Join.ROUND);
        // Add animated path effect
        routePaint.setPathEffect(new DashPathEffect(new float[]{18f, 10f}, 0));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#374151"));
        textPaint.setTextSize(22f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerPaint.setStyle(Paint.Style.FILL);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#F9FAFB"));

        setupHouses();
    }

    private void setupHouses() {
        housesList.clear();
        // Setup coordinates for Block F (Jalan Mawar - Left Section)
        // Draw 8 houses on Block F left and right side of Jalan Mawar
        for (int i = 1; i <= 6; i++) {
            // Left row of Block F
            housesList.add(new House("F", "F-" + String.format("%02d", i), 100f, 100f + i * 85f));
            // Right row of Block F
            housesList.add(new House("F", "F-" + String.format("%02d", i + 6), 250f, 100f + i * 85f));
        }

        // Setup coordinates for Block G (Jalan Melati - Right Section)
        for (int i = 1; i <= 6; i++) {
            // Left row of Block G
            housesList.add(new House("G", "G-" + String.format("%02d", i), 450f, 100f + i * 85f));
            // Right row of Block G
            housesList.add(new House("G", "G-" + String.format("%02d", i + 6), 600f, 100f + i * 85f));
        }
    }

    public void updateDestination(String block, String houseNum) {
        this.selectedBlock = block.toUpperCase();
        // ensure nice format
        try {
            int num = Integer.parseInt(houseNum);
            this.selectedHouseNum = String.format("%02d", num);
        } catch (Exception e) {
            this.selectedHouseNum = houseNum;
        }

        for (House h : housesList) {
            h.isSelected = h.block.equals(selectedBlock) && h.label.endsWith(selectedHouseNum);
        }
        invalidate(); // Redraw map
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        // Draw soft ambient background
        canvas.drawRect(0, 0, width, height, backgroundPaint);

        // Core points positioning based on scale
        float centerX = width / 2f;
        float bottomY = height - 120f;
        float middleY = height / 2f + 40f;
        float topY = 80f;

        // Reset coords based on exact runtime dimensions for precision
        float leftAvenueX = centerX - 180f;
        float rightAvenueX = centerX + 180f;

        // 1. Draw Roads Vector Lines (Jalan-Jalan Utama)
        Path roadPath = new Path();
        // Entrance: Portal Utama
        roadPath.moveTo(centerX, height);
        roadPath.lineTo(centerX, middleY); // Main road
        // Left Branch: Jalan Mawar
        roadPath.lineTo(leftAvenueX, middleY);
        roadPath.lineTo(leftAvenueX, topY + 50f);
        // Right Branch: Jalan Melati
        roadPath.moveTo(centerX, middleY);
        roadPath.lineTo(rightAvenueX, middleY);
        roadPath.lineTo(rightAvenueX, topY + 50f);
        
        canvas.drawPath(roadPath, roadPaint);

        // Raw drawing helper for center line markings
        Paint centerLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerLinePaint.setColor(Color.WHITE);
        centerLinePaint.setStyle(Paint.Style.STROKE);
        centerLinePaint.setStrokeWidth(4f);
        centerLinePaint.setPathEffect(new DashPathEffect(new float[]{10f, 8f}, 0));
        canvas.drawPath(roadPath, centerLinePaint);

        // 2. Draw Landmark: Pos Siskamling (Main Entrance Gate)
        RectF posGate = new RectF(centerX - 95f, height - 100f, centerX - 15f, height - 40f);
        markerPaint.setColor(Color.parseColor("#4B5563")); // slate pos
        canvas.drawRoundRect(posGate, 12f, 12f, markerPaint);
        
        Paint gateTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gateTextPaint.setColor(Color.WHITE);
        gateTextPaint.setTextSize(14f);
        gateTextPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("POS RT 02", centerX - 55f, height - 65f, gateTextPaint);

        // 3. Draw Landmark: Balai RT & Lapangan (Top Center)
        RectF balaiRTBox = new RectF(centerX - 70f, 40f, centerX + 70f, 130f);
        markerPaint.setColor(Color.parseColor("#075E54"));
        canvas.drawRoundRect(balaiRTBox, 16f, 16f, markerPaint);
        canvas.drawText("BALAI RT", centerX, 80f, gateTextPaint);
        canvas.drawText("& LAPANGAN", centerX, 105f, gateTextPaint);

        // 4. Position and Draw houses based on runtime dimensions
        // Block F houses along left avenue (X coordinates scaled)
        float scaledF1 = leftAvenueX - 80f;
        float scaledF2 = leftAvenueX + 80f;
        float scaledG1 = rightAvenueX - 80f;
        float scaledG2 = rightAvenueX + 80f;

        // Redistribute houses coordinates to make map gorgeous
        housesList.clear();
        for (int i = 0; i < 6; i++) {
            float yPos = topY + 110f + i * 85f;
            housesList.add(new House("F", "F-" + String.format("%02d", i + 1), scaledF1, yPos));
            housesList.add(new House("F", "F-" + String.format("%02d", i + 7), scaledF2, yPos));

            housesList.add(new House("G", "G-" + String.format("%02d", i + 1), scaledG1, yPos));
            housesList.add(new House("G", "G-" + String.format("%02d", i + 7), scaledG2, yPos));
        }

        // Draw the houses
        for (House h : housesList) {
            h.isSelected = h.block.equals(selectedBlock) && h.label.endsWith(selectedHouseNum);

            // House card shadow
            RectF houseRect = new RectF(h.x - 30f, h.y - 25f, h.x + 30f, h.y + 25f);
            
            if (h.isSelected) {
                // Glow active target
                housePaint.setColor(Color.parseColor("#FFECEF"));
                canvas.drawRoundRect(new RectF(h.x - 36f, h.y - 31f, h.x + 36f, h.y + 31f), 10f, 10f, housePaint);
                
                housePaint.setColor(Color.parseColor("#E53E3E")); // Bright active target
            } else {
                housePaint.setColor(Color.parseColor("#FFFFFF"));
            }
            canvas.drawRoundRect(houseRect, 8f, 8f, housePaint);

            // Draw border
            Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(h.isSelected ? 5f : 2f);
            borderPaint.setColor(h.isSelected ? Color.parseColor("#E53E3E") : Color.parseColor("#CBD5E1"));
            canvas.drawRoundRect(houseRect, 8f, 8f, borderPaint);

            // House Label
            textPaint.setColor(h.isSelected ? Color.parseColor("#E53E3E") : Color.parseColor("#334155"));
            textPaint.setFakeBoldText(h.isSelected);
            textPaint.setTextSize(18f);
            canvas.drawText(h.label, h.x, h.y + 7f, textPaint);
        }

        // 5. Draw labels for streets
        Paint streetLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        streetLabelPaint.setColor(Color.parseColor("#64748B"));
        streetLabelPaint.setTextSize(18f);
        streetLabelPaint.setTextAlign(Paint.Align.CENTER);
        streetLabelPaint.setFakeBoldText(true);

        canvas.save();
        canvas.rotate(-90f, leftAvenueX, middleY + 120f);
        canvas.drawText("◀ JALAN MAWAR (BLOK F) ◀", leftAvenueX, middleY + 120f, streetLabelPaint);
        canvas.restore();

        canvas.save();
        canvas.rotate(90f, rightAvenueX, middleY - 120f);
        canvas.drawText("▶ JALAN MELATI (BLOK G) ▶", rightAvenueX, middleY - 120f, streetLabelPaint);
        canvas.restore();

        canvas.drawText("JALAN UTAMA PORTAL", centerX, middleY + 100f, streetLabelPaint);

        // 6. DRAW COURIER NEON ROUTE PATH
        Path courierRoute = new Path();
        courierRoute.moveTo(centerX, height); // starts outside
        courierRoute.lineTo(centerX, middleY); // goes up to split point

        // Now calculate path to target house
        House targetHouse = null;
        for (House h : housesList) {
            if (h.isSelected) {
                targetHouse = h;
                break;
            }
        }

        if (targetHouse != null) {
            float avenueX = targetHouse.block.equals("F") ? leftAvenueX : rightAvenueX;
            // Go from split to street avenue
            courierRoute.lineTo(avenueX, middleY);
            // Go along street to house y level
            courierRoute.lineTo(avenueX, targetHouse.y);
            // Enter the house logic
            courierRoute.lineTo(targetHouse.x, targetHouse.y);

            // Draw orange layout line
            canvas.drawPath(courierRoute, routePaint);

            // Update courier visual marker position coordinates directly in simulation
            courierX = targetHouse.x;
            courierY = targetHouse.y;
        } else {
            // default resting position at local pos ronda
            courierX = centerX;
            courierY = middleY + 150f;
        }

        // 7. Draw Courier Marker Symbol (🛵 representation or cool pulsing marker)
        Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pulsePaint.setColor(Color.parseColor("#10B981")); // Courier Emerald Green
        pulsePaint.setStyle(Paint.Style.FILL);
        
        // draw glowing outer ring
        Paint pulseOuter = new Paint(Paint.ANTI_ALIAS_FLAG);
        pulseOuter.setColor(Color.parseColor("#3010B981"));
        pulseOuter.setStyle(Paint.Style.FILL);
        canvas.drawCircle(courierX, courierY, 28f, pulseOuter);
        canvas.drawCircle(courierX, courierY, 15f, pulsePaint);

        // Marker tag label
        Paint courierLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        courierLabelPaint.setColor(Color.WHITE);
        courierLabelPaint.setTextSize(15f);
        courierLabelPaint.setTextAlign(Paint.Align.CENTER);
        courierLabelPaint.setFakeBoldText(true);
        canvas.drawText("🛵", courierX, courierY + 5f, courierLabelPaint);

        // Draw start point tag
        canvas.drawCircle(centerX, height - 10f, 12f, pulsePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float clickX = event.getX();
            float clickY = event.getY();

            // Check if resident clicked close to a house block
            for (House h : housesList) {
                float distSq = (h.x - clickX) * (h.x - clickX) + (h.y - clickY) * (h.y - clickY);
                if (distSq < 1600f) { // 40dp close radius
                    updateDestination(h.block, h.label.substring(2));
                    if (onBlockSelectionListener != null) {
                        onBlockSelectionListener.onBlockSelected(h.block, h.label.substring(2));
                    }
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    public interface OnBlockSelectionListener {
        void onBlockSelected(String block, String houseNum);
    }

    private OnBlockSelectionListener onBlockSelectionListener;

    public void setOnBlockSelectionListener(OnBlockSelectionListener listener) {
        this.onBlockSelectionListener = listener;
    }
}
