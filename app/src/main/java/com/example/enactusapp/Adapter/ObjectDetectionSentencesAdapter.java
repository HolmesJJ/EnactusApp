package com.example.enactusapp.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
public class ObjectDetectionSentencesAdapter extends RecyclerView.Adapter<ObjectDetectionSentencesAdapter.ObjectDetectionSentencesViewHolder> {

    private Context context;
    private List<String> objectDetectionSentencesList;

    private LayoutInflater mInflater = null;
    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener){
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public ObjectDetectionSentencesAdapter(Context context, List<String> objectDetectionSentencesList) {
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
        this.objectDetectionSentencesList = objectDetectionSentencesList;
    }


    @NonNull
    @Override
    public ObjectDetectionSentencesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_object_detection_sentence, parent, false);
        final ObjectDetectionSentencesViewHolder holder = new ObjectDetectionSentencesViewHolder(view);
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
    public void onBindViewHolder(@NonNull ObjectDetectionSentencesViewHolder holder, int position) {
        String possibleAnswer = objectDetectionSentencesList.get(position);
        holder.mSentenceTextView.setText(possibleAnswer);
    }

    @Override
    public int getItemCount() {
        return objectDetectionSentencesList.size();
    }

    public class ObjectDetectionSentencesViewHolder extends RecyclerView.ViewHolder {

        private TextView mSentenceTextView;

        public ObjectDetectionSentencesViewHolder(View itemView) {
            super(itemView);
            mSentenceTextView = itemView.findViewById(R.id.sentence_tv);
        }
    }
}
