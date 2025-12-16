package com.kw.parserProject.tokens;

public class ClosingBracketToken extends Token {
    public ClosingBracketToken(String data) {
        super(data);
    }

    @Override
    public String toString() {
        return data;
    }
}
