package susemi2024.ssm2024.capstone;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.ssm2024.capstone.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class InfoActivity extends AppCompatActivity {
    private int id = 0;
    DBActivityHelper mDbOpenHelper;
    private String name = null, cate = null, memo = null, image = null;
    private int Lyear = 0, Lmonth = 0, Lday = 0, Ayear = 0, Amonth = 0, Aday = 0, usedate = 0,isChecked=0;
    private boolean checkBoxState = false;
    ImageView iv;
    Bitmap bm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        // 툴바 설정
        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        iv = findViewById(R.id.imageView);

        TextView Nname = findViewById(R.id.pName);
        TextView Ncate = findViewById(R.id.pCategory);
        TextView Ndate = findViewById(R.id.pDate);
        TextView Usedate = findViewById(R.id.pAlarm);
        TextView Nstartdate = findViewById(R.id.pStartDate);
        TextView Nmemo = findViewById(R.id.pMemo);

        Nmemo.setMovementMethod(new ScrollingMovementMethod());

        Intent intent = getIntent();
        id = intent.getIntExtra("id", 0);
        checkBoxState = intent.getBooleanExtra("checkBoxState", false);

        mDbOpenHelper = new DBActivityHelper(this);
        mDbOpenHelper.open();

        String[] columns = new String[]{
                DBActivity.COL_NAME, DBActivity.COL_CATE,
                DBActivity.COL_LYEAR, DBActivity.COL_LMONTH, DBActivity.COL_LDAY,
                DBActivity.COL_AYEAR, DBActivity.COL_AMONTH, DBActivity.COL_ADAY,
                DBActivity.COL_MEMO, DBActivity.COL_IMAGE,
                DBActivity.COL_USEDATE,DBActivity.COL_IS_CHECKED
        };

        Cursor cursor = mDbOpenHelper.select(columns, "_ID = " + id, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) { // moveToFirst()를 사용하여 첫 번째 행으로 이동
                name = cursor.getString(0);
                cate = cursor.getString(1);
                Lyear = cursor.getInt(2);
                Lmonth = cursor.getInt(3);
                Lday = cursor.getInt(4);
                Ayear = cursor.getInt(5);
                Amonth = cursor.getInt(6);
                Aday = cursor.getInt(7);
                memo = cursor.getString(8);
                image = cursor.getString(9);
                usedate = cursor.getInt(10);
                isChecked=cursor.getInt(11);

                // 디버그 로그 추가
                Log.d("InfoActivity", "name: " + name);
                Log.d("InfoActivity", "cate: " + cate);
                Log.d("InfoActivity", "Lyear: " + Lyear);
                Log.d("InfoActivity", "Lmonth: " + Lmonth);
                Log.d("InfoActivity", "Lday: " + Lday);
                Log.d("InfoActivity", "Ayear: " + Ayear);
                Log.d("InfoActivity", "Amonth: " + Amonth);
                Log.d("InfoActivity", "Aday: " + Aday);
                Log.d("InfoActivity", "memo: " + memo);
                Log.d("InfoActivity", "image: " + image);
                Log.d("InfoActivity", "usedate: " + usedate);
                Log.d("InfoActivity", "ischecked: " + isChecked);
            }
            cursor.close(); // cursor를 닫아줍니다.
        }
        if(isChecked==1){
            Nstartdate.setText("사용중");
        }
        else{
            Nstartdate.setText("미사용");
        }

        final String Ldate = Lyear + "/" + Lmonth + "/" + Lday;
        String Adate = Ayear + "/" + Amonth + "/" + Aday;

        Nname.setText(name);
        Ncate.setText(cate);
        Ndate.setText(Ldate);
        Nmemo.setText(memo);
        Usedate.setText(String.valueOf(usedate)); // int 값을 문자열로 변환하여 설정

        // 이미지 설정
        if (image == null) {
            iv.setImageResource(R.drawable.img_smol_put);
        } else if (image.indexOf("http") == -1) {
            iv.setImageURI(Uri.parse(image));
        } else {
            Thread mThread = new Thread() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(image);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setDoInput(true);
                        conn.connect();
                        InputStream is = conn.getInputStream();
                        bm = BitmapFactory.decodeStream(is);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            mThread.start();
            try {
                mThread.join();
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.info_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.modify:
                Intent intent = new Intent(getApplicationContext(), ModActivity.class);
                intent.putExtra("id", id);
                startActivityForResult(intent, 112);
                return true;

            case R.id.delete:
                // 바로 삭제
                DBActivityHelper dbHelper = new DBActivityHelper(this);
                dbHelper.open();

                // 카테고리와 수량을 가져오기
                String category = getCategoryById(id);
                int amount = getCategoryAmount(category);

                boolean isDeleted = dbHelper.deleteColumn(id);

                if (isDeleted) {
                    Toast.makeText(getApplicationContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show();

                    // 카테고리 수량 감소
                    dbHelper.updateCate(category, amount - 1);

                    Intent refresh = new Intent(this, MainActivity.class);
                    startActivity(refresh);
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), "삭제 실패: 항목을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                }

                dbHelper.close();
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getCategoryById(int itemId) {
        // 항목 ID로 카테고리를 가져오는 메소드 구현
        Cursor cursor = DBActivityHelper.mDB.query(DBActivity._TABLENAME,
                new String[]{DBActivity.COL_CATE},
                "_id = ?",
                new String[]{String.valueOf(itemId)},
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String category = cursor.getString(cursor.getColumnIndex(DBActivity.COL_CATE));
            cursor.close();
            return category;
        }
        return null;
    }

    private int getCategoryAmount(String category) {
        // 카테고리의 현재 수량을 가져오는 메소드 구현
        Cursor cursor = DBActivityHelper.mDB.query(DBActivity._TABLENAME2,
                new String[]{DBActivity.COL_AMOUNT},
                "category = ?",
                new String[]{category},
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int amount = cursor.getInt(cursor.getColumnIndex(DBActivity.COL_AMOUNT));
            cursor.close();
            return amount;
        }
        return 0;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 112 && resultCode == RESULT_OK) {
            Intent intent2 = getIntent();
            setResult(RESULT_OK, intent2);
            finish();
        }
    }
}
