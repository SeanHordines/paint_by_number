package com.zybooks.paintbynumber;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;


public class PaintView extends View {
    //grid
    private final int rows = 8;
    private final int columns = 8;

    //hard-coded number grid (to be replaced)
    private int[][] numberGrid;

    //hard-coded user painting grid (to be replaced)
    private int[][] userPaintGrid;

    //game state
    private int selectedColorNumber = 1;
    private boolean showNumbers = true;
    private boolean isComplete = false;
    private boolean showCompletionOverlay = false;

    //drawing
    private Paint paint;
    private Paint textPaint;
    private Paint borderPaint;
    private Paint overlayPaint;

    //color palette
    private int paletteY;
    private final int paletteSize = 100;
    private final int paletteSpacing = 20;

    //buttons
    private Button admireButton;
    private Button mainMenuButton;
    private Button shareButton;

    //hard-coded color values (for now)
    private int[] colorPalette = {
            Color.WHITE,
            Color.rgb(43, 63, 149),
            Color.rgb(130, 140, 69),
            Color.rgb(44, 51, 59),
            Color.rgb(114, 136, 150),
            Color.rgb(21, 30, 73),
            Color.rgb(167, 182, 161),
            Color.rgb(89, 120, 174),
            Color.rgb(56, 80, 118)
    };

    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);

        //hard-coded color grid (for now)
        numberGrid = new int[][]{
                {1, 5, 2, 1, 1, 1, 3, 7},
                {1, 2, 7, 4, 7, 3, 1, 6},
                {8, 4, 3, 3, 4, 4, 7, 6},
                {3, 8, 7, 4, 8, 8, 8, 6},
                {3, 8, 6, 7, 4, 6, 6, 6},
                {3, 3, 8, 5, 3, 3, 8, 8},
                {3, 3, 3, 5, 3, 3, 3, 3},
                {3, 3, 3, 3, 3, 3, 3, 3}
        };
        userPaintGrid = new int[rows][columns];

        //initialize paint objects
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
    }

    //xml button setters
    public void setAdmireButton(Button b) {
        admireButton = b;
        admireButton.setOnClickListener(v -> {
            showCompletionOverlay = false;
            admireButton.setVisibility(GONE);
            mainMenuButton.setVisibility(GONE);
            invalidate();
        });
    }

    public void setMainMenuButton(Button b) {
        mainMenuButton = b;
        mainMenuButton.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(getContext(), MainMenuActivity.class);
            getContext().startActivity(intent);
        });
    }

    public void setShareButton(Button b) {
        shareButton = b;
        shareButton.setOnClickListener(v -> shareCompletedPainting());
    }

    @SuppressLint("WrongThread")
    private void shareCompletedPainting() {
        // Create a bitmap of the current view
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);

        try {
            // Save the bitmap to the cache directory
            File cachePath = new File(getContext().getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "painting.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            // Get file URI using FileProvider (defined in manifest)
            Uri contentUri = FileProvider.getUriForFile(
                    getContext(),
                    getContext().getPackageName() + ".fileprovider",
                    file
            );

            if (contentUri != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setType("image/png"); // ← missing semicolon fixed
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out my finished painting on the Paint By Number App!");
                getContext().startActivity(Intent.createChooser(shareIntent, "Share your painting via"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int paletteHeight = paletteSize + 40;
        int availableHeight = height - paletteHeight;
        int cellSize = Math.min(width / columns, availableHeight / rows);
        int xOffset = (width - (cellSize * columns)) / 2;
        int yOffset = (availableHeight - (cellSize * rows)) / 2;

        textPaint.setTextSize(cellSize * 0.6f);

        // draw painting grid by iterating through the 2d array
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                int x = xOffset + c * cellSize;
                int y = yOffset + r * cellSize;

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

        // draw the palette
        paletteY = height - paletteHeight + 20;
        int totalWidth = (paletteSize + paletteSpacing) * 8 - paletteSpacing;
        int startX = (width - totalWidth) / 2;

        textPaint.setTextSize(paletteSize * 0.5f); // adjust for palette size

        for (int i = 1; i <= 8; i++) {
            int x = startX + (i - 1) * (paletteSize + paletteSpacing);

            // draw palette color
            paint.setColor(colorPalette[i]);
            canvas.drawRect(x, paletteY, x + paletteSize, paletteY + paletteSize, paint);

            // draw palette border
            borderPaint.setColor(Color.DKGRAY);
            canvas.drawRect(x, paletteY, x + paletteSize, paletteY + paletteSize, borderPaint);

            // highlight the selected color
            if (i == selectedColorNumber) {
                borderPaint.setColor(Color.BLACK);
                borderPaint.setStrokeWidth(6);
                canvas.drawRect(x - 4, paletteY - 4, x + paletteSize + 4, paletteY + paletteSize + 4, borderPaint);
                borderPaint.setStrokeWidth(2);
            }

            // draw the corresponding number on each color square
            Paint textShadow = new Paint(textPaint);
            textShadow.setColor(Color.WHITE);
            canvas.drawText(String.valueOf(i), x + paletteSize / 2f + 2, paletteY + paletteSize / 1.6f + 2, textShadow);
            canvas.drawText(String.valueOf(i), x + paletteSize / 2f, paletteY + paletteSize / 1.6f, textPaint);
        }


        // painting complete logic
        if (showCompletionOverlay) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

            Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
            text.setColor(Color.WHITE);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(80);
            text.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

            canvas.drawText("Painting Complete!", getWidth() / 2f, getHeight() / 2f, text);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int width = getWidth();
        int height = getHeight();
        int paletteHeight = paletteSize + 40;
        int availableHeight = height - paletteHeight;
        int cellSize = Math.min(width / columns, availableHeight / rows);
        int xOffset = (width - (cellSize * columns)) / 2;
        int yOffset = (availableHeight - (cellSize * rows)) / 2;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < columns; c++) {
                        int cx = xOffset + c * cellSize;
                        int cy = yOffset + r * cellSize;
                        if (x >= cx && x < cx + cellSize && y >= cy && y < cy + cellSize) {
                            if (userPaintGrid[r][c] != selectedColorNumber) {
                                userPaintGrid[r][c] = selectedColorNumber;
                                checkForCompletion();
                                invalidate();
                            }
                            return true;
                        }
                    }
                }

                int totalWidth = (paletteSize + paletteSpacing) * 8 - paletteSpacing;
                int startX = (width - totalWidth) / 2;
                for (int i = 1; i <= 8; i++) {
                    int px = startX + (i - 1) * (paletteSize + paletteSpacing);
                    int py = paletteY;
                    if (x >= px && x <= px + paletteSize && y >= py && y <= py + paletteSize) {
                        selectedColorNumber = i;
                        invalidate();
                        return true;
                    }
                }
                break;
        }
        return true;
    }

    private void checkForCompletion() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                if (userPaintGrid[r][c] != numberGrid[r][c]) {
                    isComplete = false;
                    showCompletionOverlay = false;
                    if (admireButton != null) admireButton.setVisibility(GONE);
                    if (mainMenuButton != null) mainMenuButton.setVisibility(GONE);
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
}