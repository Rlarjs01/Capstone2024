package susemi2024.ssm2024.capstone;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ssm2024.capstone.R;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class AddActivity extends AppCompatActivity {
    private ImageView iv;
    private Spinner spinner1;
    private CheckBox checkBox;
    private TextView text5, text6;
    private EditText text1, text3;
    DBActivityHelper mDbOpenHelper;
    BarAdapter mBarDbOpenHelper;

    private int year = 0, month = 0, day = 0, usedate = 0;
    private int Ayear = 0, Amonth = 0, Aday = 0;
    private String category = null, name = null, memo = null, barcategory = null;
    private String photoPath = null;
    private int amount = 0, isChecked=0;

    private int isChecked1=0;
    private Calendar calendar;
    private String expirationDate1=null, expirationDate=null;
    Bitmap bm;

    public void resetDatabase(Context context) {
        context.deleteDatabase("User.db");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);




        mDbOpenHelper = new DBActivityHelper(this);
        mDbOpenHelper.open();

        mBarDbOpenHelper = new BarAdapter(this);
        mBarDbOpenHelper.open();

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //뒤로가기 버튼
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        checkSelfPermission();
        iv = findViewById(R.id.imageView);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, 101);
            }
        });

        text1 = findViewById(R.id.editText);
        text1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                name = text1.getText().toString(); //제품명 추출
            }
        });

        text3 = findViewById(R.id.editText3);
        text3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            public void afterTextChanged(Editable editable) {
                memo = text3.getText().toString(); //메모 추출
                Log.d("AddActivity", "Memo after text changed: " + memo); // 로그 추가
            }
        });

        spinner1 = findViewById(R.id.spinner);
        String[] columns = new String[]{DBActivity.COL_CATE};
        ArrayList<String> cates = new ArrayList<>();
        Cursor cursor = mDbOpenHelper.selectCate(columns, null, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String cate = cursor.getString(0);
                cates.add(cate);
            }
        }
        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cates);
        spinner1.setAdapter(adapter1); //카테고리 배열과 어댑터 연결

        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                category = spinner1.getSelectedItem().toString(); //category 추출
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        calendar = Calendar.getInstance(); // 캘린더 객체를 통해 현재 년월일 추출
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH); //월은 0월 부터 11월까지
        day = calendar.get(Calendar.DAY_OF_MONTH);

        text5 = findViewById(R.id.text5);
        text6 = findViewById(R.id.text6);
        checkBox = findViewById(R.id.checkBox);

        // 등록 날짜를 오늘 날짜로 설정
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
        String todayDate = dateFormat.format(Calendar.getInstance().getTime());
        text5.setText(todayDate);

        // CheckBox에 리스너 추가
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    text6.setText(expirationDate1);
                    text6.setEnabled(true);
                    setAlarm();
                } else {
                    expirationDate = text6.getText().toString();
                    text6.setText("");
                    text6.setEnabled(false);
                }
            }
        });
        text6.setEnabled(checkBox.isChecked());

        // 바코드 인식 버튼 누르면 스캐너 실행
        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentIntegrator integrator = new IntentIntegrator(AddActivity.this);
                integrator.setBeepEnabled(false);
                integrator.setCaptureActivity(CustomScannerActivity.class);
                integrator.initiateScan();
            }
        });
    }

    private void setAlarm() {
        if (expirationDate1 != null && !expirationDate1.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar expCalendar = Calendar.getInstance();
                expCalendar.setTime(sdf.parse(expirationDate1));

                expCalendar.set(Calendar.HOUR_OF_DAY, 8);
                expCalendar.set(Calendar.MINUTE, 30);
                expCalendar.set(Calendar.SECOND, 0);

                // 현재일보다 이전이면 등록 실패
                if (expCalendar.before(Calendar.getInstance())) {
                    Toast.makeText(this, "해당 날짜 이후로 알람을 설정해 주세요", Toast.LENGTH_LONG).show();
                    return;
                }

                // Receiver 설정
                Intent intent = new Intent(this, AlarmReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                // Toast 보여주기 (알람 시간 표시)
                SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
                Toast.makeText(this, expirationDate1 + " AM 08:30에 PUSH", Toast.LENGTH_LONG).show();

                NotificationSomething(expCalendar);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "유통기한이 설정되지 않았습니다", Toast.LENGTH_LONG).show();
        }
    }

    private void NotificationSomething(Calendar calendar) {
        PackageManager pm = this.getPackageManager();

        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //알람 설정
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    //권한에 대한 응답이 있을때 작동하는 함수
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //권한을 허용 했을 경우
        if (requestCode == 1) {
            int length = permissions.length;
            for (int i = 0; i < length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    // 동의
                    Log.d("MainActivity", "권한 허용 : " + permissions[i]);
                }
            }
        }
    }

    public void checkSelfPermission() {

        String temp = "";
        //파일 읽기 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.READ_EXTERNAL_STORAGE + " ";
        }

        //파일 쓰기 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.WRITE_EXTERNAL_STORAGE + " ";
        }

        if (!TextUtils.isEmpty(temp)) {
            // 권한 요청
            ActivityCompat.requestPermissions(this, temp.trim().split(" "), 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK) {
            try {
                InputStream is = getContentResolver().openInputStream(data.getData());
                Uri photoUri = data.getData();
                photoPath = getRealPathFromURI(this, photoUri);
                Bitmap bm = BitmapFactory.decodeStream(is);
                is.close();
                iv.setImageBitmap(bm);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (requestCode == 101 && resultCode == RESULT_CANCELED) {
            // 취소
        } else if (resultCode == Activity.RESULT_OK) {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            String msg = scanResult.getContents();
            String barcode = msg;
            Log.d("onActivityResult", "onActivityResult: ." + msg);
            getDateFromBarcodeDB(barcode);
        }
    }

    // 스캔한 바코드로 DB에서 데이터 가져오기
    protected void getDateFromBarcodeDB(String barcode) {
        String[] columns = new String[]{BarDBActivity.COL_BARCODE, BarDBActivity.COL_BARNAME, BarDBActivity.COL_BARCOM, BarDBActivity.COL_BARIMAGE, BarDBActivity.COL_BARCATEGORY, BarDBActivity.COL_BARUSEDATE};

        Cursor cursor = mBarDbOpenHelper.selectBar(columns, BarDBActivity.COL_BARCODE + " = " + barcode, null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                name = cursor.getString(1);
                photoPath = cursor.getString(3);
                usedate = cursor.getInt(5); // usedate 값을 데이터베이스에서 가져오기
            }
        }
        cursor.close();
        text1.setText(name);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, usedate);

        int newYear = calendar.get(Calendar.YEAR);
        int newMonth = calendar.get(Calendar.MONTH) + 1; // 월은 0부터 시작하므로 +1 해줘야 함
        int newDay = calendar.get(Calendar.DAY_OF_MONTH);


        expirationDate1 = newYear + "년" + newMonth + "월" + newDay +"일 까지";


        Ayear = newYear;
        Amonth = newMonth;
        Aday = newDay;

        if (checkBox.isChecked()) {
            text6.setText(expirationDate1);

        }

        // 이미지 불러오기
        Thread mThread = new Thread() {
            @Override
            public void run() {
                try {
                    URL url = new URL(photoPath);

                    // Web에서 이미지를 가져온 뒤 ImageView에 지정할 Bitmap을 만든다
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true); // 서버로부터 응답 수신
                    conn.connect();

                    InputStream is = conn.getInputStream(); // InputStream 값 가져오기
                    bm = BitmapFactory.decodeStream(is); // Bitmap으로 변환
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        mThread.start(); // Thread 실행
        try {
            // 메인 Thread는 별도의 작업 Thread가 작업을 완료할 때까지 대기해야 한다
            // join()을 호출하여 별도의 작업 Thread가 종료될 때까지 메인 Thread가 기다리게 한다
            mThread.join();

            // 작업 Thread에서 이미지를 불러오는 작업을 완료한 뒤
            // UI 작업을 할 수 있는 메인 Thread에서 ImageView에 이미지를 지정한다
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    iv.setImageBitmap(bm);
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_menu, menu);


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: { //toolbar의 back키 눌렀을 때 동작
                finish();
                return true;
            }
            // 추가 완료 눌렀을 때
            case R.id.complete: {
                if (name == null) {
                    Toast.makeText(getApplicationContext(), "제품 이름을 입력하세요", Toast.LENGTH_LONG).show();
                } else {

                    if(checkBox.isChecked()){
                        isChecked=1;

                        expirationDate=expirationDate1;
                    }

                    Log.d("AddActivity", "Expiration Date to be inserted: " + expirationDate);
                    // 여기서 usedate 값 확인을 위한 로그 추가
                    Log.d("AddActivity", "Name: " + name);
                    Log.d("AddActivity", "Category: " + category);
                    Log.d("AddActivity", "Year: " + year);
                    Log.d("AddActivity", "Month: " + (month + 1));
                    Log.d("AddActivity", "Day: " + day);
                    Log.d("AddActivity", "Ayear: " + Ayear);
                    Log.d("AddActivity", "Amonth: " + Amonth);
                    Log.d("AddActivity", "Aday: " + Aday);
                    Log.d("AddActivity", "Memo: " + memo);
                    Log.d("AddActivity", "PhotoPath: " + photoPath);
                    Log.d("AddActivity", "isChecked " + isChecked);
                    Log.d("AddActivity", "ExpirationDate: " + expirationDate);
                    Log.d("AddActivity", "Usedate: " + usedate); //




                    mDbOpenHelper.insertColumn(name, category, year, month + 1, day, Ayear, Amonth, Aday, null, memo, photoPath,expirationDate,usedate,isChecked);
                    String[] columns = new String[]{DBActivity.COL_AMOUNT};
                    Cursor cursor = mDbOpenHelper.selectCate(columns, "category = '" + category + "'", null, null, null, null);
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            amount = cursor.getInt(0);
                        }
                    }

                    mDbOpenHelper.updateCate(category, amount + 1);
                    Intent intent = new Intent(this, MainActivity.class);
                    Product product = new Product(name, category, null, year, month, day, photoPath);
                    intent.putExtra("product", product);
                    setResult(RESULT_OK, intent);
                    finish();

                    Intent refresh = new Intent(this, MainActivity.class);
                    startActivity(refresh);

                    finish();

                    return true;

                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public static String getRealPathFromURI(final Context context, final Uri uri) {
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                } else {
                    String SDcardpath = getRemovableSDCardPath(context).split("/Android")[0];
                    return SDcardpath + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static String getRemovableSDCardPath(Context context) {
        File[] storages = ContextCompat.getExternalFilesDirs(context, null);
        if (storages.length > 1 && storages[0] != null && storages[1] != null)
            return storages[1].toString();
        else
            return "";
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}