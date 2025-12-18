package com.kw.parserProject.statements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Expression {
    Integer value;
    String variable;

    Expression leftExpression;
    String operator;
    Expression rightExpression;

    Expression expressionInBrackets;

    public Expression(Integer value) {
        this.value = value;
    }

    public Expression(String variable) {
        this.variable = variable;
    }

    public Expression(Expression left, String operator, Expression right) {
        this.leftExpression = left;
        this.operator = operator;
        this.rightExpression = right;
    }

    public Expression(Expression expressionInBrackets) {
        this.expressionInBrackets = expressionInBrackets;
    }

    public List<String> readVariables() {
        // value - do nothing
        if (value != null) {
            return List.of();
        }
        // variable case
        if (variable != null) {
            return List.of(variable);
        }
        // brackets - just delegate
        if (expressionInBrackets != null) {
            return expressionInBrackets.readVariables();
        }
        // operators - return both operand sides,
        HashSet<String> usedVariables = new HashSet<>(leftExpression.readVariables());
        usedVariables.addAll(rightExpression.readVariables());
        return new ArrayList<>(usedVariables);
    }
}
