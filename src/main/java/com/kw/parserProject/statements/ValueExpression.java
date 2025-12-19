package com.kw.parserProject.statements;

import java.util.List;

public record ValueExpression(Float value, String stringRepresentation) implements Expression {
    public ValueExpression(String stringRepresentation) {
        this(Float.parseFloat(stringRepresentation), stringRepresentation);
    }

    @Override
    public List<String> readVariables() {
        // no variables read in this case
        return List.of();
    }

    @Override
    public String print() {
        // uses string representation to prettily print the number, even if input was a plain integer
        return stringRepresentation;
    }

}
