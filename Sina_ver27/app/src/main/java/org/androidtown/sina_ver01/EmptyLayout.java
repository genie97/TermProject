package org.androidtown.sina_ver01;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.List;

public class EmptyLayout extends AppCompatActivity implements TextEditorDialogFragment.OnTextLayerCallback {
    DiaryData data = new DiaryData();
    private ImageButton save_Btn;
    protected MotionView motionView;
    protected View textEntityEditPanel;
    ImageButton textwrite, paint, erase, picture, location;
    private LinearLayout container;

    EmptyView emptyView;

    private Uri mImageCaptureUri;
    // ImageView image;
    protected String absolutePath;
    Bitmap photo;
    private int emotion;
    private String subtitle;
    String place_name;

    TextView mDisplayDate;
    Button mDisplayWeather;
    DatePickerDialog.OnDateSetListener mDateListener;
    Calendar date;

    private static final int PICK_FROM_CAMERA = 0;
    private static final int PICK_FROM_ALBUM = 1;
    private static final int CROP_FROM_IMAGE = 2;
    private static final int PLACE_PICKER_REQUEST = 3; //지도

    final MotionView.MotionViewCallback motionViewCallback = new MotionView.MotionViewCallback() {
        @Override
        public void onEntitySelected(@Nullable MotionEntity entity) {
            textEntityEditPanel.setVisibility(View.VISIBLE);
        }

        @Override
        public void onEntityDoubleTap(@NonNull MotionEntity entity) {
            startTextEntityEditing();
        }
    };

    private FontProvider fontProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty_layout);
        checkPermission();
        /*감정 번들 풀기*/
        Intent myIntent = getIntent();
        Bundle myBundle = myIntent.getExtras();
        int emo_flag = myBundle.getInt("emo_num");
        String edit = myBundle.getString("subtitle");
        /*******************************************************/
        //Log.i("넘어온값","? " + emo_flag);
        emotion = emo_flag;
        subtitle = edit;
        data.setEmotion(emotion);

        mDisplayDate = (TextView) findViewById(R.id.Draw_Date);
        mDisplayWeather = (Button) findViewById(R.id.Draw_Weather);

        //Play Music
        Intent intent1 = new Intent(EmptyLayout.this, MusicService.class);
        intent1.putExtra(MusicService.MESSEAGE_KEY, true);

        Bundle myBundle1 = new Bundle();
        myBundle1.putInt("emo_num", emotion);
        intent1.putExtras(myBundle);
        startService(intent1);

        date = Calendar.getInstance();

        calendar();


        picture = (ImageButton) findViewById(R.id.upload_btn);
