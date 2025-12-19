package com.kw.parserProject.statements;

import java.util.List;

public record BracketExpression(Expression expressionInBrackets) implements Expression {

    @Override
    public List<String> readVariables() {
        // brackets - just delegate
        return expressionInBrackets.readVariables();
    }

    @Override
    public String print() {
        return "(" + expressionInBrackets.print() + ")";
    }
}
