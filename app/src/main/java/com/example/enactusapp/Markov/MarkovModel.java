package com.example.enactusapp.Markov;

import android.content.Context;
import android.util.Log;

import java.io.DataInputStream;
import java.util.*;

public class MarkovModel {

    private HashMap<String, HashMap<String, Integer>> mp;
    private final static String TAG = "MarkovModel";

    public MarkovModel() {
        mp = new HashMap<>();
    }

    public void addFrequency(String s1, String s2) {
        mp.putIfAbsent(s1, new HashMap<>());
        HashMap<String, Integer> table = mp.get(s1);
        table.putIfAbsent(s2, 0);
        table.put(s2, table.get(s2) + 1);
    }

    public void initializeText(String[] text) {
        for (int i = 0; i < text.length - 1; i++) {
            addFrequency(text[i], text[i + 1]);
        }
    }

    public String[] nextWords(String str) {
        HashMap<String, Integer> table = mp.get(str);
        if (table == null) {
            return new String[0];
        }
        List<Map.Entry<String,Integer>> list = new ArrayList<>(table.entrySet());
        list.sort((o1, o2) -> (o2.getValue() - o1.getValue()));
        String[] ret = new String[list.size()];
        for(int i = 0; i<list.size(); i++){
            ret[i] = list.get(i).getKey();
        }
        return ret;
    }

    private HashSet<String> readDict(Context context) throws Exception {
        HashSet<String> st = new HashSet<>();
        for (int i = 1; i <= 5; i++) {
            String filename = "dicts/dict" + i + ".txt";
            DataInputStream textFileStream = new DataInputStream(context.getAssets().open(filename));
            Scanner sc = new Scanner(textFileStream);
            while (sc.hasNext()) {
                st.add(sc.next());
            }
        }
        return st;
    }

    public void readFile(Context context, String filename) throws Exception {
        HashSet<String> st = readDict(context);
        DataInputStream textFileStream = new DataInputStream(context.getAssets().open(filename));
        Scanner sc = new Scanner(textFileStream);
        String word;
        ArrayList<String> text = new ArrayList<>();
        while (sc.hasNext()) {
            word = sc.next();
            int i = 0, j = word.length() - 1;
            while (i < word.length() && !Character.isAlphabetic(word.charAt(i))) i++;
            while (j >= 0 && !Character.isAlphabetic(word.charAt(j))) j--;
            if (i >= word.length() || j < 0) continue;
            word = word.substring(i, j + 1);
            if (st.contains(word))
                text.add(word);
        }
        initializeText(text.toArray(new String[0]));
    }
}