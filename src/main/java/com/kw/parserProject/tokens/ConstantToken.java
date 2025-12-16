package com.kw.parserProject.tokens;

public class ConstantToken extends Token {
    public ConstantToken(String data) {
        super(data);
    }

    @Override
    public String toString() {
        return data;
    }
}
