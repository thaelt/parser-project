package com.kw.parserProject.statements;

import java.util.List;

public record VariableExpression(String variable) implements Expression {
    @Override
    public List<String> readVariables() {
        return List.of(variable);
    }

    @Override
    public String print() {
        return variable;
    }
}
