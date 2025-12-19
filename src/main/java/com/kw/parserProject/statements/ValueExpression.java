package com.kw.parserProject.statements;

import java.util.List;

public record ValueExpression(Integer value) implements Expression {

    @Override
    public List<String> readVariables() {
        // no variables read in this case
        return List.of();
    }

    @Override
    public String print() {
        return value.toString();
    }

}
