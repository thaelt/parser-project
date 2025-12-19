package com.kw.parserProject.statements;

import com.kw.parserProject.utility.Printable;

import java.util.List;

public interface Expression extends Printable {
    List<String> readVariables();
}
