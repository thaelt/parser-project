package com.kw.parserProject.tokens;

public class Token {
    public String data;

    public Token(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Token{" +
                "data=" + data +
                '}';
    }
}
