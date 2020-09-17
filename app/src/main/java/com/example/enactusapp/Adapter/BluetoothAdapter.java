package com.example.enactusapp.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.enactusapp.Config.Config;
import com.example.enactusapp.Listener.OnItemClickListener;
import com.example.enactusapp.R;
import com.example.enactusapp.Utils.CalculateUtils;
import com.hc.bluetoothlibrary.DeviceModule;

import java.util.List;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class BluetoothAdapter extends RecyclerView.Adapter<BluetoothAdapter.BluetoothViewHolder> {

    private static final String TAG = "BluetoothAdapter";
    private Context context;
    private List<DeviceModule> deviceModules;

    private LayoutInflater mInflater = null;
    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public BluetoothAdapter(Context context, List<DeviceModule> deviceModules) {
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
        this.deviceModules = deviceModules;
    }

    @NonNull
    @Override
    public BluetoothViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_bluetooth, parent, false);
        final BluetoothViewHolder holder = new BluetoothViewHolder(view);
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
    public void onBindViewHolder(@NonNull BluetoothViewHolder holder, int position) {
        holder.mIvBluetooth.setImageResource(deviceModules.get(position).isBLE() ? R.drawable.ic_bluetooth_ble : R.drawable.ic_bluetooth);
        holder.mTvName.setText(deviceModules.get(position).getName());
        holder.mTvMacAddress.setText(deviceModules.get(position).getMac());
        holder.mTvPair.setText(deviceModules.get(position).isBeenConnected() ? "Paired" : "Unpaired");
        holder.mTvPair.setTextColor(deviceModules.get(position).isBeenConnected() ? Color.parseColor("#79D0A5") : Color.parseColor("#737373"));
        holder.mTvRssi.setText(deviceModules.get(position).getRssi() != 10 ? deviceModules.get(position).getRssi()  + " dBm" : "");
        if (deviceModules.get(position).isCollect()) {
            holder.mTvName.setText("");
            holder.mTvCollect.setText(deviceModules.get(position).getName());
        } else {
            holder.mTvCollect.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return deviceModules.size();
    }

    public class BluetoothViewHolder extends RecyclerView.ViewHolder {

        private ImageView mIvBluetooth;
        private TextView mTvName;
        private TextView mTvMacAddress;
        private TextView mTvPair;
        private TextView mTvRssi;
        private TextView mTvCollect;

        public BluetoothViewHolder(View itemView) {
            super(itemView);
            mIvBluetooth = itemView.findViewById(R.id.iv_bluetooth);
            mTvName = itemView.findViewById(R.id.tv_name);
            mTvMacAddress = itemView.findViewById(R.id.tv_mac_address);
            mTvPair = itemView.findViewById(R.id.tv_pair);
            mTvRssi = itemView.findViewById(R.id.tv_rssi);
            mTvCollect = itemView.findViewById(R.id.tv_collect);
        }
    }
}
