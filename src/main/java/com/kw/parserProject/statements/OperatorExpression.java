package com.kw.parserProject.statements;

import com.kw.parserProject.Operator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class OperatorExpression implements Expression {
    private Expression leftExpression;
    private final Operator operator;
    private final Expression rightExpression;

    public OperatorExpression(Expression leftExpression, Operator operator,
                              Expression rightExpression) {
        this.leftExpression = leftExpression;
        this.operator = operator;
        this.rightExpression = rightExpression;
    }

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

    public void setLeftExpression(Expression leftExpression) {
        this.leftExpression = leftExpression;
    }

    public Expression leftExpression() {
        return leftExpression;
    }

    public Operator operator() {
        return operator;
    }

    public Expression rightExpression() {
        return rightExpression;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (OperatorExpression) obj;
        return Objects.equals(this.leftExpression, that.leftExpression) &&
                Objects.equals(this.operator, that.operator) &&
                Objects.equals(this.rightExpression, that.rightExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftExpression, operator, rightExpression);
    }

    @Override
    public String toString() {
        return "OperatorExpression[" +
                "leftExpression=" + leftExpression + ", " +
                "operator=" + operator + ", " +
                "rightExpression=" + rightExpression + ']';
    }


}
