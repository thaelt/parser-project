package com.kw.parserProject.tokens;

public class OpeningBracketToken extends Token {
    public OpeningBracketToken(String data) {
        super(data);
    }

    @Override
    public String toString() {
        return data;
    }
}
