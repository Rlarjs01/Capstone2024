package susemi2024.ssm2024.capstone;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.net.Uri;
import android.database.SQLException;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.ssm2024.capstone.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainAdapter extends RecyclerView.Adapter<MainAdapter.ViewHolder>
        implements OnProductItemClickListener, ItemTouchHelperCallback.OnItemMoveListener {

    ArrayList<Product> items = new ArrayList<>();
    OnProductItemClickListener listener;
    Context mContext;

    public MainAdapter(Context context) {
        mContext = context;
    }

    public ArrayList<Product> getItems() {
        return items;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView1;
        TextView textView2;
        ImageView drag;
        CardView cardView;
        ImageButton button0;
        Bitmap bm;

        public ViewHolder(View itemView, final OnProductItemClickListener listener, final MainAdapter adapter) {
            super(itemView);

            imageView = itemView.findViewById(R.id.product_image);
            textView1 = itemView.findViewById(R.id.product_name);
            textView2 = itemView.findViewById(R.id.product_date);
            cardView = itemView.findViewById(R.id.product_cardView);
            drag = itemView.findViewById(R.id.drag);
            button0 = itemView.findViewById(R.id.button0);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (listener != null)
                        listener.onItemClick(ViewHolder.this, v, position);
                }
            });

            button0.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPopupMenu(v, adapter);
                }
            });
        }

        private void showPopupMenu(final View v, final MainAdapter adapter) {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.info_delete_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.delete:
                            int position = getAdapterPosition();
                            if (position != RecyclerView.NO_POSITION) {
                                int id = adapter.items.get(position).getId();
                                adapter.handleDelete(v.getContext(), id, position);

                                Intent refresh = new Intent(v.getContext(), MainActivity.class);
                                v.getContext().startActivity(refresh);
                            }
                            return true;
                        default:
                            return false;
                    }
                }
            });

            popup.show();
        }

        public void setItem(Product item) {
            textView1.setText(item.getName());
            String expirationDate = item.getExpirationDate();
            if (expirationDate != null) {
                textView2.setText(expirationDate);
            } else {
                textView2.setText("유통기한 정보 없음");
            }

            if (item.isPassed) {
                Log.d("color", "color is changed");
                cardView.setBackgroundColor(Color.parseColor("#828282"));
            } else {
                cardView.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }

            final String image = item.getImage_src();
            if (image == null) {
                imageView.setImageResource(R.drawable.img_smol_put);
            } else if (image.indexOf("http") == -1) {
                imageView.setImageURI(Uri.parse(image));
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
                    imageView.setImageBitmap(bm);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void handleDelete(Context context, int id, int position) {
        DBActivityHelper dbHelper = new DBActivityHelper(context);
        dbHelper.open();

        String category = getCategoryById(id, dbHelper);
        if (category == null) {
            Toast.makeText(context, "카테고리를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            dbHelper.close();
            return;
        }

        int amount = getCategoryAmount(category, dbHelper);
        if (amount == -1) {
            Toast.makeText(context, "카테고리 수량을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            dbHelper.close();
            return;
        }

        boolean isDeleted = dbHelper.deleteColumn(id);

        if (isDeleted) {
            Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
            dbHelper.updateCate(category, amount - 1);
            items.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, items.size());
        } else {
            Toast.makeText(context, "삭제 실패: 항목을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
        }

        dbHelper.close();
    }

    private String getCategoryById(int itemId, DBActivityHelper dbHelper) {
        Cursor cursor = null;
        try {
            cursor = dbHelper.mDB.query(DBActivity._TABLENAME,
                    new String[]{DBActivity.COL_CATE},
                    "_id = ?",
                    new String[]{String.valueOf(itemId)},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String category = cursor.getString(cursor.getColumnIndex(DBActivity.COL_CATE));
                return category;
            }
        } catch (SQLException e) {
            Log.e("MainAdapter", "Database query error", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private int getCategoryAmount(String category, DBActivityHelper dbHelper) {
        Cursor cursor = null;
        try {
            cursor = dbHelper.mDB.query(DBActivity._TABLENAME2,
                    new String[]{DBActivity.COL_AMOUNT},
                    "category = ?",
                    new String[]{category},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int amount = cursor.getInt(cursor.getColumnIndex(DBActivity.COL_AMOUNT));
                return amount;
            }
        } catch (SQLException e) {
            Log.e("MainAdapter", "Database query error", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return -1;
    }

    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.product_item, parent, false);
        return new ViewHolder(itemView, listener, this);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        Product item = items.get(position);
        holder.setItem(item);
    }

    public void addProduct(Product item) {
        items.add(item);
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        items.remove(position);
        notifyItemRemoved(position);
        notifyDataSetChanged();
    }

    public void nameAsc() {
        Comparator<Product> nAsc = new Comparator<Product>() {
            @Override
            public int compare(Product o1, Product o2) {
                if (o1.getName() == null && o2.getName() == null)
                    return 0;
                else if (o1.getName() == null)
                    return -1;
                else if (o2.getName() == null)
                    return 1;
                else
                    return o1.getName().compareTo(o2.getName());
            }
        };
        Collections.sort(items, nAsc);
        notifyDataSetChanged();
    }

    public void nameDsc() {
        Comparator<Product> nDsc = new Comparator<Product>() {
            @Override
            public int compare(Product o1, Product o2) {
                return o2.getName().compareTo(o1.getName());
            }
        };
        Collections.sort(items, nDsc);
        notifyDataSetChanged();
    }

    public void dateAsc() {
        Comparator<Product> dAsc = new Comparator<Product>() {
            @Override
            public int compare(Product o1, Product o2) {
                return o1.getDate().compareTo(o2.getDate());
            }
        };
        Collections.sort(items, dAsc);
        notifyDataSetChanged();
    }

    public void dateDsc() {
        Comparator<Product> dDsc = new Comparator<Product>() {
            @Override
            public int compare(Product o1, Product o2) {
                return o2.getDate().compareTo(o1.getDate());
            }
        };
        Collections.sort(items, dDsc);
        notifyDataSetChanged();
    }

    @Override
    public void onItemMove(int fromPos, int toPos) {
        Collections.swap(items, fromPos, toPos);
        notifyItemMoved(fromPos, toPos);
    }

    public void setOnItemClickListener(OnProductItemClickListener listener) {
        this.listener = listener;
    }

    public void onItemClick(ViewHolder holder, View view, int position) {
        if (listener != null)
            listener.onItemClick(holder, view, position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public Product getItem(int position) {
        return items.get(position);
    }

    public void setItem(int position, Product item) {
        items.set(position, item);
    }
}
