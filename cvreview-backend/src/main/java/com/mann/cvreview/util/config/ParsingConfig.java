package com.mann.cvreview.util.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;


@ConfigurationProperties(prefix = "ats.rules")
public class ParsingConfig {

    // Configuration fields bound from application.properties
    private List<String> headingWhitelist;
    private List<String> fontWhitelist;
    private int minTextLength;
    private int keywordDensityMin;
    private int keywordDensityMax;
    private double weightParsing;
    private double weightKeyword;
    private double weightCompleteness;

    // Standard getters and setters
    public List<String> getHeadingWhitelist() {
        return headingWhitelist;
    }
    public void setHeadingWhitelist(List<String> headingWhitelist) {
        this.headingWhitelist = headingWhitelist;
    }
    public List<String> getFontWhitelist() {
        return fontWhitelist;
    }
    public void setFontWhitelist(List<String> fontWhitelist) {
        this.fontWhitelist = fontWhitelist;
    }
    public int getMinTextLength() {
        return minTextLength;
    }
    public void setMinTextLength(int minTextLength) {
        this.minTextLength = minTextLength;
    }
    public int getKeywordDensityMin() {
        return keywordDensityMin;
    }
    public void setKeywordDensityMin(int keywordDensityMin) {
        this.keywordDensityMin = keywordDensityMin;
    }
    public int getKeywordDensityMax() {
        return keywordDensityMax;
    }
    public void setKeywordDensityMax(int keywordDensityMax) {
        this.keywordDensityMax = keywordDensityMax;
    }
    public double getWeightParsing() {
        return weightParsing;
    }
    public void setWeightParsing(double weightParsing) {
        this.weightParsing = weightParsing;
    }
    public double getWeightKeyword() {
        return weightKeyword;
    }
    public void setWeightKeyword(double weightKeyword) {
        this.weightKeyword = weightKeyword;
    }
    public double getWeightCompleteness() {
        return weightCompleteness;
    }
    public void setWeightCompleteness(double weightCompleteness) {
        this.weightCompleteness = weightCompleteness;
    }
}