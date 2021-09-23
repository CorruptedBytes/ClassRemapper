package me.alex.remapper;

import java.io.File;
import java.util.Random;

public class ClassRemapperAPI {

	private static String resultDictionary = null;
    protected static String customDictionary = null;

    public static void startClassRemapping(String input, String output) {
        try {
            CustomRemapper.loadJar(new File(input));
            CustomRemapper.remapClass(CustomRemapper.classes);
            CustomRemapper.saveJar(new File(output));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void startFieldRemapping(String input, String output) {
        try {
            CustomRemapper.loadJar(new File(input));
            CustomRemapper.remapFields(CustomRemapper.classes);
            CustomRemapper.saveJar(new File(output));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void setDictionary(String dictionary) {
    	ClassRemapperAPI.customDictionary = dictionary;
    }

    protected static String getDictionary() {
        Random rand = new Random();
        switch (rand.nextInt(8)) {
            case 0:
                resultDictionary = "++";
                break;
            case 1:
                resultDictionary = "Il";
                break;
            case 2:
                resultDictionary = "__";
                break;
            case 3:
                resultDictionary = "%%";
                break;
            case 4:
                resultDictionary = "~~";
                break;
            case 5:
                resultDictionary = "$$";
                break;
            case 6:
                resultDictionary = "??";
                break;
            case 7:
                resultDictionary = "==";
                break;
            case 8:
                resultDictionary = "**";
                break;
        }
        return resultDictionary;
    }


}