package net.runelite.client.plugins.owoify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Owoify {

    private static Map<String, String> wordMap = new HashMap();
    private static List<String> prefixes = Arrays.asList("OwO",
            "OwO whats this?",
            "*unbuttons shirt*",
            "*nuzzles*",
            "*waises paw*",
            "*notices bulge*",
            "*blushes*",
            "*giggles*",
            "hehe");

    public static void Setup() {
        wordMap.put("love", "wuv");
        wordMap.put("mr", "mistuh");
        wordMap.put("dog", "doggo");
        wordMap.put("cat", "kitteh");
        wordMap.put("hello", "henwo");
        wordMap.put("hell", "heck");
        wordMap.put("fuck", "fwick");
        wordMap.put("fuk", "fwick");
        wordMap.put("shit", "shoot");
        wordMap.put("friend", "fwend");
        wordMap.put("stop", "stawp");
        wordMap.put("god", "gosh");
        wordMap.put("dick", "peepee");
        wordMap.put("penis", "peepee");
        wordMap.put("damn", "darn");
    }

    public static String convert(String text) {
        String[] words = text.split(" ");
        String result = "";

        double roll = Math.floor(Math.random()*5);

        if(roll == 0) {
            result += prefixes.get((int) Math.floor(Math.random() * prefixes.size()));
        }

        for(String w : words) {
            if(wordMap.get(w) != null) {
                result += " " + wordMap.get(w);
            }
            else {
                result += " ";
                for(char c : w.toCharArray()) {
                    char newChar = c;
                    if(c == 'l' || c == 'r') {
                        newChar = 'w';
                    }
                    else if(c == 'L' || c == 'R') {
                        newChar = 'W';
                    }

                    result += newChar;
                }
            }
        }

        return result;
    }
}
