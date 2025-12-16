package com.kw.parserProject.tokens;

public class AssignmentToken extends Token {
    public AssignmentToken(String data) {
        super(data);
    }

    @Override
    public String toString() {
        return data;
    }
}
