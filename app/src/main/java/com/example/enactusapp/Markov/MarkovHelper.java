package com.example.enactusapp.Markov;

import android.content.Context;

import com.example.enactusapp.Markov.Listener.MarkovListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MarkovHelper {

    private final static int LIMIT = 10000;

    private MarkovModel markovModel;
    private Trie[] tries;
    private WordTable wordTable;
    private String preWord;
    private ArrayList<String> enteredWords;
    private Context mContext;

    private List<MarkovListener> mMarkovListeners;

    private boolean isInitialized = false;
    private boolean isDataSetsLoaded = false;

    private MarkovHelper() {
    }

    private static class SingleInstance {
        private static final MarkovHelper INSTANCE = new MarkovHelper();
    }

    public static MarkovHelper getInstance() {
        return MarkovHelper.SingleInstance.INSTANCE;
    }

    public void initMarkov(Context context) {
        markovModel = new MarkovModel();
        tries = new Trie[11];
        enteredWords = new ArrayList<>();
        mContext = context;
        mMarkovListeners = new ArrayList<>();
        isInitialized = true;
    }

    public void addMarkovListener(MarkovListener markovListener) {
        mMarkovListeners.add(markovListener);
    }

    public void clear() {

    }

    // 暂时作用不大
    public void releaseMarkov() {
        wordTable = null;
        preWord = null;
        mMarkovListeners = null;
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

    public List<String> getByPattern(String pattern) {
        wordTable = new WordTable(getWords(pattern));
        return wordTable.getCurPage();
    }

    public void showByPreWord() {
        if (preWord == null) {
            System.out.println("preWord not exist!");
            return;
        }
        wordTable = new WordTable(markovModel.nextWords(preWord));
        wordTable.showCurPage();
    }

    public List<String> getByPreWord() {
        if (preWord == null) {
            System.out.println("preWord not exist!");
            return null;
        }
        wordTable = new WordTable(markovModel.nextWords(preWord));
        return wordTable.getCurPage();
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

    public List<String> getNextPage() {
        return wordTable.getNextPage();
    }

    public void showPrePage() {
        wordTable.showPrePage();
    }

    public List<String> getPrePage() {
        return wordTable.getPrePage();
    }

    public void loadDataSets() {
        try {
            for (int i = 1; i <= 1; i++) {
                markovModel.readFile(mContext, "Harry Potter TXT/" + i + ".txt");
            }
//            for (int i = 1; i <= 5; i++) {
//                markovModel.readFile(mContext, "The Lord of the Rings TXT/" + i + ".txt");
//            }
//            for (int i = 1; i <= 5; i++) {
//                markovModel.readFile(mContext, "Twilight Saga TXT/" + i + ".txt");
//            }
            for (int i = 1; i <= 10; i++) {
                tries[i] = new Trie();
                if (i <= 5) {
                    tries[i].readFile(mContext, "dicts/dict" + i + ".txt");
                }
            }
            for (int i = 0; i < mMarkovListeners.size(); i++) {
                mMarkovListeners.get(i).onDataSetsLoaded();
            }
            isDataSetsLoaded = true;
        } catch (Exception e) {
            e.fillInStackTrace();
            for (int i = 0; i < mMarkovListeners.size(); i++) {
                mMarkovListeners.get(i).onDataSetsError();
            }
        }
    }

    public void work() {
        System.out.print("enter the pattern: ");
        Scanner sc = new Scanner(System.in);
        String pattern = sc.next();
        showByPattern(pattern);
        while (true) {
            System.out.println("已输入单词：" + enteredWords);
            System.out.println("输入“1“使用pattern输入，输入“2 l”或者“2 r”翻页，输入“3 x”选择第x个单词，输入0退出");
            int f = sc.nextInt();
            if (f == 1) {
                work();
                return;
            } else if (f == 2) {
                char c = sc.next().charAt(0);
                if (c == 'l') showPrePage();
                else if (c == 'r') showNextPage();
            } else if (f == 3) {
                int idx = sc.nextInt();
                chooseWord(idx);
                showByPreWord();
            } else if (f == 0)
                return;
        }
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public boolean isDataSetsLoaded() {
        return isDataSetsLoaded;
    }
}
