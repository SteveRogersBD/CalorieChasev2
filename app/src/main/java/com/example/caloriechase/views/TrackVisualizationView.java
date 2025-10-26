package com.example.caloriechase.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.example.caloriechase.R;

/**
 * Custom view that displays a visual track representation based on distance
 */
public class TrackVisualizationView extends View {
    
    private Paint trackPaint;
    private Paint progressPaint;
    private Paint markerPaint;
    private Paint textPaint;
    private Paint backgroundPaint;
    
    private float distance = 1.0f; // Default 1km
    private int trackSegments = 10; // Number of segments to show
    private float trackWidth;
    private float trackHeight;
    private Path trackPath;
    private RectF trackBounds;
    
    public TrackVisualizationView(Context context) {
        super(context);
        init();
    }
    
    public TrackVisualizationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public TrackVisualizationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // Initialize paints
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setColor(ContextCompat.getColor(getContext(), R.color.surface_variant));
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(12f);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(ContextCompat.getColor(getContext(), R.color.primary_orange));
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(8f);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        
        markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerPaint.setColor(ContextCompat.getColor(getContext(), R.color.secondary_teal));
        markerPaint.setStyle(Paint.Style.FILL);
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        textPaint.setTextSize(24f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.FILL);
        
        trackPath = new Path();
        trackBounds = new RectF();
    }
    
    public void setDistance(float distance) {
        this.distance = distance;
        calculateTrackSegments();
        invalidate();
    }
    
    private void calculateTrackSegments() {
        // Calculate appropriate number of segments based on distance
        if (distance <= 2.0f) {
            trackSegments = 8;
        } else if (distance <= 5.0f) {
            trackSegments = 12;
        } else if (distance <= 10.0f) {
            trackSegments = 16;
        } else {
            trackSegments = 20;
        }
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        trackWidth = w - getPaddingLeft() - getPaddingRight();
        trackHeight = h - getPaddingTop() - getPaddingBottom();
        createTrackPath();
    }
    
    private void createTrackPath() {
        trackPath.reset();
        
        float padding = 40f;
        float availableWidth = trackWidth - (padding * 2);
        float availableHeight = trackHeight - (padding * 2);
        
        // Set bounds for the track area
        trackBounds.set(
            getPaddingLeft() + padding,
            getPaddingTop() + padding,
            getPaddingLeft() + padding + availableWidth,
            getPaddingTop() + padding + availableHeight
        );
        
        // Create a polygon track path instead of oval
        createPolygonPath();
        
        // Create gradient background
        float centerX = trackBounds.centerX();
        float centerY = trackBounds.centerY();
        float radius = Math.min(trackBounds.width(), trackBounds.height()) / 2;
        
        RadialGradient gradient = new RadialGradient(
            centerX, centerY, radius,
            new int[]{
                ContextCompat.getColor(getContext(), R.color.background_light),
                ContextCompat.getColor(getContext(), R.color.surface_variant)
            },
            new float[]{0.3f, 1.0f},
            Shader.TileMode.CLAMP
        );
        backgroundPaint.setShader(gradient);
    }
    
    private void createPolygonPath() {
        float centerX = trackBounds.centerX();
        float centerY = trackBounds.centerY();
        
        // Calculate polygon sides based on distance (same logic as map)
        int sides = calculatePolygonSides();
        
        // Calculate radius to fit within bounds
        float radius = Math.min(trackBounds.width(), trackBounds.height()) / 2.2f;
        
        // Create polygon vertices
        trackPath.reset();
        boolean firstPoint = true;
        
        for (int i = 0; i < sides; i++) {
            double angle = 2 * Math.PI * i / sides - Math.PI / 2; // Start from top
            
            float x = centerX + (float) (radius * Math.cos(angle));
            float y = centerY + (float) (radius * Math.sin(angle));
            
            if (firstPoint) {
                trackPath.moveTo(x, y);
                firstPoint = false;
            } else {
                trackPath.lineTo(x, y);
            }
        }
        
        trackPath.close();
    }
    
    private int calculatePolygonSides() {
        // Same logic as SessionMapActivity for consistency
        if (distance <= 1.0f) return 6;      // Hexagon for short distances
        else if (distance <= 2.0f) return 8;  // Octagon
        else if (distance <= 5.0f) return 10; // Decagon
        else if (distance <= 10.0f) return 12; // 12-sided
        else if (distance <= 20.0f) return 16; // 16-sided
        else return 20; // 20-sided for very long distances
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (trackPath.isEmpty()) {
            return;
        }
        
        // Draw gradient background
        canvas.drawOval(trackBounds, backgroundPaint);
        
        // Draw the base track
        canvas.drawPath(trackPath, trackPaint);
        
        // Draw distance markers
        drawDistanceMarkers(canvas);
        
        // Draw treasure indicators
        drawTreasureIndicators(canvas);
        
        // Draw start/finish line
        drawStartFinishLine(canvas);
        
        // Draw distance text in center
        drawCenterText(canvas);
    }
    
    private void drawDistanceMarkers(Canvas canvas) {
        float centerX = trackBounds.centerX();
        float centerY = trackBounds.centerY();
        float radius = Math.min(trackBounds.width(), trackBounds.height()) / 2.2f;
        
        int sides = calculatePolygonSides();
        
        // Draw markers at polygon vertices and midpoints
        for (int i = 0; i < sides; i++) {
            double angle = 2 * Math.PI * i / sides - Math.PI / 2; // Start from top
            
            // Calculate position on the polygon
            float x = centerX + (float) (radius * Math.cos(angle));
            float y = centerY + (float) (radius * Math.sin(angle));
            
            // Vertex markers (larger)
            Paint vertexPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            vertexPaint.setColor(ContextCompat.getColor(getContext(), R.color.vibrant_purple));
            canvas.drawCircle(x, y, 8f, vertexPaint);
            
            // Add midpoint markers for longer distances
            if (distance > 2.0f && i < sides - 1) {
                double nextAngle = 2 * Math.PI * (i + 1) / sides - Math.PI / 2;
                float midX = centerX + (float) (radius * Math.cos((angle + nextAngle) / 2));
                float midY = centerY + (float) (radius * Math.sin((angle + nextAngle) / 2));
                
                Paint midPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                midPaint.setColor(ContextCompat.getColor(getContext(), R.color.secondary_teal));
                canvas.drawCircle(midX, midY, 5f, midPaint);
            }
        }
    }
    
    private void drawTreasureIndicators(Canvas canvas) {
        float centerX = trackBounds.centerX();
        float centerY = trackBounds.centerY();
        float radius = Math.min(trackBounds.width(), trackBounds.height()) / 2.2f;
        
        // Calculate number of treasures based on distance (roughly 1 treasure per 0.5km)
        int treasureCount = Math.max(3, Math.min(12, (int)(distance * 2)));
        
        // Draw treasure indicators at random-ish positions inside polygon
        for (int i = 0; i < treasureCount; i++) {
            // Use a pseudo-random but consistent angle based on distance and index
            float angle = (float) (2 * Math.PI * (i + 0.3 * Math.sin(distance * i)) / treasureCount);
            
            // Calculate position inside the polygon (40% to 80% of radius)
            float treasureRadius = radius * (0.4f + 0.4f * (float)Math.sin(i * 0.7));
            float x = centerX + (float) (treasureRadius * Math.cos(angle));
            float y = centerY + (float) (treasureRadius * Math.sin(angle));
            
            // Draw treasure icon (simple diamond shape)
            Paint treasurePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            
            // Vary treasure colors based on position
            if (i % 6 == 0) {
                treasurePaint.setColor(ContextCompat.getColor(getContext(), R.color.vibrant_purple));
            } else if (i % 3 == 0) {
                treasurePaint.setColor(ContextCompat.getColor(getContext(), R.color.accent_blue));
            } else {
                treasurePaint.setColor(ContextCompat.getColor(getContext(), R.color.vibrant_yellow));
            }
            treasurePaint.setStyle(Paint.Style.FILL);
            
            // Draw diamond shape
            Path treasurePath = new Path();
            treasurePath.moveTo(x, y - 6f); // Top
            treasurePath.lineTo(x + 6f, y); // Right
            treasurePath.lineTo(x, y + 6f); // Bottom
            treasurePath.lineTo(x - 6f, y); // Left
            treasurePath.close();
            
            canvas.drawPath(treasurePath, treasurePaint);
            
            // Add small highlight
            Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            highlightPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
            canvas.drawCircle(x - 2f, y - 2f, 1.5f, highlightPaint);
        }
    }
    
    private void drawStartFinishLine(Canvas canvas) {
        float centerX = trackBounds.centerX();
        float centerY = trackBounds.centerY();
        float radius = Math.min(trackBounds.width(), trackBounds.height()) / 2.2f;
        
        // Start/finish at the top vertex of polygon (angle = -π/2)
        float startX = centerX;
        float startY = centerY - radius;
        
        // Draw start/finish marker
        Paint startPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        startPaint.setColor(ContextCompat.getColor(getContext(), R.color.vibrant_green));
        startPaint.setStyle(Paint.Style.FILL);
        
        canvas.drawCircle(startX, startY, 12f, startPaint);
        
        // Draw "START" text
        Paint startTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        startTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        startTextPaint.setTextSize(20f);
        startTextPaint.setTextAlign(Paint.Align.CENTER);
        startTextPaint.setFakeBoldText(true);
        
        canvas.drawText("START", startX, startY - 20f, startTextPaint);
    }
    
    private void drawCenterText(Canvas canvas) {
        float centerX = trackBounds.centerX();
        float centerY = trackBounds.centerY();
        
        // Draw distance text
        Paint distanceTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        distanceTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.primary_orange));
        distanceTextPaint.setTextSize(36f);
        distanceTextPaint.setTextAlign(Paint.Align.CENTER);
        distanceTextPaint.setFakeBoldText(true);
        
        String distanceText = String.format("%.1f km", distance);
        canvas.drawText(distanceText, centerX, centerY - 10f, distanceTextPaint);
        
        // Draw "TRACK" label
        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        labelPaint.setTextSize(18f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        
        canvas.drawText("TRACK", centerX, centerY + 20f, labelPaint);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = 300;
        int desiredHeight = 200;
        
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        
        int width;
        int height;
        
        // Measure width
        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(desiredWidth, widthSize);
        } else {
            width = desiredWidth;
        }
        
        // Measure height
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, heightSize);
        } else {
            height = desiredHeight;
        }
        
        setMeasuredDimension(width, height);
    }
}