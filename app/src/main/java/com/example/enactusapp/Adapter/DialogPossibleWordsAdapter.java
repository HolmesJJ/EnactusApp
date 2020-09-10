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
public class DialogPossibleWordsAdapter extends RecyclerView.Adapter<DialogPossibleWordsAdapter.DialogPossibleWordsViewHolder> {

    private Context context;
    private List<String> possibleWordsList;

    private LayoutInflater mInflater = null;

    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener){
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public DialogPossibleWordsAdapter(Context context, List<String> possibleWordsList) {
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
        this.possibleWordsList = possibleWordsList;
    }

    @NonNull
    @Override
    public DialogPossibleWordsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_dialog_possible_word, parent, false);
        final DialogPossibleWordsViewHolder holder = new DialogPossibleWordsViewHolder(view);
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
    public void onBindViewHolder(@NonNull DialogPossibleWordsViewHolder holder, int position) {
        String possibleWord = possibleWordsList.get(position);
        holder.mPossibleWordTextView.setText(possibleWord);
    }

    @Override
    public int getItemCount() {
        return possibleWordsList.size();
    }

    public class DialogPossibleWordsViewHolder extends RecyclerView.ViewHolder {

        private TextView mPossibleWordTextView;

        public DialogPossibleWordsViewHolder(View itemView) {
            super(itemView);
            mPossibleWordTextView = itemView.findViewById(R.id.possible_word_tv);
        }
    }
}