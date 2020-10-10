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
public class SentencesAdapter extends RecyclerView.Adapter<SentencesAdapter.SentencesViewHolder> {

    private Context context;
    private List<String> sentences;

    private LayoutInflater mInflater = null;
    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public SentencesAdapter(Context context, List<String> sentences) {
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
        this.sentences = sentences;
    }

    @NonNull
    @Override
    public SentencesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_sentence, parent, false);
        final SentencesViewHolder holder = new SentencesViewHolder(view);
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
    public void onBindViewHolder(@NonNull SentencesViewHolder holder, int position) {
        String possibleAnswer = sentences.get(position);
        holder.mSentenceTextView.setText(possibleAnswer);
    }

    @Override
    public int getItemCount() {
        return sentences.size();
    }

    public class SentencesViewHolder extends RecyclerView.ViewHolder {

        private TextView mSentenceTextView;

        public SentencesViewHolder(View itemView) {
            super(itemView);
            mSentenceTextView = itemView.findViewById(R.id.sentence_tv);
        }
    }
}
