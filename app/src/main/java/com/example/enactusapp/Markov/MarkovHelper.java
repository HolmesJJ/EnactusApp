package com.example.enactusapp.Markov;

import android.content.Context;

import com.example.enactusapp.Utils.ToastUtils;

import java.util.ArrayList;

public class MarkovHelper {

    private final static int LIMIT = 10000;

    private MarkovModel markovModel;
    private Trie[] tries;
    private WordTable wordTable;
    private String preWord;
    private ArrayList<String> enteredWords;
    private Context mContext;

    private MarkovHelper() {
    }

    private static class SingleInstance {
        private static MarkovHelper INSTANCE = new MarkovHelper();
    }

    public static MarkovHelper getInstance() {
        return MarkovHelper.SingleInstance.INSTANCE;
    }

    public void initMarkov(Context context) {
        markovModel = new MarkovModel();
        tries = new Trie[11];
        enteredWords = new ArrayList<>();
        mContext = context;
    }

    public void releaseMarkov() {
        wordTable = null;
        preWord = null;
        mContext = null;
        enteredWords = null;
        markovModel = null;
        tries = null;
    }

    private String[][] getWords(String pattern) {
        String[][] ret = new String[11][];
        for (int i = 1; i <= 10; i++) {
            ret[i] = tries[i].prefixSearch(pattern, LIMIT);
        }
        return ret;
    }

    public void showByPattern(String pattern) {
        wordTable = new WordTable(getWords(pattern));
        wordTable.showCurPage();
    }

    public void showByPreWord() {
        if (preWord == null) {
            System.out.println("preWord not exist!");
            return;
        }
        wordTable = new WordTable(markovModel.nextWords(preWord));
        wordTable.showCurPage();
    }

    public String chooseWord(int idx) {
        WordTable.WordCell wordCell = wordTable.table[wordTable.cur_page][idx];
        System.out.println("chosen word: " + wordCell.word);
        if (preWord == null) {
            // 更新权重
            if (wordCell.trieID != 10) {
                tries[wordCell.trieID].delete(wordCell.word);
                tries[wordCell.trieID + 1].insert(wordCell.word);
            }
            int cnt = 0;
            for (int i = 0; i < wordTable.pages; i++) {
                for (int j = 1; j <= wordTable.WORD_NUM_PER_PAGE; j++) {
                    if (cnt++ >= wordTable.word_num) break;
                    WordTable.WordCell wc = wordTable.table[i][j];
                    if (wc.word.equals(wordCell.word)) continue;
                    if (wc.trieID != 1) {
                        tries[wc.trieID].delete(wc.word);
                        tries[wc.trieID - 1].insert(wc.word);
                    }
                }
            }
        } else {
            markovModel.addFrequency(preWord, wordCell.word);
        }
        enteredWords.add(wordCell.word);
        return preWord = wordCell.word;
    }

    public void showNextPage() {
        wordTable.showNextPage();
    }

    public void showPrePage() {
        wordTable.showPrePage();
    }

    public void loadDataSets() {
        try {
            for (int i = 1; i <= 7; i++) {
                markovModel.readFile(mContext, "Harry Potter TXT/" + i + ".txt");
            }
            for (int i = 1; i <= 5; i++) {
                markovModel.readFile(mContext, "The Lord of the Rings TXT/" + i + ".txt");
            }
            for (int i = 1; i <= 5; i++) {
                markovModel.readFile(mContext, "Twilight Saga TXT/" + i + ".txt");
            }
            for (int i = 1; i <= 10; i++) {
                tries[i] = new Trie();
                if (i <= 5) {
                    tries[i].readFile(mContext, "dict/dict" + i + ".txt");
                }
            }
        } catch (Exception e) {
            ToastUtils.showShortSafe("Load data sets error...");
        }
    }
}
