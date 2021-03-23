package com.example.enactusapp.Markov;

import java.util.ArrayList;
import java.util.List;

public class WordTable {

    public final int WORD_NUM_PER_PAGE = 5;

    public class WordCell {
        String word;
        int trieID;
        int idx;

        public WordCell(String word, int trieID, int idx) {
            this.trieID = trieID;
            this.word = word;
            this.idx = idx;
        }
    }

    public int pages;
    public int cur_page;
    public int word_num;
    public WordCell[][] table;

    public WordTable(String[][] words) {
        cur_page = 0;
        word_num = 0;
        for (int i = 1; i <= 10; i++) {
            word_num += words[i].length;
        }
        pages = (word_num + WORD_NUM_PER_PAGE - 1) / WORD_NUM_PER_PAGE;
        table = new WordCell[pages][WORD_NUM_PER_PAGE + 1];

        int cur_level = 10, cur_id = 0;

        for (int i = 0; i < pages; i++) {
            for (int j = 1; j <= WORD_NUM_PER_PAGE; j++) {
                while (cur_level >= 1 && cur_id >= words[cur_level].length) {
                    cur_level--;
                    cur_id = 0;
                }
                if (cur_level == 0) break;
                table[i][j] = new WordCell(words[cur_level][cur_id++], cur_level, cur_id);
            }
        }
    }


    public WordTable(String[] words) {
        cur_page = 0;
        word_num = words.length;

        pages = (word_num + WORD_NUM_PER_PAGE - 1) / WORD_NUM_PER_PAGE;
        table = new WordCell[pages][WORD_NUM_PER_PAGE + 1];

        int cur_id = 0;

        for (int i = 0; i < pages; i++) {
            for (int j = 1; j <= WORD_NUM_PER_PAGE; j++) {
                if (cur_id >= word_num) break;
                table[i][j] = new WordCell(words[cur_id++], -1, -1);
            }
        }
    }

    public void showCurPage() {
        System.out.println("-----------------------------------------------------------------------------------");
        System.out.println("page" + (cur_page + 1) + ":");
        for (int j = 1; j <= WORD_NUM_PER_PAGE; j++) {
            if (cur_page * WORD_NUM_PER_PAGE + j > word_num) break;
            System.out.print(j + ": " + table[cur_page][j].word + " ");
        }
        System.out.println('\n');
    }

    public List<String> getCurPage() {
        List<String> words = new ArrayList<>();
        for (int j = 1; j <= WORD_NUM_PER_PAGE; j++) {
            if (cur_page * WORD_NUM_PER_PAGE + j > word_num) break;
            words.add(table[cur_page][j].word);
        }
        return words;
    }

    public void showNextPage() {
        if (cur_page < pages - 1) cur_page++;
        showCurPage();
    }

    public List<String> getNextPage() {
        if (cur_page < pages - 1) cur_page++;
        return getCurPage();
    }

    public void showPrePage() {
        if (cur_page > 0) cur_page--;
        showCurPage();
    }

    public List<String> getPrePage() {
        if (cur_page > 0) cur_page--;
        return getCurPage();
    }
}