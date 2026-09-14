package com.example.forum.utils;

import org.jsoup.Jsoup;

public class HtmlUtils {
    public static String removeTag(String htmlStr){
        if(htmlStr == null){
            return "";
        }

        return Jsoup.parse(htmlStr).text();
    }    
}
