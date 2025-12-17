package com.kw.parserProject;

import com.kw.parserProject.statements.IfStatement;
import com.kw.parserProject.statements.Statement;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class UnusedStatementChecker {

    List<Statement> getUnusedStatements(List<Statement> program) {
        List<Statement> unusedOverwrittenAssignmentsFromStatements = new LinkedList<>();
        Map<String, List<Statement>> recentAssignments = new HashMap<>();

        for (Statement parsedAssignment : program) {
            updateUnusedStatements(unusedOverwrittenAssignmentsFromStatements, recentAssignments, parsedAssignment);
        }

        return Stream.concat(
                unusedOverwrittenAssignmentsFromStatements.stream(),
                recentAssignments.values().stream().flatMap(Collection::stream)
        ).toList();
    }

    private void updateUnusedStatements(List<Statement> unusedStatements, Map<String, List<Statement>> recentAssignments, Statement statement) {
        // let's start pre-traversal first
        // if variable not in recent assignments, nothing to do, if it's there - let's clear the flag
        if (statement instanceof IfStatement x) {
            handleIfs(unusedStatements, recentAssignments, x);
            return;
        }
        statement.readVariables().forEach(recentAssignments::remove);

        // then, any substatements - handle recursively
        statement.subStatements().forEach(subStatement -> updateUnusedStatements(unusedStatements, recentAssignments, subStatement));

        // if variable is written to, mark it
        String writeVariable = statement.writeVariable();
        if (Objects.nonNull(writeVariable)) {
            List<Statement> previousStatement = recentAssignments.computeIfAbsent(writeVariable, _ -> new ArrayList<>());
            if (!previousStatement.isEmpty()) {
                // value was written to, but never read up to this point, let's store it
                unusedStatements.addAll(previousStatement);
                previousStatement.clear();
            }
            // let's populate recent assignments with the freshest entry
            previousStatement.add(statement);
        }

        // and post visit options like while loops
        statement.postVisitReadVariable().forEach(recentAssignments::remove);
    }

    private void handleIfs(List<Statement> unusedStatements, Map<String, List<Statement>> recentAssignments, IfStatement statement) {
        statement.getCondition().readVariables().forEach(recentAssignments::remove);

        HashMap<String, List<Statement>> recentStateForFirstExecutionBranch = deepCopy(recentAssignments);
        HashMap<String, List<Statement>> recentStateForSecondExecutionBranch = deepCopy(recentAssignments);

        statement.getIfClauseStatements().forEach(subStatement -> updateUnusedStatements(unusedStatements, recentStateForFirstExecutionBranch, subStatement));
        statement.getElseClauseStatements().forEach(subStatement -> updateUnusedStatements(unusedStatements, recentStateForSecondExecutionBranch, subStatement));

        BiConsumer<String, List<Statement>> collectUnusedVariables = (variable, values) -> recentAssignments.computeIfAbsent(variable, _ -> new ArrayList<>()).addAll(values);
        recentStateForFirstExecutionBranch.forEach(collectUnusedVariables);
        recentStateForSecondExecutionBranch.forEach(collectUnusedVariables);
    }

    private HashMap<String, List<Statement>> deepCopy(Map<String, List<Statement>> toCopy) {
        HashMap<String, List<Statement>> copy = new HashMap<>();
        toCopy.forEach((key, val) -> copy.put(key, new ArrayList<>(val)));
        return copy;
    }
}
