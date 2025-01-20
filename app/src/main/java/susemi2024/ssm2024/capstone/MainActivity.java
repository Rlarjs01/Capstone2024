package susemi2024.ssm2024.capstone;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.ssm2024.capstone.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
{
    RecyclerView rv;
    RecyclerView categoryRv;
    MainAdapter adapter;
    su_CategoryAdapter2 categoryAdapter;
    ItemTouchHelper itemTouchHelper;
    Spinner spinner;
    DBActivityHelper mDbOpenHelper;
    BarAdapter mBarDbOpenHelper;
    private String sel = null;
    private ArrayList<Product> allItems = new ArrayList<>();
    private ArrayList<Product> remainItems = new ArrayList<>();
    private ArrayList<Product> goneItmes = new ArrayList<>();
    private int amount = 0;
    public int sort = 0;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "onCreate() called");
        mDbOpenHelper = new DBActivityHelper(this);
        mDbOpenHelper.open();
        mDbOpenHelper.create();

        mBarDbOpenHelper = new BarAdapter(this);
        mBarDbOpenHelper.createDatabase();
        mBarDbOpenHelper.open();

        SharedPreferences pref = getSharedPreferences("checkFirst", MainActivity.MODE_PRIVATE);
        boolean checkFirst = pref.getBoolean("checkFirst", false);
        if (!checkFirst)
        {
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("checkFirst", true);
            editor.commit();

            mDbOpenHelper.insertCate("육류", 0);
            mDbOpenHelper.insertCate("해산물", 0);
            mDbOpenHelper.insertCate("음료", 0);
            mDbOpenHelper.insertCate("조미료", 0);
            mDbOpenHelper.insertCate("야채", 0);
            mDbOpenHelper.insertCate("냉동식품", 0);
            mDbOpenHelper.insertSort(sort);
        }

        // 툴바
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);

        // 정렬
        final String[] sort_opt = getResources().getStringArray(R.array.sort_list);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, sort_opt);
        spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setAdapter(arrayAdapter);

        String[] columns = new String[]{DBActivity.COL_SORT};
        Cursor cursor1 = mDbOpenHelper.selectSort(columns, null, null, null, null, null);
        if(cursor1 != null)
        {
            while (cursor1.moveToNext())
            {
                sort = cursor1.getInt(0);
            }
        }
        spinner.setSelection(sort);

        // 제품 리사이클러뷰 설정
        rv = findViewById(R.id.ProductRecycle);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1);
        rv.setLayoutManager(layoutManager);

        adapter = new MainAdapter(this);

        // 제품 목록 설정
        initItemList();
        itemListToAdapter(allItems);

        // 아이템 드래그 적용
        ItemTouchHelperCallback callback = new ItemTouchHelperCallback((ItemTouchHelperCallback.OnItemMoveListener) adapter);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(rv);

        rv.setAdapter(adapter);

        // 제품 선택 시 제품정보창으로 이동
        adapter.setOnItemClickListener(new OnProductItemClickListener()
        {
            @Override
            public void onItemClick(MainAdapter.ViewHolder holder, View view, int position)
            {
                Product item = adapter.getItem(position);

                Intent intent = new Intent(getApplicationContext(), InfoActivity.class);
                intent.putExtra("id", adapter.getItem(position).primaryKey);

                startActivityForResult(intent, 111);
            }
        });

        // 카테고리 리사이클러뷰 설정
        categoryRv = findViewById(R.id.CategoryRecycle);
        GridLayoutManager categoryLayoutManager = new GridLayoutManager(this, 3);
        categoryRv.setLayoutManager(categoryLayoutManager);

        categoryAdapter = new su_CategoryAdapter2(this);
        categoryRv.setAdapter(categoryAdapter);

        // DB에서 카테고리 가져오기, 최대 3개만 추가
        String[] categoryColumns = new String[]{DBActivity.COL_CATE};
        Cursor categoryCursor = mDbOpenHelper.selectCate(categoryColumns, null, null, null, null, null);
        if (categoryCursor != null) {
            int count = 0;
            while (categoryCursor.moveToNext() && count < 3) {
                String categoryName = categoryCursor.getString(0);
                categoryAdapter.addCategory(new su_Category(categoryName));
                count++;
            }
            categoryCursor.close();
        }

        // 카테고리 선택 시 해당 제품들 목록창으로 이동
        categoryAdapter.setOnItemClickListener(new OnCategoryItemClickListener2() {
            @Override
            public void onItemClick(su_CategoryAdapter2.ViewHolder holder, View view, int position) {
                su_Category item = categoryAdapter.getItem(position);
                Toast.makeText(getApplicationContext(), "카테고리 선택됨 : " + item.getName(), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(getApplicationContext(), SelectedCategoryActivity.class);
                intent.putExtra("category_name", item.getName());
                startActivityForResult(intent, 112);
            }
        });

        // 하단 메뉴
        BottomNavigationView bottomNavigationView = findViewById(R.id.mainNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener()
        {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item)
            {
                switch (item.getItemId())
                {
                    case R.id.home:
                    {
                        Intent intent2 = new Intent(getApplicationContext(), MainActivity.class);
                        intent2.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivityForResult(intent2, 103);
                        break;
                    }

                    case R.id.addProduct:
                    {
                        Intent intent = new Intent(getApplicationContext(), AddActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivityForResult(intent, 102);
                        break;
                    }

                    case R.id.category:
                    {
                        Intent intent2 = new Intent(getApplicationContext(), su_CategoryActivity.class);
                        intent2.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivityForResult(intent2, 101);
                        finish();
                        break;
                    }
                }
                return true;
            }
        });

        Log.d("MainActivity", "onCreate() completed");
    }

    // allItems 리스트 초기화
    protected void initItemList()
    {
        Log.d("MainActivity", "initItemList() called");
        if (allItems != null && !allItems.isEmpty())
            allItems.clear();

        String[] columns = new String[]{DBActivity.COL_ID,DBActivity.COL_NAME,DBActivity.COL_CATE
                , DBActivity.COL_LYEAR, DBActivity.COL_LMONTH, DBActivity.COL_LDAY
                , DBActivity.COL_AYEAR, DBActivity.COL_AMONTH, DBActivity.COL_ADAY
                , DBActivity.COL_COM, DBActivity.COL_MEMO, DBActivity.COL_IMAGE
                , DBActivity.COL_BARCATE, DBActivity.COL_USEDATE, DBActivity.COL_EXPIRATION_DATE};

        Cursor cursor = mDbOpenHelper.select(columns, null, null, null, null, null);

        if (cursor != null)
        {
            while (cursor.moveToNext())
            {
                int id = cursor.getInt(0);
                String productName = cursor.getString(1);
                String category = cursor.getString(2);
                int lifeYear = cursor.getInt(3);
                int lifeMonth = cursor.getInt(4);
                int lifeDay = cursor.getInt(5);
                String company = cursor.getString(9);
                String image = cursor.getString(11);
                String barcategory = cursor.getString(12);
                int usedate = cursor.getInt(13);
                String expirationdate=cursor.getString(14);


                allItems.add(new Product(id,productName, category, company, lifeYear, lifeMonth, lifeDay, image, barcategory, usedate,expirationdate));
                Log.d("cursor", cursor.getString(0));
            }
            cursor.close();
        } else {
            Log.d("MainActivity", "Cursor is null");
        }
        Log.d("MainActivity", "initItemList() completed");

        divideItemList();
    }

    // MainAdapter에 있는 item리스트 초기화하고 list를 adapter에 있는 제품리스트에 반영
    protected void itemListToAdapter(ArrayList<Product> list)
    {
        Log.d("MainActivity", "itemListToAdapter() called");
        if (adapter.items != null && !adapter.items.isEmpty())
            adapter.items.clear();

        for (int i = 0; i < list.size(); ++i)
        {
            adapter.addProduct(list.get(i));
        }
        adapter.notifyDataSetChanged(); // 변경사항 즉시 반영
        Log.d("MainActivity", "itemListToAdapter() completed");
    }

    // 유통기한 지난 제품과 남은 제품들 나누기
    protected void divideItemList()
    {
        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        Date time = new Date();
        String today = format1.format(time);
        int compareDate;

        if (remainItems != null && !remainItems.isEmpty())
            remainItems.clear();
        if (goneItmes != null && !goneItmes.isEmpty())
            goneItmes.clear();

        for (int i = 0; i < allItems.size(); ++i)
        {
            compareDate = today.compareTo(allItems.get(i).getDate());

            if (compareDate <= 0)
            {
                Log.d("date", today + " <= " +allItems.get(i).getDate());
                allItems.get(i).isPassed = false;
                remainItems.add(allItems.get(i));
            }
            else
            {
                allItems.get(i).setIsPassed();
                goneItmes.add(allItems.get(i));
            }
        }
    }

    // 상단 툴바
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);

        return true;
    }

    // 상단 정렬 메뉴 선택
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.show_all:
                itemListToAdapter(allItems);
                rv.setAdapter(adapter);
                break;
            case R.id.show_remain:
                itemListToAdapter(remainItems);
                rv.setAdapter(adapter);
                break;
            case R.id.show_pass:
                itemListToAdapter(goneItmes);
                rv.setAdapter(adapter);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 102)
        {
            if (resultCode == RESULT_OK)
            {
                initItemList();
                itemListToAdapter(allItems);
                adapter.notifyDataSetChanged();

                if (sort == 0) adapter.nameAsc();
                else if (sort == 1) adapter.nameDsc();
                else if (sort == 2) adapter.dateAsc();
                else adapter.dateDsc();
            }
        }
        else if (requestCode == 111)
        {
            if (resultCode == RESULT_OK)
            {
                initItemList();
                itemListToAdapter(allItems);
                adapter.notifyDataSetChanged();
            }
        }
        else if (requestCode == 112)
        {
            categoryAdapter.notifyDataSetChanged();
        }
    }

    // 뒤로가기 버튼 클릭시
    private long time = 0;
    @Override
    public void onBackPressed()
    {
        if (System.currentTimeMillis() - time >= 2000)
        {
            time = System.currentTimeMillis();
            Toast.makeText(getApplicationContext(), "한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
        }
        else if (System.currentTimeMillis() - time < 2000)
        {
            mDbOpenHelper.close();
            finish();
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        saveState();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        restoreState();
    }

    protected void saveState()
    {
        SharedPreferences pref = getSharedPreferences("main", Activity.MODE_PRIVATE);
    }

    protected void restoreState()
    {
        SharedPreferences pref = getSharedPreferences("main", Activity.MODE_PRIVATE);
    }
}