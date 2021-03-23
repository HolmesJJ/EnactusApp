package com.example.enactusapp.Markov;

import android.content.Context;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Trie {

    private final static String TAG = "Trie";

    public class TrieNode {
        TrieNode[] present_chars;
        boolean exist;
        int dep;

        public TrieNode(int dep) {
            present_chars = new TrieNode[256];
            exist = false;
            this.dep = dep;
        }
    }

    private TrieNode root;

    public Trie() {
        root = new TrieNode(0);
    }

    // inserts string s into the Trie
    public void insert(String s) {
        TrieNode node = root;
        for (int i = 0; i < s.length(); i++) {
            if (node.present_chars[s.charAt(i)] == null)
                node.present_chars[s.charAt(i)] = new TrieNode(node.dep + 1);
            node = node.present_chars[s.charAt(i)];
        }
        node.exist = true;
    }

    public void delete(String s){
        TrieNode node = root;
        for (int i = 0; i < s.length(); i++) {
            if (node.present_chars[s.charAt(i)] == null)
                return ;
            node = node.present_chars[s.charAt(i)];
        }
        node.exist = false;
    }

    // checks whether string s exists inside the Trie or not
    public boolean contains(String s) {
        TrieNode node = root;
        for (int i = 0; i < s.length(); i++) {
            if (node.present_chars[s.charAt(i)] == null)
                return false;
            node = node.present_chars[s.charAt(i)];
        }
        return node.exist;
    }

    public void dfs(String s, ArrayList<String> results, int limit, TrieNode node, String tmp) {
        if (results.size() >= limit || node == null) return;
        if (node.dep == s.length()) {
            if (node.exist)
                results.add(tmp);
            return;
        }
        if (s.charAt(node.dep) == 'l') {
            for (int i = 'a'; i <= 'm'; i++)
                if (node.present_chars[i] != null)
                    dfs(s, results, limit, node.present_chars[i], tmp + (char) i);
            for (int i = 'A'; i <= 'M'; i++)
                if (node.present_chars[i] != null)
                    dfs(s, results, limit, node.present_chars[i], tmp + (char) i);
        }
        else if(s.charAt(node.dep)=='r'){
            for (int i = 'n'; i <= 'z'; i++)
                if (node.present_chars[i] != null)
                    dfs(s, results, limit, node.present_chars[i], tmp + (char) i);
            for (int i = 'N'; i <= 'Z'; i++)
                if (node.present_chars[i] != null)
                    dfs(s, results, limit, node.present_chars[i], tmp + (char) i);
        }
    }

    public String[] prefixSearch(String s, int limit) {
        ArrayList<String> results = new ArrayList<>();
        dfs(s, results, limit, root, "");
        return results.toArray(new String[0]);
    }

    public void readFile(Context context, String filename) throws Exception {
        DataInputStream textFileStream = new DataInputStream(context.getAssets().open(filename));
        Scanner sc = new Scanner(textFileStream);
        while (sc.hasNextLine()) {
            String str = sc.nextLine();
            Log.i(TAG, "Trie File content: " + str);
            if(str.contains(" "))continue;
            insert(str);
        }
    }
}
