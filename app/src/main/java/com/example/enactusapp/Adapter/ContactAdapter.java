package com.example.enactusapp.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.enactusapp.Listener.OnItemClickListener;
import com.example.enactusapp.R;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private Context context;
    private List<String> nameList;
    private List<String> thumbnailList;

    private LayoutInflater mInflater = null;
    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener){
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public ContactAdapter(Context context, List<String> nameList, List<String> thumbnailList) {
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
        this.nameList = nameList;
        this.thumbnailList = thumbnailList;
    }


    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_contact, parent, false);
        final ContactViewHolder holder = new ContactViewHolder(view);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                mOnItemClickListener.onItemClick(position);
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        String name = nameList.get(position);
        String thumbnail = thumbnailList.get(position);
        holder.mNameTextView.setText(name);
        holder.mThumbnailImageView.setImageResource(context.getResources().getIdentifier(thumbnail, "drawable", context.getPackageName()));
    }

    @Override
    public int getItemCount() {
        return nameList.size();
    }

    public class ContactViewHolder extends RecyclerView.ViewHolder {

        private TextView mNameTextView;
        private ImageView mThumbnailImageView;

        public ContactViewHolder(View itemView) {
            super(itemView);
            mNameTextView = itemView.findViewById(R.id.name_tv);
            mThumbnailImageView = itemView.findViewById(R.id.thumbnail_iv);
        }
    }
}
