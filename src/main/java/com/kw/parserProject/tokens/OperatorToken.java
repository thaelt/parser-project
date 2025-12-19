package com.kw.parserProject.tokens;

import com.kw.parserProject.Operator;

public class OperatorToken extends Token {
    private final Operator operator;

    public OperatorToken(Operator resolvedOperator) {
        super(resolvedOperator.name());
        this.operator = resolvedOperator;
    }

    public Operator getOperator() {
        return operator;
    }
}
