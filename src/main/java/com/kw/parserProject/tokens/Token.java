package com.kw.parserProject.tokens;

public abstract class Token {
    public final String data;
    public int lineNumber;

    public Token(String data) {
        this.data = data;
    }

    public void addLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

}
