package com.kw.parserProject;

import com.kw.parserProject.statements.*;
import com.kw.parserProject.tokens.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    Parser parser;

    @BeforeEach
    void setUp() {
        parser = new Parser();
    }


    @Test
    void shouldRecognizeAssignment() {
        // when
        List<Statement> statements = parser.parse(List.of(new VariableToken("x"), new AssignmentToken(), new ConstantToken("25")));

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());
        assertEquals(0, actualStatement.readVariables().size());
    }

    @Test
    void shouldRecognizeAssignmentWithOperation() {
        // when
        List<Statement> statements = parser.parse(List.of(new VariableToken("x"), new AssignmentToken(),
                new ConstantToken("25"), new OperatorToken("+"), new VariableToken("x")));

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());
        assertIterableEquals(List.of("x"), actualStatement.readVariables());
    }

    @Test
    void shouldRecognizeAssignmentWithChainOperation() {
        // when
        // x = y - x * 2
        List<Statement> statements = parser.parse(List.of(new VariableToken("x"), new AssignmentToken(),
                new VariableToken("y"), new OperatorToken("-"), new VariableToken("x"), new OperatorToken("*"), new ConstantToken("2")));

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        // y - rightExpression
        Expression topTierExpression = actualStatement.getExpression();
        assertOperator(topTierExpression, "-");
        assertVariableExpression(topTierExpression.getLeftExpression(), "y");

        // rightExpression -> x * 2
        Expression rightExpression = topTierExpression.getRightExpression();
        assertOperator(rightExpression, "*");
        assertVariableExpression(rightExpression.getLeftExpression(), "x");
        assertValueExpression(rightExpression.getRightExpression(), 2);

        assertIterableEquals(List.of("x", "y"), actualStatement.readVariables());

        assertIterableEquals(List.of("x", "y"), actualStatement.readVariables());
    }

    @Test
    void shouldRecognizeAssignmentWithChainOperationWithOperatorBalancing() {
        // when
        // operator balancing not yet implemented fully
        // x = x * 2 - y
        List<Statement> statements = parser.parse(List.of(new VariableToken("x"), new AssignmentToken(),
                new VariableToken("x"), new OperatorToken("*"), new ConstantToken("2"), new OperatorToken("-"), new VariableToken("y")));

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        // leftExpression - 2
        Expression topTierExpression = actualStatement.getExpression();
        assertOperator(topTierExpression, "-");

        assertVariableExpression(topTierExpression.getRightExpression(), "y");

        // leftExpression -> x * 2
        Expression leftExpression = topTierExpression.getLeftExpression();
        assertOperator(leftExpression, "*");
        assertVariableExpression(leftExpression.getLeftExpression(), "x");
        assertValueExpression(leftExpression.getRightExpression(), 2);

        assertIterableEquals(List.of("x", "y"), actualStatement.readVariables());
    }

    @Test
    void shouldRecognizeAssignmentWithChainOperationAndBracketsWithOperatorBalancing() {
        // when
        // operator balancing not yet implemented fully
        // x = x * (2 - y)
        List<Statement> statements = parser.parse(List.of(new VariableToken("x"), new AssignmentToken(),
                new VariableToken("x"), new OperatorToken("*"),
                new OpeningBracketToken(), new ConstantToken("2"), new OperatorToken("-"), new VariableToken("y"), new ClosingBracketToken()));

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        Expression topTierExpression = actualStatement.getExpression();
        // x * rightExpression
        assertOperator(topTierExpression, "*");
        assertVariableExpression(topTierExpression.getLeftExpression(), "x");
        assertTrue(topTierExpression.getRightExpression().isBracketExpression());

        // rightExpression -> expressionInBracket -> ( 2 - y )
        Expression expressionInBrackets = topTierExpression.getRightExpression().getExpressionInBrackets();
        // 2 - y
        assertOperator(expressionInBrackets, "-");
        assertValueExpression(expressionInBrackets.getLeftExpression(), 2);
        assertVariableExpression(expressionInBrackets.getRightExpression(), "y");

        assertIterableEquals(List.of("x", "y"), actualStatement.readVariables());
    }

    @Test
    void shouldRecognizeAssignmentWithMultipleMultiplicationsWithOperatorBalancing() {
        // when
        // operator balancing not yet implemented fully
        // x = 2 * x + y / 5
        List<Statement> statements = parser.parse(List.of(new VariableToken("x"), new AssignmentToken(),
                new ConstantToken("2"), new OperatorToken("*"), new VariableToken("x"),
                new OperatorToken("+"),
                new VariableToken("y"), new OperatorToken("/"), new ConstantToken("5")));

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        Expression topTierExpression = actualStatement.getExpression();
        // leftExpression * rightExpression
        assertOperator(topTierExpression, "+");

        // leftExpression -> 2 * x
        Expression leftExpression = topTierExpression.getLeftExpression();
        assertOperator(leftExpression, "*");

        assertValueExpression(leftExpression.getLeftExpression(), 2);
        assertVariableExpression(leftExpression.getRightExpression(), "x");

        // rightExpression -> y / 5
        Expression rightExpression = topTierExpression.getRightExpression();
        assertOperator(rightExpression, "/");

        assertVariableExpression(rightExpression.getLeftExpression(), "y");
        assertValueExpression(rightExpression.getRightExpression(), 5);

        assertIterableEquals(List.of("x", "y"), actualStatement.readVariables());
    }

    @Test
    void shouldHandleMultipleMultiplicationsWithOperatorBalancing() {
        // when
        // operator balancing not yet implemented fully
        // x = x * y * z / a
        List<Statement> statements = parser.parse(List.of(new VariableToken("x"), new AssignmentToken(),
                new VariableToken("x"),
                new OperatorToken("*"), new VariableToken("y"),
                new OperatorToken("*"), new VariableToken("z"),
                new OperatorToken("/"), new VariableToken("a")));

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        Expression topTierExpression = actualStatement.getExpression();
        // leftExpression / a
        assertOperator(topTierExpression, "/");

        Expression rightExpression = topTierExpression.getRightExpression();
        assertVariableExpression(rightExpression, "a");

        // leftExpression -> x * y * z
        // x -> left, y * z -> right, due to rotation
        Expression secondLevelLeftExpression = topTierExpression.getLeftExpression();
        assertOperator(secondLevelLeftExpression, "*");

        assertVariableExpression(secondLevelLeftExpression.getLeftExpression(), "x");
        Expression thirdTierExpression = secondLevelLeftExpression.getRightExpression();
        assertOperator(thirdTierExpression, "*");
        assertVariableExpression(thirdTierExpression.getLeftExpression(), "y");
        assertVariableExpression(thirdTierExpression.getRightExpression(), "z");

        assertIterableEquals(List.of("a", "x", "y", "z"), actualStatement.readVariables());
    }

    @Test
    void shouldHandleBracketsAndMultipleOperatorsWithOperatorBalancing() {
        // when
        // operator balancing not yet implemented fully
        // x = x * ( y + z ) / a
        List<Statement> statements = parser.parse(List.of(new VariableToken("x"), new AssignmentToken(),
                new VariableToken("x"), new OperatorToken("*"),
                new OpeningBracketToken(), new VariableToken("y"), new OperatorToken("+"), new VariableToken("z"), new ClosingBracketToken(),
                new OperatorToken("/"), new VariableToken("a")));

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        Expression topTierExpression = actualStatement.getExpression();
        // leftExpression / a
        assertOperator(topTierExpression, "/");

        Expression rightExpression = topTierExpression.getRightExpression();
        assertVariableExpression(rightExpression, "a");

        // x * ( y + z )
        Expression secondLevelLeftExpression = topTierExpression.getLeftExpression();
        assertOperator(secondLevelLeftExpression, "*");

        assertVariableExpression(secondLevelLeftExpression.getLeftExpression(), "x");
        Expression thirdTierExpression = secondLevelLeftExpression.getRightExpression();

        assertTrue(thirdTierExpression.isBracketExpression());
        Expression expressionInBracket = thirdTierExpression.getExpressionInBrackets();

        assertVariableExpression(expressionInBracket.getLeftExpression(), "y");
        assertVariableExpression(expressionInBracket.getRightExpression(), "z");

        assertIterableEquals(List.of("a", "x", "y", "z"), actualStatement.readVariables());
    }


    @Test
    void shouldRecognizeAssignmentReadingVariable() {
        // when
        List<Statement> statements = parser.parse(List.of(new VariableToken("x"), new AssignmentToken(), new VariableToken("y")));

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());
        assertEquals(1, actualStatement.readVariables().size());

        assertEquals("y", actualStatement.readVariables().getFirst());
    }

    @Test
    void shouldHandleIfStatementWithoutElseClause() {
        // when
        List<Statement> statements = parser.parse(
                List.of(new KeywordToken("if"), new VariableToken("a"), new OperatorToken("<"), new ConstantToken("3"),
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("y"),
                        new KeywordToken("end")));

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(IfStatement.class, statements.getFirst());

        IfStatement actualStatement = (IfStatement) statements.getFirst();
        assertNull(actualStatement.writeVariable());

        // if-clause - one assignment
        assertEquals(1, actualStatement.getIfClauseStatements().size());

        // else clause - empty
        assertEquals(0, actualStatement.getElseClauseStatements().size());

        // read variables from condition
        assertEquals(1, actualStatement.readVariables().size());
        assertEquals("a", actualStatement.readVariables().getFirst());
    }

    @Test
    void shouldHandleIfStatementWithElseClause() {
        // when
        List<Statement> statements = parser.parse(
                List.of(new KeywordToken("if"), new VariableToken("a"), new OperatorToken("<"), new ConstantToken("3"),
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("y"),
                        new KeywordToken("else"),
                        new VariableToken("y"), new AssignmentToken(), new VariableToken("x"),
                        new KeywordToken("end")));

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(IfStatement.class, statements.getFirst());

        IfStatement actualStatement = (IfStatement) statements.getFirst();
        assertNull(actualStatement.writeVariable());

        // if-clause - one assignment
        assertEquals(1, actualStatement.getIfClauseStatements().size());

        // else clause - one assignment
        assertEquals(1, actualStatement.getElseClauseStatements().size());

        // read variables from condition
        assertEquals(1, actualStatement.readVariables().size());
        assertEquals("a", actualStatement.readVariables().getFirst());
    }

    @Test
    void shouldHandleWhileStatement() {
        // when
        List<Statement> statements = parser.parse(
                List.of(new KeywordToken("while"), new VariableToken("a"), new OperatorToken("<"), new ConstantToken("3"),
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("y"),
                        new KeywordToken("end")));

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(WhileStatement.class, statements.getFirst());

        WhileStatement actualStatement = (WhileStatement) statements.getFirst();
        assertNull(actualStatement.writeVariable());

        assertEquals(1, actualStatement.subStatements().size());

        // read variables from condition - this part might require better handling/asserts
        assertEquals(0, actualStatement.readVariables().size());
        assertEquals(1, actualStatement.postVisitReadVariable().size());
        assertEquals("a", actualStatement.postVisitReadVariable().getFirst());
    }

    @Test
    void shouldHandleLackOfEndKeywordInWhileStatement() {
        // when
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                List.of(new KeywordToken("while"), new VariableToken("a"), new OperatorToken("<"), new ConstantToken("3"),
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("y"))));
    }

    @Test
    void shouldHandleDifferentKeywordTokenInWhileStatement() {
        // when
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                List.of(new KeywordToken("while"), new VariableToken("a"), new OperatorToken("<"), new ConstantToken("3"),
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("y"),
                        new KeywordToken("else"), new ConstantToken("2"), new KeywordToken("end"))));
    }

    @Test
    void shouldHandleDifferentTypeTokenInWhileStatement() {
        // something reasonable, but not parsable to expression
        // when
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                List.of(new KeywordToken("while"), new VariableToken("a"), new OperatorToken("<"), new ConstantToken("3"),
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("y"),
                        new ConstantToken("2"), new KeywordToken("end"))));
    }

    @Test
    void shouldHandleBogusTokenAfterWhileStatement() {
        // something unreasonable, like closing bracket
        // when
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                List.of(new KeywordToken("while"),
                        new ClosingBracketToken(), new AssignmentToken(), new VariableToken("y"))
        ));
    }

    @Test
    void shouldHandleDeadSilenceAfterWhileStatement() {
        // when
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                List.of(new VariableToken("x"), new AssignmentToken(), new VariableToken("y"),
                        new KeywordToken("while"))));
    }

    @Test
    void shouldHandleNonExpressionAfterWhileStatement() {
        // when
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                List.of(new KeywordToken("while"),
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("y"))
        ));
    }

    @Test
    void shouldHandleEndingStatementListRandomly() {
        // fails due to not consuming all tokens; any non-statement can be used
        // when
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                List.of(new VariableToken("x"), new AssignmentToken(), new VariableToken("y"),
                        new KeywordToken("end"))
        ));
    }

    private static void assertOperator(Expression operatorExpression, String expected) {
        assertTrue(operatorExpression.isOperatorExpression());
        assertEquals(expected, operatorExpression.getOperator());
    }

    private static void assertValueExpression(Expression valueExpression, int expected) {
        assertTrue(valueExpression.isValueExpression());
        assertEquals(expected, valueExpression.getValue());
    }

    private static void assertVariableExpression(Expression variableExpression, String expected) {
        assertTrue(variableExpression.isVariableExpression());
        assertEquals(expected, variableExpression.getVariable());
    }

}