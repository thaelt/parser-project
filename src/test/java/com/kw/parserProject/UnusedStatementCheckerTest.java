package com.kw.parserProject;

import com.kw.parserProject.statements.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.kw.parserProject.Operator.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UnusedStatementCheckerTest {

    UnusedStatementChecker unusedStatementChecker;

    // commonly defined statements/conditions:
    Assignment xIsFive = new Assignment("x", new ValueExpression("5"));
    Assignment xIsSix = new Assignment("x", new ValueExpression("6"));
    Assignment yIsXPlusTwo = new Assignment("y", new OperatorExpression(new VariableExpression("x"), PLUS, new ValueExpression("2")));
    Assignment zIsXPlusThree = new Assignment("z", new OperatorExpression(new VariableExpression("x"), PLUS, new ValueExpression("3")));
    Expression xLessThanFive = new OperatorExpression(new VariableExpression("x"), LESS_THAN, new ValueExpression("5"));
    Expression zLessThanFive = new OperatorExpression(new VariableExpression("z"), LESS_THAN, new ValueExpression("5"));

    @BeforeEach
    void setUp() {
        unusedStatementChecker = new UnusedStatementChecker();
    }

    @Test
    void shouldHandleSingleAssignment() {
        // given
        List<Statement> statements = List.of(
                xIsFive
        );

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(new Program(statements));

        // then
        assertEquals(1, results.size());
        assertEquals(xIsFive, results.getFirst());
    }

    @Test
    void shouldPickAllAssignmentsForSameVariable() {
        // given
        List<Statement> statements = List.of(
                xIsFive, xIsSix
        );

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(new Program(statements));

        // then
        assertEquals(2, results.size());
        assertEquals(xIsFive, results.getFirst());
        assertEquals(xIsSix, results.get(1));
    }

    @Test
    void shouldPickOnlyLastUnusedAssignmentIfVariableIsReadSeveralTimes() {
        // given
        List<Statement> statements = List.of(
                xIsFive, xIsSix, yIsXPlusTwo, zIsXPlusThree
        );

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(new Program(statements));

        // then
        assertEquals(3, results.size());
        assertEquals(xIsFive, results.getFirst());
        assertEquals(yIsXPlusTwo, results.get(1));
        assertEquals(zIsXPlusThree, results.get(2));
    }

    @Test
    void shouldHandleStatementDefinedTwiceIfNeverRead() {
        // given
        List<Statement> statements = List.of(
                xIsFive, xIsFive
        );

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(new Program(statements));

        // then
        assertEquals(2, results.size());
        assertEquals(xIsFive, results.getFirst());
        assertEquals(xIsFive, results.get(1));
    }

    @Test
    void shouldHandleStatementDefinedTwiceIfRead() {
        // given
        List<Statement> statements = List.of(
                xIsFive, xIsFive, yIsXPlusTwo
        );

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(new Program(statements));

        // then
        assertEquals(2, results.size());
        assertEquals(xIsFive, results.getFirst());
        assertEquals(yIsXPlusTwo, results.get(1));
        // adding line numbers or other metadata might be a good next step to find exactly which one is unused in this case
    }

    @Test
    void shouldMarkVariableUsedInWhileConditionAsRead() {
        // given
        Statement whileStatement = new WhileStatement(xLessThanFive, List.of(xIsFive));

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(new Program(List.of(whileStatement)));

        // then
        assertEquals(0, results.size());
    }

    @Test
    void shouldMarkUnusedAssignmentFromWhileLoop() {
        // given
        Statement whileStatement = new WhileStatement(zLessThanFive, List.of(xIsFive));

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(new Program(List.of(whileStatement)));

        // then
        assertEquals(1, results.size());
        assertEquals(xIsFive, results.getFirst());
    }

    @Test
    void shouldHandleIfStatement() {
        // given
        Statement ifStatement = new IfStatement(zLessThanFive, List.of(xIsFive), List.of());

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(new Program(List.of(ifStatement)));

        // then
        assertEquals(1, results.size());
        assertEquals(xIsFive, results.getFirst());
    }

    @Test
    void shouldHandleIfElseStatement() {
        // given
        Statement ifStatement = new IfStatement(zLessThanFive, List.of(xIsFive), List.of(xIsSix));

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(new Program(List.of(ifStatement)));

        // then
        assertEquals(2, results.size());
        assertEquals(xIsFive, results.getFirst());
        assertEquals(xIsSix, results.get(1));
    }

    @Test
    void shouldHandleUsingVariableFromIfElseStatement() {
        // given
        Statement ifStatement = new IfStatement(zLessThanFive, List.of(xIsFive), List.of(xIsSix));

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(new Program(List.of(ifStatement, yIsXPlusTwo)));

        // then
        assertEquals(1, results.size());
        assertEquals(yIsXPlusTwo, results.getFirst());
    }


}