package com.zybooks.paintbynumber;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class ImageSelectionActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_ID = "com.zybooks.paintbynumber.image_id";
    public static final String EXTRA_GRID_SIZE = "com.zybooks.paintbynumber.grid_size";
    public static final String EXTRA_COLOR_COUNT = "com.zybooks.paintbynumber.color_count";

    private GridView mGridView;
    private ArrayList<Integer> mImageIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_selection);

        mGridView = findViewById(R.id.image_grid_view);
        loadImageIds();

        ImageAdapter adapter = new ImageAdapter(this, mImageIds);
        mGridView.setAdapter(adapter);

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int selectedImageId = mImageIds.get(position);
                showGameOptionsDialog(selectedImageId);
            }
        });
    }

    private void showGameOptionsDialog(final int selectedImageId) {
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_game_options, null);


        final SeekBar gridSizeSlider = dialogView.findViewById(R.id.grid_size_slider);
        final TextView gridSizeValueText = dialogView.findViewById(R.id.grid_size_value_text);
        final SeekBar colorCountSlider = dialogView.findViewById(R.id.color_count_slider);
        final TextView colorCountValueText = dialogView.findViewById(R.id.color_count_value_text);


        final int MIN_GRID_SIZE = 10;
        final int MIN_COLOR_COUNT = 2;


        gridSizeValueText.setText(String.valueOf(gridSizeSlider.getProgress() + MIN_GRID_SIZE));

        gridSizeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                gridSizeValueText.setText(String.valueOf(progress + MIN_GRID_SIZE));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        colorCountValueText.setText(String.valueOf(colorCountSlider.getProgress() + MIN_COLOR_COUNT));

        colorCountSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                colorCountValueText.setText(String.valueOf(progress + MIN_COLOR_COUNT));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });



        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setTitle("Game Options")
                .setPositiveButton("Start", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        int gridSize = gridSizeSlider.getProgress() + MIN_GRID_SIZE;
                        int colorCount = colorCountSlider.getProgress() + MIN_COLOR_COUNT;


                        launchGame(selectedImageId, gridSize, colorCount);
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void launchGame(int imageId, int gridSize, int colorCount) {
        Intent intent = new Intent(ImageSelectionActivity.this, GameActivity.class);
        intent.putExtra(EXTRA_IMAGE_ID, imageId);
        intent.putExtra(EXTRA_GRID_SIZE, gridSize);
        intent.putExtra(EXTRA_COLOR_COUNT, colorCount);
        startActivity(intent);
    }

    private void loadImageIds() {
        Field[] drawables = R.drawable.class.getFields();
        for (Field field : drawables) {
            try {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                        field.getType().equals(int.class)) {
                    String name = field.getName();
                    if (!name.startsWith("abc_") && !name.startsWith("ic_launcher") && !name.startsWith("design_") && !name.startsWith("material_") && !name.startsWith("notification_")) {
                        mImageIds.add(field.getInt(null));
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