/*
        viewPager = (ImageView)findViewById(R.id.view);
        adapter = new Adapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setVisibility(View.INVISIBLE);

        image=(ImageView)findViewById(R.id.view);
        image.setVisibility(View.INVISIBLE);
*/
        try {
            this.fontProvider = new FontProvider(getResources());
            container = (LinearLayout) findViewById(R.id.main_container);
            emptyView = (EmptyView) findViewById(R.id.emptyView);
            motionView = (MotionView) findViewById(R.id.main_motion_view);
            textEntityEditPanel = (View) findViewById(R.id.main_motion_text_entity_edit_panel);
            textEntityEditPanel.bringToFront();

            motionView.setMotionViewCallback(motionViewCallback);
            textwrite = (ImageButton) findViewById(R.id.textwrite);
            textwrite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addTextSticker();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

        save_Btn = (ImageButton) findViewById(R.id.save_btn);

        save_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                container.buildDrawingCache();
                Bitmap captureView = container.getDrawingCache();

                FileOutputStream fos;
                try {
                    fos = new FileOutputStream(Environment.getExternalStorageDirectory().toString() + "/capture.jpeg");
                    captureView.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                    //Stop Music
                    Intent intent5 = new Intent(getApplicationContext(), MusicService.class); // 이동할 컴포넌트
                    stopService(intent5);

                    finish();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                //Toast.makeText(getApplicationContext(), "파일이 저장되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });


        paint = (ImageButton) findViewById(R.id.paint);

        paint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePaintEntityColor();
            }
        });

        erase = (ImageButton) findViewById(R.id.erase);

        erase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emptyView.reset();
                Toast.makeText(getApplicationContext(), "erase Button", Toast.LENGTH_SHORT).show();
            }
        });


        picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogInterface.OnClickListener albumListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doTakeAlbumAction();
                    }
                };
                new AlertDialog.Builder(EmptyLayout.this).setTitle("사진앨범").setPositiveButton("앨범선택", albumListener).show();

            }
        });

        location = (ImageButton) findViewById(R.id.location_btn);

        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogInterface.OnClickListener gpsListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Place();
                    }
                };
                new AlertDialog.Builder(EmptyLayout.this).setTitle("나의 위치").setPositiveButton("위치선택", gpsListener).show();
            }
        });
        initTextEntitiesListeners();
    }

    private void initTextEntitiesListeners() {
        findViewById(R.id.text_entity_font_size_increase).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                increaseTextEntitySize();
            }
        });
        findViewById(R.id.text_entity_font_size_decrease).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                decreaseTextEntitySize();
            }
        });
        findViewById(R.id.text_entity_color_change).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // motionView.changePenColor();
                changeTextEntityColor();
            }
        });
        findViewById(R.id.text_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delete();
            }
        });
        findViewById(R.id.text_entity_edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTextEntityEditing();
            }
        });
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case android.view.KeyEvent.KEYCODE_BACK:
                exit();
        }
        return false;
    }

    public void exit() {
        //Stop Music
        Intent intent1 = new Intent(EmptyLayout.this, MusicService.class);
        intent1.putExtra(MusicService.MESSEAGE_KEY, false);
        stopService(intent1);
        finish();
    }

    private void increaseTextEntitySize() {
        TextEntity textEntity = currentTextEntity();
        if (textEntity != null) {
            textEntity.getLayer().getFont().increaseSize(TextLayer.Limits.FONT_SIZE_STEP);
            textEntity.updateEntity();
            motionView.invalidate();
        }
    }

    private void decreaseTextEntitySize() {
        TextEntity textEntity = currentTextEntity();
        if (textEntity != null) {
            textEntity.getLayer().getFont().decreaseSize(TextLayer.Limits.FONT_SIZE_STEP);
            textEntity.updateEntity();
            motionView.invalidate();
        }
    }

    private void changeTextEntityColor() {

        TextEntity textEntity = currentTextEntity();
        if (textEntity == null) {
            return;
        }

        int initialColor = textEntity.getLayer().getFont().getColor();

        ColorPickerDialogBuilder
                .with(EmptyLayout.this)
                .setTitle(R.string.select_color)
                .initialColor(initialColor)
                .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                .density(8) // magic number
                .setPositiveButton(R.string.ok, new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                        TextEntity textEntity = currentTextEntity();
                        if (textEntity != null) {
                            textEntity.getLayer().getFont().setColor(selectedColor);
                            textEntity.updateEntity();
                            emptyView.setColor(selectedColor);
                            motionView.invalidate();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .build()
                .show();
    }

    private void changePaintEntityColor() {

        int initialColor = Color.BLACK;

        ColorPickerDialogBuilder
                .with(EmptyLayout.this)
                .setTitle(R.string.select_color)
                .initialColor(initialColor)
                .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                .density(8) // magic number
                .setPositiveButton(R.string.ok, new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                        emptyView.setColor(selectedColor);
                        motionView.invalidate();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .build()
                .show();
    }

    private void changeTextEntityFont() {
        final List<String> fonts = fontProvider.getFontNames();
        FontsAdapter fontsAdapter = new FontsAdapter(this, fonts, fontProvider);
        new AlertDialog.Builder(this)
                .setTitle(R.string.select_font)
                .setAdapter(fontsAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        TextEntity textEntity = currentTextEntity();
                        if (textEntity != null) {
                            textEntity.getLayer().getFont().setTypeface(fonts.get(which));
                            textEntity.updateEntity();
                            motionView.invalidate();
                        }
                    }
                })
                .show();
    }

    private void startTextEntityEditing() {
        TextEntity textEntity = currentTextEntity();
        if (textEntity != null) {
            TextEditorDialogFragment fragment0 = TextEditorDialogFragment.getInstance(textEntity.getLayer().getText());
            fragment0.show(getFragmentManager(), TextEditorDialogFragment.class.getName());
        }
    }

    @Nullable
    private TextEntity currentTextEntity() {
        if (motionView != null && motionView.getSelectedEntity() instanceof TextEntity) {
            return ((TextEntity) motionView.getSelectedEntity());
        } else {
            return null;
        }
    }

    /*
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            super.onCreateOptionsMenu(menu);
            getMenuInflater().inflate(R.menu.empty_menu, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getItemId() == R.id.main_add_text) {
                addTextSticker();
            }
            return super.onOptionsItemSelected(item);
        }
    */
    protected void addTextSticker() {
        TextLayer textLayer = createTextLayer();
        TextEntity textEntity;
        try {
            textEntity = new TextEntity(textLayer,
                    motionView.getWidth(),
                    motionView.getHeight(),
                    fontProvider);
            motionView.addEntityAndPosition(textEntity);


            // move text sticker up so that its not hidden under keyboard
            PointF center = textEntity.absoluteCenter();
            center.y = center.y * 0.5F;
            textEntity.moveCenterTo(center);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // redraw
        motionView.invalidate();

        startTextEntityEditing();
    }

    protected void addPlaceSticker(String place) {
        TextLayer textLayer = createTextLayer();
        TextEntity textEntity;
        try {
            textLayer.setText(place_name);
            textEntity = new TextEntity(textLayer,
                    motionView.getWidth(),
                    motionView.getHeight(),
                    fontProvider);
            motionView.addEntityAndPosition(textEntity);


            // move text sticker up so that its not hidden under keyboard
            PointF center = textEntity.absoluteCenter();
            center.y = center.y * 0.5F;
            textEntity.moveCenterTo(center);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // redraw
        motionView.invalidate();
    }


    private TextLayer createTextLayer() {
        TextLayer textLayer = new TextLayer();
        Font font = new Font();

        font.setColor(TextLayer.Limits.INITIAL_FONT_COLOR);
        font.setSize(TextLayer.Limits.INITIAL_FONT_SIZE);
        font.setTypeface(fontProvider.getDefaultFontName());

        textLayer.setFont(font);

        if (BuildConfig.DEBUG) {
            textLayer.setText("Text 입력하세요 :))");
        }

        return textLayer;
    }

    @Override
    public void textChanged(@NonNull String text) {
        TextEntity textEntity = currentTextEntity();
        if (textEntity != null) {
            TextLayer textLayer = textEntity.getLayer();
            if (!text.equals(textLayer.getText())) {
                textLayer.setText(text);
                textEntity.updateEntity();
                motionView.invalidate();
            }
        }
    }

    public void doTakeAlbumAction() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, PICK_FROM_ALBUM);

    }

    private void storeCropImage(Bitmap bitmap, String filePath) {
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SmartWheel";
        File directory_SmartWheel = new File(dirPath);

        if (!directory_SmartWheel.exists())
            directory_SmartWheel.mkdir();

        File copyFile = new File(filePath);
        BufferedOutputStream out;

        try {
            copyFile.createNewFile();
            out = new BufferedOutputStream(new FileOutputStream(copyFile));
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(copyFile)));

            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK)
            return;

        switch (requestCode) {
            case PICK_FROM_ALBUM: {
                mImageCaptureUri = data.getData();
                Log.d("SmartWheel", mImageCaptureUri.getPath().toString());
            }

            case PICK_FROM_CAMERA: {
                Intent intent = new Intent("com.android.camera.action.CROP");
                intent.setDataAndType(mImageCaptureUri, "image/*");

                intent.putExtra("outputX", 1080);
                intent.putExtra("outputY", 900);
                intent.putExtra("aspectX", 4);
                intent.putExtra("aspectY", 3);
                intent.putExtra("scale", true);
                intent.putExtra("return-data", true);
                startActivityForResult(intent, CROP_FROM_IMAGE);
                break;
            }

            case CROP_FROM_IMAGE: {
                if (resultCode != RESULT_OK) {
                    return;
                }

                //image = findViewById(R.id.view);
                final Bundle extras = data.getExtras();

                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SmartWheel/" + System.currentTimeMillis() + ".jpg";

                if (extras != null) {
                    photo = extras.getParcelable("data");
                    //image.setVisibility(R);
                    //image.setImageBitmap(photo);

                    storeCropImage(photo, filePath);
                    absolutePath = filePath;

                    Layer layer = new Layer();
                    ImageEntity entity = new ImageEntity(layer, photo, motionView.getWidth(), motionView.getHeight());
                    motionView.addEntityAndPosition(entity);
                    motionView.invalidate();
                    break;
                }

                File f = new File(mImageCaptureUri.getPath());
                if (f.exists()) {
                    f.delete();
                }
            }
            case PLACE_PICKER_REQUEST: {
                final Place place = PlacePicker.getPlace(this, data);
                final CharSequence name = place.getName();
                place_name = (String)name;
                addPlaceSticker(place_name);
            }
        }
    }

    void checkPermission() {
        int permissioninfo = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissioninfo == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "SDCard 쓰기 권한 있음", Toast.LENGTH_SHORT).show();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(getApplicationContext(), "권한의 필요성 설명", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            }
        }
    }

    void Place() {
        PlacePicker.IntentBuilder intentBuilder = new PlacePicker.IntentBuilder();
        try {
            Intent intent = intentBuilder.build(EmptyLayout.this);
            startActivityForResult(intent, PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    public void calendar() {
        mDisplayWeather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final CharSequence[] items = {"맑음", "눈 / 비", "흐림"};
                AlertDialog.Builder builder = new AlertDialog.Builder(EmptyLayout.this);     // 여기서 this는 Activity의 this

                // 여기서 부터는 알림창의 속성 설정
                builder.setTitle("날씨를 선택하세요")        // 제목 설정
                        .setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {    // 목록 클릭시 설정
                            public void onClick(DialogInterface dialog, int index) {
                                mDisplayWeather.setText(items[index]);
                                mDisplayWeather.setTextColor(Color.GRAY);
                                Toast.makeText(getApplicationContext(), items[index], Toast.LENGTH_SHORT).show();
                                dialog.cancel();
                            }
                        });

                AlertDialog dialog = builder.create();    // 알림창 객체 생성
                dialog.show();    // 알림창 띄우기

            }
        });
        mDisplayDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar cal = Calendar.getInstance();

                DatePickerDialog dialog = new DatePickerDialog(EmptyLayout.this, android.R.style.Theme_Holo_Light_Dialog_MinWidth, mDateListener, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });
        mDateListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                Log.d("DrawLayout", "onDataSet:yyyy/mm/dd:" + year + "년" + month + "월" + dayOfMonth + "일");

                date.set(year, month, dayOfMonth);
                mDisplayDate.setText(Utils.calToString(date.getTime()));
            }
        };

    }

    public void delete() {
        motionView.deletedSelectedEntity();
    }
}