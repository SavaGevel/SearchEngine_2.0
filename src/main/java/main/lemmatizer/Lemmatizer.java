package main.lemmatizer;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class Lemmatizer {

    private final static String SERVICE_PARTS_OF_SPEECH = "\\p{all}+[[СОЮЗ]?[ПРЕДЛ]?[МЕЖД]?[ЧАСТ]?]+";
    private final static Pattern servicePartsOfSpeechPattern = Pattern.compile(SERVICE_PARTS_OF_SPEECH);

    private final static String RUSSIAN_WORD = "[А-Яа-я]+";
    private final static Pattern russianWordPattern = Pattern.compile(RUSSIAN_WORD);

    private static LuceneMorphology russianLuceneMorphology;

    public static LuceneMorphology getLuceneMorphology() {
        if(russianLuceneMorphology == null) {
            try {
                russianLuceneMorphology = new RussianLuceneMorphology();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return russianLuceneMorphology;
    }

    public static Map<String, Integer> getLemmasList(String text) {

        Map<String, Integer> lemmas = new HashMap<>();
        List<String> wordsBaseForms = new LinkedList<>();

        Arrays.stream(text.split("\\s"))
                .map(Lemmatizer::deletePunctuationMarks)
                .map(String::toLowerCase)
                .filter(Lemmatizer::isRussianWord)
                .filter(Lemmatizer::isNotServicePartOfSpeech)
                .map(getLuceneMorphology()::getNormalForms)
                .forEach(wordsBaseForms::addAll);

        for(String wordBaseForm : wordsBaseForms) {
            if(lemmas.containsKey(wordBaseForm)) {
                lemmas.put(wordBaseForm, lemmas.get(wordBaseForm) + 1);
            } else {
                lemmas.put(wordBaseForm, 1);
            }
        }
        return lemmas;
    }

    private static boolean isRussianWord(String word) {
        return russianWordPattern.matcher(word).matches();
    }

    private static boolean isNotServicePartOfSpeech(String word) {
        AtomicBoolean isServicePartOfSpeech = new AtomicBoolean();
        getLuceneMorphology().getMorphInfo(word).forEach(info -> isServicePartOfSpeech.set(servicePartsOfSpeechPattern.matcher(info).matches()));
        return !isServicePartOfSpeech.get();
    }

    private static String deletePunctuationMarks(String word) {
        return word.replaceAll("\\p{Punct}", "");
    }

}
