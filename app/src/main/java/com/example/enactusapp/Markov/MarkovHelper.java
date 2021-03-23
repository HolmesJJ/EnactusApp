package com.example.enactusapp.Markov;

import java.util.ArrayList;

public class MarkovHelper {

    private final static int LIMIT = 10000;

    private MarkovModel markovModel;
    private Trie[] tries;
    private WordTable wordTable;
    public String preWord;
    public ArrayList<String> enteredWords = new ArrayList<>();

    private MarkovHelper() {
    }

    private static class SingleInstance {
        private static MarkovHelper INSTANCE = new MarkovHelper();
    }

    public static MarkovHelper getInstance() {
        return MarkovHelper.SingleInstance.INSTANCE;
    }

    public void initMarkov() {
        markovModel = new MarkovModel();
        tries = new Trie[11];
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
            System.out.println("preWord not exist！");
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
}
