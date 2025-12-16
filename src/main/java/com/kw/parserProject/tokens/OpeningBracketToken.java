package com.kw.parserProject.tokens;

public class OpeningBracketToken extends Token {
    public OpeningBracketToken() {
        super("(");
    }

    @Override
    public String toString() {
        return data;
    }
}
