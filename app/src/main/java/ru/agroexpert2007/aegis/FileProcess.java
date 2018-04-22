package ru.agroexpert2007.aegis;

import java.io.File;
import java.util.ArrayList;

public class FileProcess {


    public static ArrayList<File> getFileList(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<>();
        File[] files = parentDir.listFiles();
        for(File f: files) {
            if(f.isDirectory()) {
                inFiles.addAll(getFileList(f));
            } else {
                inFiles.add(f);
            }
        }
        return inFiles;
    }
}
