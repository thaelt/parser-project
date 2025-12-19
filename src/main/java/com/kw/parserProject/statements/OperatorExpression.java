package com.kw.parserProject.statements;

import com.kw.parserProject.Operator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public record OperatorExpression(Expression leftExpression, Operator operator,
                                 Expression rightExpression) implements Expression {

    @Override
    public List<String> readVariables() {
        // operators - return both operand sides,
        HashSet<String> usedVariables = new HashSet<>(leftExpression.readVariables());
        usedVariables.addAll(rightExpression.readVariables());
        return new ArrayList<>(usedVariables);
    }

    @Override
    public String print() {
        return leftExpression.print() + " " + operator.print() + " " + rightExpression.print();
    }

}
