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

public class DialogPossibleAnswersAdapter extends RecyclerView.Adapter<DialogPossibleAnswersAdapter.DialogPossibleAnswersViewHolder> {

    private final Context context;
    private final List<String> possibleAnswersList;

    private LayoutInflater mInflater = null;
    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener){
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public DialogPossibleAnswersAdapter(Context context, List<String> possibleAnswersList) {
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
        this.possibleAnswersList = possibleAnswersList;
    }

    @NonNull
    @Override
    public DialogPossibleAnswersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_dialog_possible_answer, parent, false);
        final DialogPossibleAnswersViewHolder holder = new DialogPossibleAnswersViewHolder(view);
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
    public void onBindViewHolder(@NonNull DialogPossibleAnswersViewHolder holder, int position) {
        String possibleAnswer = possibleAnswersList.get(position);
        holder.mPossibleAnswerTextView.setText(possibleAnswer);
    }

    @Override
    public int getItemCount() {
        return possibleAnswersList.size();
    }

    public static class DialogPossibleAnswersViewHolder extends RecyclerView.ViewHolder {

        private final TextView mPossibleAnswerTextView;

        public DialogPossibleAnswersViewHolder(View itemView) {
            super(itemView);
            mPossibleAnswerTextView = itemView.findViewById(R.id.possible_answer_tv);
        }
    }
}
