package com.kw.parserProject.statements;

import java.util.List;
import java.util.Objects;

public abstract class Statement {
    private final String content;

    public Statement(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return content;
    }

    // return a variable written to
    public abstract String writeVariable();

    // return variables accessed post-action, like in while loop's condition, for loop's exit condition etc
    public abstract List<String> postVisitReadVariable();

    // return variables accessed pre-action, like if conditions, variables used in assignments
    public abstract List<String> readVariables();

    // return substatements, if any (usable for loops)
    public abstract List<Statement> subStatements();

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Statement) obj;
        return Objects.equals(this.content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

}
