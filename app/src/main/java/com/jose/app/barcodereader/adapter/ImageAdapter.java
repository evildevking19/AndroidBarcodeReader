package com.jose.app.barcodereader.adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.jose.app.barcodereader.R;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private List<Bitmap> arrImg;

    private ItemClickListener listener;
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */


    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iv;
//        private final ImageButton btn_del;
        private final TextView emptyView;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            iv = (ImageView) view.findViewById(R.id.iv_item_pic);
//            btn_del = (ImageButton) view.findViewById(R.id.btn_rm_img);
            emptyView = (TextView) view.findViewById(R.id.empty_view);
        }

        public ImageView getIv() {
            return iv;
        }

//        public ImageButton getBtn_del() {
//            return btn_del;
//        }

        public TextView getEmptyView() {
            return emptyView;
        }
    }

    /**
     * Initialize the dataset of the Adapter
     *
     * @param dataSet String[] containing the data to populate views to be used
     * by RecyclerView
     */
    public ImageAdapter(List<Bitmap> dataSet, ItemClickListener listener) {
        this.arrImg = dataSet;
        this.listener = listener;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.pic_item, viewGroup, false);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, @SuppressLint("RecyclerView") int position) {

        Bitmap bitmap = arrImg.get(position);
        viewHolder.getIv().setImageBitmap(bitmap);
//        viewHolder.getBtn_del().setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                arrImg.remove(position);
//                notifyItemRemoved(position);
//            }
//        });
        viewHolder.getIv().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onTouch(position, bitmap, viewHolder.getIv());
            }
        });
        viewHolder.getIv().setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new AlertDialog.Builder(v.getContext())
                        .setMessage("Are you confirm to delete this image?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                arrImg.remove(position);
                                notifyItemRemoved(position);
                                listener.onDelete(getItemCount() == 0);
                            }
                        })
                        .create().show();
                return true;
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return arrImg.size();
    }

    public interface ItemClickListener {
        void onTouch(int position, Bitmap curImg, ImageView thumbView);
        void onDelete(boolean isEmptyList);
    }
}
