package com.kw.parserProject.statements;

import java.util.List;

public interface Expression {
    List<String> readVariables();

    String print();
}
