package susemi2024.ssm2024.capstone;

import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ssm2024.capstone.R;

import java.util.ArrayList;

public class su_CategoryAdapter2 extends RecyclerView.Adapter<su_CategoryAdapter2.ViewHolder> {

    ArrayList<su_Category> items = new ArrayList<>();
    OnCategoryItemClickListener2 listener;
    Context mContext;
    DBActivityHelper mDbOpenHelper;
    private int amount = 0;
    private int addCheck = 0;
    private String cate = null;

    public su_CategoryAdapter2(Context context) {
        mContext = context;
        mDbOpenHelper = new DBActivityHelper(context); // DBActivityHelper 초기화
        mDbOpenHelper.open();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView1;
        TextView textView2;
        ImageButton imageButton;
        Context mCnt;
        DBActivityHelper mDbOpenHelper;
        private int amount = 0;
        private String cate = null;

        public ViewHolder(View itemView, final OnCategoryItemClickListener2 listener, DBActivityHelper dbOpenHelper) {
            super(itemView);

            mCnt = itemView.getContext();
            mDbOpenHelper = dbOpenHelper;
            textView1 = itemView.findViewById(R.id.nameOfCategory);
            textView2 = itemView.findViewById(R.id.numberOfItems);
            imageButton = itemView.findViewById(R.id.imageButton);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (listener != null && position != RecyclerView.NO_POSITION)
                        listener.onItemClick(ViewHolder.this, v, position);
                }
            });
        }

        public void setItem(su_Category item) {
            cate = item.getName();
            textView1.setText(cate);
            String[] columns = new String[]{DBActivity.COL_AMOUNT};
            Cursor cursor = mDbOpenHelper.selectCate(columns, "category = '" + cate + "'", null, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    amount = cursor.getInt(0);
                }
                cursor.close(); // 리소스 누수를 방지하기 위해 커서를 닫습니다.
            }
            textView2.setText(String.valueOf(amount));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View itemView = inflater.inflate(R.layout.su_item_category2, viewGroup, false); // 기본적으로 su_item_category2 사용
        return new ViewHolder(itemView, this.listener, mDbOpenHelper);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int position) {
        final su_Category item = items.get(position);
        viewHolder.setItem(item);
        ImageButton imageButton = viewHolder.imageButton;
        imageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                Log.d("Btn", "버튼눌린 위치 : " + position);
                PopupMenu popupMenu = new PopupMenu(mContext, viewHolder.imageButton);
                popupMenu.inflate(R.menu.su_category_item_menu);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        int position = viewHolder.getAdapterPosition();
                        if (position == RecyclerView.NO_POSITION) return false;

                        switch (menuItem.getItemId()) {
                            case R.id.edit:
                                Log.d("Btn", "수정버튼 클릭");
                                showEditDialog(position, item);
                                break;
                            case R.id.delete:
                                Log.d("Btn", "삭제버튼 클릭");
                                showDeleteDialog(position, item);
                                break;
                        }
                        return false;
                    }
                });
                popupMenu.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addCategory(su_Category item) {
        items.add(item);
    }

    public void setOnItemClickListener(OnCategoryItemClickListener2 listener) {
        this.listener = listener;
    }

    public su_Category getItem(int position) {
        return items.get(position);
    }

    public void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        View view = LayoutInflater.from(mContext).inflate(R.layout.category_add_box, null, false);
        builder.setView(view);

        final EditText editText = view.findViewById(R.id.add_category_name);
        final Button addOk = view.findViewById(R.id.add_ok_btn);
        final Button addCancel = view.findViewById(R.id.add_cancel_btn);

        final AlertDialog dialog = builder.create();

        addOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editText.getText().toString().equals("")) {
                    Toast.makeText(mContext, "카테고리명을 입력하세요", Toast.LENGTH_SHORT).show();
                } else {
                    cate = editText.getText().toString();
                    String[] columns = new String[]{DBActivity.COL_CATE};
                    Cursor cursor = mDbOpenHelper.selectCate(columns, "category = '" + cate + "'", null, null, null, null);
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            String check = cursor.getString(0);
                            if (check.equals(cate)) {
                                addCheck = 1;
                            }
                        }
                        cursor.close(); // 리소스 누수를 방지하기 위해 커서를 닫습니다.
                    }
                    if (addCheck == 1) {
                        Toast.makeText(mContext, "해당 카테고리는 이미 존재합니다.", Toast.LENGTH_LONG).show();
                        addCheck = 0;
                    } else {
                        addCategory(new su_Category(cate));
                        mDbOpenHelper.insertCate(cate, 0);
                        notifyDataSetChanged();
                        dialog.dismiss();
                    }
                }
            }
        });

        addCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showEditDialog(final int position, final su_Category item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        View view = LayoutInflater.from(mContext).inflate(R.layout.category_edit_box, null, false);
        builder.setView(view);

        cate = item.getName();
        String[] columns = new String[]{DBActivity.COL_AMOUNT};
        Cursor cursor = mDbOpenHelper.selectCate(columns, "category = '" + cate + "'", null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                amount = cursor.getInt(0);
            }
            cursor.close(); // 리소스 누수를 방지하기 위해 커서를 닫습니다.
        }

        final EditText editText = view.findViewById(R.id.edit_category_name);
        final Button editOk = view.findViewById(R.id.edit_ok_btn);
        final Button editCancel = view.findViewById(R.id.edit_cancel_btn);

        final AlertDialog dialog = builder.create();
        editOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String Ncate = editText.getText().toString();
                mDbOpenHelper.deleteCate(cate);
                mDbOpenHelper.insertCate(Ncate, amount);
                mDbOpenHelper.updateAllCate(cate, Ncate);
                Toast.makeText(mContext, Ncate + "으로 수정되었습니다.", Toast.LENGTH_LONG).show();
                item.setName(Ncate);
                notifyItemChanged(position);
                dialog.dismiss();
            }
        });

        editCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(mContext, "취소되었습니다.", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public void removeItem(int position) {
        items.remove(position);
        notifyItemRemoved(position);
        notifyDataSetChanged();
    }

    private void showDeleteDialog(final int position, su_Category item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        View view = LayoutInflater.from(mContext).inflate(R.layout.category_delete_box, null, false);
        builder.setView(view);

        cate = item.getName();
        String[] columns = new String[]{DBActivity.COL_AMOUNT};
        Cursor cursor = mDbOpenHelper.selectCate(columns, "category = '" + cate + "'", null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                amount = cursor.getInt(0);
            }
            cursor.close(); // 리소스 누수를 방지하기 위해 커서를 닫습니다.
        }

        final Button deleteOk = view.findViewById(R.id.delete_ok);
        final Button deleteCancel = view.findViewById(R.id.delete_cancel);

        final AlertDialog dialog = builder.create();
        deleteOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (amount == 0) {
                    Toast.makeText(mContext, cate + "는 삭제되었습니다.", Toast.LENGTH_LONG).show();
                    mDbOpenHelper.deleteCate(cate);
                    removeItem(position);
                    dialog.dismiss();
                } else {
                    Toast.makeText(mContext, "해당 카테고리에 해당하는 제품이 있어 삭제할 수 없습니다.", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                }
            }
        });

        deleteCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}
