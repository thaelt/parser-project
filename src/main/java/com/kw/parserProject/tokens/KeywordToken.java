package com.kw.parserProject.tokens;

public class KeywordToken extends Token {
    public KeywordToken(String data) {
        super(data);
    }

    @Override
    public String toString() {
        return data;
    }
}
