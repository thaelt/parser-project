package com.kw.parserProject;

import com.kw.parserProject.statements.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class UnusedStatementChecker {

    List<Statement> getUnusedStatements(Program program) {
        List<Statement> unusedOverwrittenAssignmentsFromStatements = new LinkedList<>();
        Map<String, List<Statement>> recentAssignments = new HashMap<>();

        for (Statement parsedAssignment : program.statements()) {
            updateUnusedStatements(unusedOverwrittenAssignmentsFromStatements, recentAssignments, parsedAssignment);
        }

        return Stream.concat(
                unusedOverwrittenAssignmentsFromStatements.stream(),
                recentAssignments.values().stream().flatMap(Collection::stream)
        ).sorted(Comparator.comparing(HasLineNumber::getLineNumber)).toList();
    }

    private void updateUnusedStatements(List<Statement> unusedStatements, Map<String, List<Statement>> recentAssignments, Statement statement) {
        // let's start pre-traversal first
        // if variable not in recent assignments, nothing to do, if it's there - let's clear the flag
        switch (statement) {
            case IfStatement ifStatement -> handleIfs(unusedStatements, recentAssignments, ifStatement);
            case WhileStatement whileStatement ->
                    handleWhileStatement(unusedStatements, recentAssignments, whileStatement);
            case Assignment assignmentStatement ->
                    handleAssignment(unusedStatements, recentAssignments, assignmentStatement);
            case null -> throw new IllegalArgumentException("Null statement given for analysis");
            default -> throw new IllegalArgumentException("Undefined statement type: " + statement);
        }
    }

    private void handleAssignment(List<Statement> unusedStatements, Map<String, List<Statement>> recentAssignments, Assignment statement) {
        // remove read variables
        statement.expression().readVariables().forEach(recentAssignments::remove);

        String writeVariable = statement.writeVariable();
        List<Statement> recentlyDefinedStatementsForVariable = recentAssignments.computeIfAbsent(writeVariable, _ -> new ArrayList<>());
        if (!recentlyDefinedStatementsForVariable.isEmpty()) {
            // value was written to, but never read up to this point, let's store it
            unusedStatements.addAll(recentlyDefinedStatementsForVariable);
            recentlyDefinedStatementsForVariable.clear();
        }
        // let's populate recent assignments with the freshest entry
        recentlyDefinedStatementsForVariable.add(statement);
    }

    private void handleWhileStatement(List<Statement> unusedStatements, Map<String, List<Statement>> recentAssignments, WhileStatement statement) {
        // condition - called when entering the loop
        statement.condition().readVariables().forEach(recentAssignments::remove);

        // iterate through statements inside, once is enough
        statement.statements().forEach(subStatement -> updateUnusedStatements(unusedStatements, recentAssignments, subStatement));

        // condition - called when evaluating before leaving the loop
        statement.condition().readVariables().forEach(recentAssignments::remove);
    }

    private void handleIfs(List<Statement> unusedStatements, Map<String, List<Statement>> recentAssignments, IfStatement statement) {
        // condition - called when entering the statement
        statement.condition().readVariables().forEach(recentAssignments::remove);

        HashMap<String, List<Statement>> recentStateForFirstExecutionBranch = deepCopy(recentAssignments);
        HashMap<String, List<Statement>> recentStateForSecondExecutionBranch = deepCopy(recentAssignments);

        // execute each if execution path separately, gather the most recent state for all variables
        statement.ifClauseStatements().forEach(subStatement -> updateUnusedStatements(unusedStatements, recentStateForFirstExecutionBranch, subStatement));
        statement.elseClauseStatements().forEach(subStatement -> updateUnusedStatements(unusedStatements, recentStateForSecondExecutionBranch, subStatement));

        // merge the output of both, storing them without overriding each other
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
