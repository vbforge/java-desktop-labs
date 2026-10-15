package com.vbforge.kanadrill;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One kana character plus the romaji answers we accept for it.
 *
 * "Accepted" is a set, not a single string, because several kana have
 * competing romanisations in common use: し is typed "shi" or "si",
 * を is "wo" or "o", ん is "n" or "nn".
 */
public record Kana(String character, String romaji, Set<String> accepted, Script script) {

    public enum Script { HIRAGANA, KATAKANA }

    public Kana(String character, String romaji, String... alternates) {
        this(character, romaji, acceptedSet(romaji, alternates), inferScript(character));
    }

    private static Set<String> acceptedSet(String romaji, String[] alternates) {
        Set<String> s = new HashSet<>();
        s.add(romaji.toLowerCase());
        for (String a : alternates) s.add(a.toLowerCase());
        return Set.copyOf(s);
    }

    private static Script inferScript(String c) {
        // Hiragana: U+3040–U+309F. Katakana: U+30A0–U+30FF.
        int cp = c.codePointAt(0);
        return (cp >= 0x30A0 && cp <= 0x30FF) ? Script.KATAKANA : Script.HIRAGANA;
    }

    // -----------------------------------------------------------------
    // The 46 basic gojūon, hiragana then katakana.
    // -----------------------------------------------------------------

    public static List<Kana> hiragana() {
        return List.of(
            new Kana("あ","a"), new Kana("い","i"), new Kana("う","u"),
            new Kana("え","e"), new Kana("お","o"),

            new Kana("か","ka"), new Kana("き","ki"), new Kana("く","ku"),
            new Kana("け","ke"), new Kana("こ","ko"),

            new Kana("さ","sa"), new Kana("し","shi","si"), new Kana("す","su"),
            new Kana("せ","se"), new Kana("そ","so"),

            new Kana("た","ta"), new Kana("ち","chi","ti"), new Kana("つ","tsu","tu"),
            new Kana("て","te"), new Kana("と","to"),

            new Kana("な","na"), new Kana("に","ni"), new Kana("ぬ","nu"),
            new Kana("ね","ne"), new Kana("の","no"),

            new Kana("は","ha"), new Kana("ひ","hi"), new Kana("ふ","fu","hu"),
            new Kana("へ","he"), new Kana("ほ","ho"),

            new Kana("ま","ma"), new Kana("み","mi"), new Kana("む","mu"),
            new Kana("め","me"), new Kana("も","mo"),

            new Kana("や","ya"), new Kana("ゆ","yu"), new Kana("よ","yo"),

            new Kana("ら","ra"), new Kana("り","ri"), new Kana("る","ru"),
            new Kana("れ","re"), new Kana("ろ","ro"),

            new Kana("わ","wa"), new Kana("を","wo","o"), new Kana("ん","n","nn")
        );
    }

    public static List<Kana> katakana() {
        return List.of(
            new Kana("ア","a"), new Kana("イ","i"), new Kana("ウ","u"),
            new Kana("エ","e"), new Kana("オ","o"),

            new Kana("カ","ka"), new Kana("キ","ki"), new Kana("ク","ku"),
            new Kana("ケ","ke"), new Kana("コ","ko"),

            new Kana("サ","sa"), new Kana("シ","shi","si"), new Kana("ス","su"),
            new Kana("セ","se"), new Kana("ソ","so"),

            new Kana("タ","ta"), new Kana("チ","chi","ti"), new Kana("ツ","tsu","tu"),
            new Kana("テ","te"), new Kana("ト","to"),

            new Kana("ナ","na"), new Kana("ニ","ni"), new Kana("ヌ","nu"),
            new Kana("ネ","ne"), new Kana("ノ","no"),

            new Kana("ハ","ha"), new Kana("ヒ","hi"), new Kana("フ","fu","hu"),
            new Kana("ヘ","he"), new Kana("ホ","ho"),

            new Kana("マ","ma"), new Kana("ミ","mi"), new Kana("ム","mu"),
            new Kana("メ","me"), new Kana("モ","mo"),

            new Kana("ヤ","ya"), new Kana("ユ","yu"), new Kana("ヨ","yo"),

            new Kana("ラ","ra"), new Kana("リ","ri"), new Kana("ル","ru"),
            new Kana("レ","re"), new Kana("ロ","ro"),

            new Kana("ワ","wa"), new Kana("ヲ","wo","o"), new Kana("ン","n","nn")
        );
    }
}