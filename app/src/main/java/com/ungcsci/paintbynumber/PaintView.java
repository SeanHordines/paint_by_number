package com.ungcsci.paintbynumber;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import android.view.ScaleGestureDetector;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PaintView extends View {

    public static final String EXTRA_IMAGE_URI = "com.ungcsci.paintbynumber.image_uri";

    //Game state
    private int selectedColorNumber = 1;
    private boolean showNumbers = true;
    private boolean isComplete = false;
    private boolean showCompletionOverlay = false;

    //drawing
    private Paint paint, textPaint, borderPaint, overlayPaint, paletteBackgroundPaint;

    //color palette
    private int paletteY;
    private final int paletteSize = 100;
    private final int paletteSpacing = 20;
    private final int paletteRows = 2;

    //buttons
    private Button admireButton, mainMenuButton, shareButton;
    private ImageButton drawModeButton, zoomModeButton;

    //mode enum
    private enum Mode { DRAW, ZOOM }
    private Mode currentMode = Mode.DRAW;

    //zoom & pan
    private Matrix canvasMatrix = new Matrix();
    private float scaleFactor = 1f;
    private final float minScale = 1f, maxScale = 4f;
    private float lastTouchX, lastTouchY;
    private int activePointerId = -1;
    private ScaleGestureDetector scaleDetector;
    private float maxTransX, maxTransY, minTransX, minTransY;
    private final float panPadding = 100f;

    //painting
    private int grid_size;
    private int[] colorPalette;
    private int[][] numberGrid, userPaintGrid;
    public Bitmap finished_image;

    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.GRAY);
        borderPaint.setStrokeWidth(2);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        overlayPaint = new Paint();
        overlayPaint.setColor(Color.argb(180, 0, 0, 0));

        paletteBackgroundPaint = new Paint();
        paletteBackgroundPaint.setColor(Color.argb(200, 220, 220, 220));

        // ensures that the painting grid is properly centered on inflation
        getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            updatePanLimits();
            invalidate();
        });

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (currentMode != Mode.ZOOM) return false;

                float prevScale = scaleFactor;
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(minScale, Math.min(scaleFactor, maxScale));

                //zoom on pinch focus
                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();

                canvasMatrix.postScale(scaleFactor / prevScale, scaleFactor / prevScale, focusX, focusY);
                updatePanLimits();
                constrainMatrix();
                invalidate();
                return true;
            }
        });
    }

    public void loadImageData(int size, int[] palette, int[][] grid, Bitmap posterized_image){
        grid_size = size;
        numberGrid = new int[grid_size][grid_size];
        userPaintGrid = new int[grid_size][grid_size];
        colorPalette = new int[palette.length + 1];
        colorPalette[0] = 0xFFFFFFFF;
        System.arraycopy(palette, 0, colorPalette, 1, palette.length);

        for (int r = 0; r < grid_size; r++) {
            for (int c = 0; c < grid_size; c++) {
                int color = grid[r][c];
                int paletteIndex = -1;
                for (int i = 0; i < palette.length; i++) {
                    if (palette[i] == color) {
                        paletteIndex = i;
                        break;
                    }
                }
                numberGrid[c][r] = paletteIndex + 1;
            }
        }
        finished_image = posterized_image;
    }

    private void updatePanLimits() {
        if (numberGrid == null || grid_size <= 0) return;

        int width = getWidth();
        int height = getHeight() - paletteSize * paletteRows - paletteSpacing * (paletteRows + 1);

        int cellSize = Math.min(width / grid_size, height / grid_size);
        float scaledWidth = cellSize * grid_size * scaleFactor;
        float scaledHeight = cellSize * grid_size * scaleFactor;

        //compute base offsets for centering when zoomed out
        float baseOffsetX = 0;
        float baseOffsetY = 0;

        if (scaledWidth < width) baseOffsetX = (width - scaledWidth) / 2f;
        if (scaledHeight < height) baseOffsetY = (height - scaledHeight) / 2f;

        //set matrix to start with centered position if not zoomed/panned
        float[] values = new float[9];
        canvasMatrix.getValues(values);
        float currentScale = values[Matrix.MSCALE_X]; //assume uniform scale

        //if no zoom yet (scale == 1) or first time, reset translation to center
        if (currentScale == 1f) {
            canvasMatrix.reset();
            canvasMatrix.postTranslate(baseOffsetX, baseOffsetY);
        }

        if (scaledWidth > width) {
            minTransX = width - scaledWidth - panPadding;
            maxTransX = panPadding;
        } else {
            // Still center, but allow some sway
            minTransX = baseOffsetX - panPadding;
            maxTransX = baseOffsetX + panPadding;
        }

        if (scaledHeight > height) {
            minTransY = height - scaledHeight - panPadding;
            maxTransY = panPadding;
        } else {
            minTransY = baseOffsetY - panPadding;
            maxTransY = baseOffsetY + panPadding;
        }


        //constrain matrix in case translation went out of bounds
        constrainMatrix();
    }

    private void constrainMatrix() {
        float[] values = new float[9];
        canvasMatrix.getValues(values);

        float transX = Math.max(minTransX, Math.min(values[Matrix.MTRANS_X], maxTransX));
        float transY = Math.max(minTransY, Math.min(values[Matrix.MTRANS_Y], maxTransY));

        canvasMatrix.setValues(new float[]{
                values[Matrix.MSCALE_X], 0, transX,
                0, values[Matrix.MSCALE_Y], transY,
                0, 0, 1
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (numberGrid == null || grid_size <= 0) return;

        int width = getWidth();
        int height = getHeight();
        int paletteAreaHeight = paletteSize * paletteRows + paletteSpacing * (paletteRows + 1);

        int availableHeight = height - paletteAreaHeight;
        int cellSize = Math.min(width / grid_size, availableHeight / grid_size);

        //draw grid
        canvas.save();
        canvas.concat(canvasMatrix);

        textPaint.setTextSize(cellSize * 0.6f);

        for (int r = 0; r < grid_size; r++) {
            for (int c = 0; c < grid_size; c++) {
                int x = c * cellSize;
                int y = r * cellSize;

                int paintedNum = userPaintGrid[r][c];
                int correctNum = numberGrid[r][c];

                paint.setColor(paintedNum == 0 ? Color.WHITE : colorPalette[paintedNum]);
                canvas.drawRect(x, y, x + cellSize, y + cellSize, paint);
                canvas.drawRect(x, y, x + cellSize, y + cellSize, borderPaint);

                if (paintedNum != 0 && paintedNum != correctNum) {
                    Paint errorBorder = new Paint();
                    errorBorder.setColor(Color.RED);
                    errorBorder.setStyle(Paint.Style.STROKE);
                    errorBorder.setStrokeWidth(6);
                    canvas.drawRect(x, y, x + cellSize, y + cellSize, errorBorder);
                }

                if (showNumbers) {
                    Paint textShadow = new Paint(textPaint);
                    textShadow.setColor(Color.WHITE);
                    canvas.drawText(String.valueOf(correctNum),
                            x + cellSize / 2f + 2,
                            y + cellSize / 1.7f + 2,
                            textShadow);
                    canvas.drawText(String.valueOf(correctNum),
                            x + cellSize / 2f,
                            y + cellSize / 1.7f,
                            textPaint);
                }
            }
        }
        canvas.restore();

        //draw palette background
        int paletteTop = height - paletteAreaHeight;
        canvas.drawRect(0, paletteTop, width, height, paletteBackgroundPaint);

        // draw color palette now centered
        textPaint.setTextSize(paletteSize * 0.5f);
        int colorsPerRow = 8;

        // new smart width wow
        int itemWidth = paletteSize + paletteSpacing;

        for (int i = 1; i < colorPalette.length; i++) {
            int row = (i - 1) / colorsPerRow;
            int col = (i - 1) % colorsPerRow;

            // how many colors are actually in this row?
            int totalColorsThisRow = Math.min(
                    colorsPerRow,
                    colorPalette.length - 1 - row * colorsPerRow
            );

            // total width of this row
            int rowWidth = totalColorsThisRow * paletteSize
                    + (totalColorsThisRow + 1) * paletteSpacing;

            // horizontally centered start position
            int startX = (width - rowWidth) / 2 + paletteSpacing;

            int x = startX + col * (paletteSize + paletteSpacing);
            int y = paletteTop + paletteSpacing + row * (paletteSize + paletteSpacing);

            paint.setColor(colorPalette[i]);
            canvas.drawRect(x, y, x + paletteSize, y + paletteSize, paint);

            borderPaint.setColor(Color.DKGRAY);
            canvas.drawRect(x, y, x + paletteSize, y + paletteSize, borderPaint);

            if (i == selectedColorNumber) {
                borderPaint.setColor(Color.BLACK);
                borderPaint.setStrokeWidth(6);
                canvas.drawRect(x - 4, y - 4, x + paletteSize + 4, y + paletteSize + 4, borderPaint);
                borderPaint.setStrokeWidth(2);
            }

            Paint textShadow = new Paint(textPaint);
            textShadow.setColor(Color.WHITE);
            canvas.drawText(String.valueOf(i),
                    x + paletteSize / 2f + 2,
                    y + paletteSize / 1.6f + 2,
                    textShadow);
            canvas.drawText(String.valueOf(i),
                    x + paletteSize / 2f,
                    y + paletteSize / 1.6f,
                    textPaint);
        }

        if (showCompletionOverlay) {
            canvas.drawRect(0, 0, width, height, overlayPaint);

            Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
            text.setColor(Color.WHITE);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(80);
            text.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            canvas.drawText("Painting Complete!", width / 2f, height / 2f, text);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //always pass the event to the scale detector for zoom gestures
        scaleDetector.onTouchEvent(event);

        int action = event.getActionMasked();

        if (currentMode == Mode.ZOOM) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    activePointerId = event.getPointerId(0);
                    break;
                case MotionEvent.ACTION_MOVE:
                    int pointerIndex = event.findPointerIndex(activePointerId);
                    if (pointerIndex < 0) break;

                    float x = event.getX(pointerIndex);
                    float y = event.getY(pointerIndex);
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;

                    canvasMatrix.postTranslate(dx, dy);
                    constrainMatrix();
                    invalidate();

                    lastTouchX = x;
                    lastTouchY = y;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    activePointerId = -1;
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    int pointerIndexReleased = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                            >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                    int pointerId = event.getPointerId(pointerIndexReleased);
                    if (pointerId == activePointerId) {
                        int newPointerIndex = pointerIndexReleased == 0 ? 1 : 0;
                        lastTouchX = event.getX(newPointerIndex);
                        lastTouchY = event.getY(newPointerIndex);
                        activePointerId = event.getPointerId(newPointerIndex);
                    }
                    break;
            }
            return true;
        }

        //DRAW MODE
        float rawX = event.getX();
        float rawY = event.getY();

        //check palette first (always in screen coordinates)
        int width = getWidth();
        int height = getHeight();
        int paletteAreaHeight = paletteSize * paletteRows + paletteSpacing * (paletteRows + 1);
        int paletteTop = height - paletteAreaHeight;
        int colorsPerRow = 8;

        for (int i = 1; i < colorPalette.length; i++) {
            int row = (i - 1) / colorsPerRow;
            int col = (i - 1) % colorsPerRow;

            // how many colors in this row
            int totalColorsThisRow = Math.min(
                    colorsPerRow,
                    colorPalette.length - 1 - row * colorsPerRow
            );

            int rowWidth = totalColorsThisRow * paletteSize
                    + (totalColorsThisRow + 1) * paletteSpacing;

            int startX = (width - rowWidth) / 2 + paletteSpacing;

            int x = startX + col * (paletteSize + paletteSpacing);
            int y = paletteTop + paletteSpacing + row * (paletteSize + paletteSpacing);

            if (rawX >= x && rawX <= x + paletteSize && rawY >= y && rawY <= y + paletteSize) {
                selectedColorNumber = i;
                invalidate();
                return true;
            }
        }

        if (rawY >= paletteTop) {
            return true;
        }

        //convert touch to grid coordinates using inverse matrix
        float[] touchPoint = new float[]{rawX, rawY};
        Matrix inverse = new Matrix();
        canvasMatrix.invert(inverse);
        inverse.mapPoints(touchPoint);
        float gridX = touchPoint[0];
        float gridY = touchPoint[1];

        int availableHeight = height - paletteAreaHeight;
        int cellSize = Math.min(width / grid_size, availableHeight / grid_size);

        int col = (int)(gridX / cellSize);
        int row = (int)(gridY / cellSize);

        if (row >= 0 && row < grid_size && col >= 0 && col < grid_size) {
            if (userPaintGrid[row][col] != selectedColorNumber) {
                userPaintGrid[row][col] = selectedColorNumber;
                checkForCompletion();
                invalidate();
            }
        }

        return true;
    }

    private void checkForCompletion() {
        for (int r = 0; r < grid_size; r++) {
            for (int c = 0; c < grid_size; c++) {
                if (userPaintGrid[r][c] != numberGrid[r][c]) {
                    isComplete = false;
                    showCompletionOverlay = false;
                    if (admireButton != null) admireButton.setVisibility(GONE);
                    if (mainMenuButton != null) mainMenuButton.setVisibility(GONE);
                    if (shareButton != null) shareButton.setVisibility(GONE);
                    return;
                }
            }
        }
        if (!isComplete) {
            isComplete = true;
            showCompletionOverlay = true;
            if (admireButton != null) admireButton.setVisibility(VISIBLE);
            if (mainMenuButton != null) mainMenuButton.setVisibility(VISIBLE);
            if (shareButton != null) shareButton.setVisibility(VISIBLE);
            invalidate();
        }
    }

    public void toggleNumbers() {
        showNumbers = !showNumbers;
        invalidate();
    }

    @SuppressLint("WrongThread")
    public Uri saveBitmapToCache(Bitmap bitmap, Context context) throws IOException {
        File cacheDir = new File(context.getCacheDir(), "images");
        if (!cacheDir.exists()) cacheDir.mkdirs();

        File file = new File(cacheDir, "finished_image.png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }

        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
    }

    //xml button setters
    public void setAdmireButton(Button b, Activity parentActivity) {
        admireButton = b;
        admireButton.setOnClickListener(v -> {
            Uri imageUri;
            try {
                imageUri = this.saveBitmapToCache(finished_image, this.getContext());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Intent intent = new Intent(parentActivity, AdmireImageActivity.class);
            intent.putExtra(EXTRA_IMAGE_URI, imageUri.toString());
            parentActivity.startActivity(intent);
        });
    }

    public void setMainMenuButton(Button b) {
        mainMenuButton = b;
        mainMenuButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), MainMenuActivity.class);
            getContext().startActivity(intent);
        });
    }

    public void setShareButton(Button b) {
        shareButton = b;
        shareButton.setOnClickListener(v -> {
            Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            draw(canvas);

            try {
                File cachePath = new File(getContext().getCacheDir(), "images");
                cachePath.mkdirs();
                File file = new File(cachePath, "painting.png");
                FileOutputStream stream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                stream.close();

                Uri contentUri = saveBitmapToCache(finished_image, this.getContext());

                if (contentUri != null) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    shareIntent.setType("image/png");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out my finished painting on the Pixel Painter app!");
                    getContext().startActivity(Intent.createChooser(shareIntent, "Share your painting via"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void setDrawModeButton(ImageButton b) {
        drawModeButton = b;
        drawModeButton.setOnClickListener(v -> {
            currentMode = Mode.DRAW;
            drawModeButton.setAlpha(1f);
            if (zoomModeButton != null) zoomModeButton.setAlpha(0.5f);
        });
    }

    public void setZoomModeButton(ImageButton b) {
        zoomModeButton = b;
        zoomModeButton.setOnClickListener(v -> {
            currentMode = Mode.ZOOM;
            zoomModeButton.setAlpha(1f);
            if (drawModeButton != null) drawModeButton.setAlpha(0.5f);
        });
    }

}