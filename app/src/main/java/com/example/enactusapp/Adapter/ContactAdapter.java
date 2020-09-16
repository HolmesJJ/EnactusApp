package com.example.enactusapp.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.enactusapp.Config.Config;
import com.example.enactusapp.Entity.User;
import com.example.enactusapp.Listener.OnItemClickListener;
import com.example.enactusapp.R;
import com.example.enactusapp.Utils.CalculateUtils;

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

    private static final String TAG = "ContactAdapter";
    private Context context;
    private List<User> users;

    private LayoutInflater mInflater = null;
    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener){
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public ContactAdapter(Context context, List<User> users) {
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
        this.users = users;
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
        String name = users.get(position).getName();
        String thumbnail = users.get(position).getThumbnail();
        holder.mNameTextView.setText(name);
        double distance = CalculateUtils.getDistance(Config.sLatitude, Config.sLongitude, users.get(position).getLatitude(), users.get(position).getLongitude());
        Log.i(TAG, "user distance lat1: " + Config.sLatitude + ", lng1: " + Config.sLongitude + ", lat2: " + users.get(position).getLatitude() + ", lng2: " + users.get(position).getLongitude() + ", distance: " + distance);
        holder.mDistanceTextView.setText(String.format("%.2f", distance) + "km");
        Glide.with(context).load(thumbnail).into(holder.mThumbnailImageView);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public class ContactViewHolder extends RecyclerView.ViewHolder {

        private TextView mNameTextView;
        private TextView mDistanceTextView;
        private ImageView mThumbnailImageView;

        public ContactViewHolder(View itemView) {
            super(itemView);
            mNameTextView = itemView.findViewById(R.id.name_tv);
            mDistanceTextView = itemView.findViewById(R.id.distance_tv);
            mThumbnailImageView = itemView.findViewById(R.id.thumbnail_iv);
        }
    }
}
