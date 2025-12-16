package com.kw.parserProject.tokens;

public class ClosingBracketToken extends Token {
    public ClosingBracketToken() {
        super(")");
    }

    @Override
    public String toString() {
        return data;
    }
}
