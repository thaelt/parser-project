package com.kw.parserProject;

import com.kw.parserProject.statements.*;
import com.kw.parserProject.tokens.*;
import com.kw.parserProject.utility.ReadResults;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Parser {

    public Program parse(List<Token> tokens) {
        ReadResults<Integer, List<Statement>> program = readStatementList(new ArrayList<>(tokens), 0);
        if (program.nextIndex() != tokens.size()) {
            throw new IllegalArgumentException("Did not manage to consume all tokens");
        }

        return new Program(program.value());
    }

    private ReadResults<Integer, List<Statement>> readStatementList(List<Token> tokens, int startIndex) {
        List<Statement> statements = new ArrayList<>();
        ReadResults<Integer, Statement> resultIndex = readConditionStatement(tokens, startIndex);
        if (resultIndex.nextIndex() == -1) {
            throw new IllegalArgumentException("Expecting at least one statement");
        }
        int previousSuccessIndex = -1;
        while (resultIndex.nextIndex() != -1) {
            previousSuccessIndex = resultIndex.nextIndex();
            statements.add(resultIndex.value());
            resultIndex = readConditionStatement(tokens, resultIndex.nextIndex());
        }
        return new ReadResults<>(previousSuccessIndex, statements);
    }

    private ReadResults<Integer, Statement> readConditionStatement(List<Token> tokens, int startIndex) {
        Token token = tryReadingToken(tokens, startIndex);

        return switch (token) {
            case KeywordToken keywordToken -> handleKeywordToken(tokens, startIndex, keywordToken);
            case VariableToken variableToken -> handleAssignmentStatement(tokens, startIndex, variableToken);
            case null, default -> new ReadResults<>(-1, null);
        };
    }

    private ReadResults<Integer, Statement> handleKeywordToken(List<Token> tokens, int startIndex, KeywordToken token) {
        return switch (token.data) {
            case "if" -> handleIfStatement(tokens, startIndex);
            case "while" -> handleWhileStatement(tokens, startIndex);
            case null, default -> new ReadResults<>(-1, null);
        };
    }

    private ReadResults<Integer, Statement> handleWhileStatement(List<Token> tokens, int startIndex) {
        ReadResults<Integer, Expression> whileExpressionReadResults = readExpressionWithBrackets(tokens, startIndex + 1);
        assertTokenIsPresent(whileExpressionReadResults, "Expecting expression after 'while' keyword, did not encounter one");

        //statementList
        ReadResults<Integer, List<Statement>> readStatementListResults = readStatementList(tokens, whileExpressionReadResults.nextIndex());
        assertTokenIsPresent(readStatementListResults, "Expecting statements in 'while' loop, did not encounter one");

        int endStatementIndex = readStatementListResults.nextIndex();
        // end keyword
        Token endKeyword = tryReadingToken(tokens, endStatementIndex);
        assertTokenIsEndKeyword(endKeyword);

        Statement whileStatement = new WhileStatement(whileExpressionReadResults.value(), readStatementListResults.value());
        return new ReadResults<>(endStatementIndex + 1, whileStatement);
    }

    private ReadResults<Integer, Statement> handleIfStatement(List<Token> tokens, int startIndex) {
        ReadResults<Integer, Expression> tryReadingIfCondition = readExpressionWithBrackets(tokens, startIndex + 1);
        assertTokenIsPresent(tryReadingIfCondition, "Expecting condition in 'if' statement, did not encounter one");

        ReadResults<Integer, List<Statement>> readStatementListIfClauseResults = readStatementList(tokens, tryReadingIfCondition.nextIndex());
        assertTokenIsPresent(readStatementListIfClauseResults, "Expecting statements in 'if' section, did not encounter one");

        int potentialEndKeywordIndex = readStatementListIfClauseResults.nextIndex();
        Token secondToken = tokens.get(potentialEndKeywordIndex);
        assertTokenIsOfType(secondToken, KeywordToken.class);

        List<Statement> ifClauseStatements = readStatementListIfClauseResults.value();
        List<Statement> elseIfClauseStatements = List.of(); // default else clause is empty if undefined

        if ("else".equals(secondToken.data)) {
            ReadResults<Integer, List<Statement>> elseSectionReadResults = readStatementList(tokens, potentialEndKeywordIndex + 1);
            assertTokenIsPresent(elseSectionReadResults, "Expecting statements in 'else' section, did not encounter one");

            elseIfClauseStatements = elseSectionReadResults.value();
            potentialEndKeywordIndex = elseSectionReadResults.nextIndex();
        }
        Token endKeyword = tokens.get(potentialEndKeywordIndex);
        assertTokenIsEndKeyword(endKeyword);

        Statement statement = new IfStatement(tryReadingIfCondition.value(), ifClauseStatements, elseIfClauseStatements);
        return new ReadResults<>(potentialEndKeywordIndex + 1, statement);
    }

    private ReadResults<Integer, Statement> handleAssignmentStatement(List<Token> tokens, int startIndex, VariableToken variableToken) {
        Token assignmentToken = tryReadingToken(tokens, startIndex + 1);
        assertTokenIsOfType(assignmentToken, AssignmentToken.class);

        ReadResults<Integer, Expression> assignmentReadResults = readExpressionWithBrackets(tokens, startIndex + 2);
        assertTokenIsPresent(assignmentReadResults, "Expecting expression to assign, did not encounter one");

        Statement statement = new Assignment(variableToken.data, assignmentReadResults.value());
        return new ReadResults<>(assignmentReadResults.nextIndex(), statement);
    }

    private ReadResults<Integer, Expression> readExpressionWithBrackets(List<Token> tokens, int startIndex) {
        Token token = tryReadingToken(tokens, startIndex);
        return switch (token) {
            case OpeningBracketToken _ -> handleOpeningBracket(tokens, startIndex);
            case VariableToken variableToken -> chainIfPossible(tokens, startIndex, wrapInExpression(variableToken));
            case ConstantToken constantToken -> chainIfPossible(tokens, startIndex, wrapInExpression(constantToken));
            case null, default -> new ReadResults<>(-1, null);
        };
    }

    private ReadResults<Integer, Expression> handleOpeningBracket(List<Token> tokens, int startIndex) {
        ReadResults<Integer, Expression> expressionReadResults = readExpressionWithBrackets(tokens, startIndex + 1);
        assertTokenIsPresent(expressionReadResults, "Expecting an expression in brackets, did not encounter one");

        Token closingBracket = tokens.get(expressionReadResults.nextIndex());
        assertTokenIsOfType(closingBracket, ClosingBracketToken.class);

        Expression expression = new BracketExpression(expressionReadResults.value());

        return chainIfPossible(tokens, expressionReadResults.nextIndex(), expression);
    }

    private ReadResults<Integer, Expression> chainIfPossible(List<Token> tokens, int startIndex, Expression expression) {
        int potentialLastTokenIndex = startIndex + 1;
        Token nextToken = tryReadingToken(tokens, startIndex + 1);
        if (nextToken instanceof OperatorToken) {
            return chainExpressions(tokens, expression, potentialLastTokenIndex, nextToken);
        }

        return new ReadResults<>(potentialLastTokenIndex, expression);
    }

    private ReadResults<Integer, Expression> chainExpressions(List<Token> tokens, Expression expression, int potentialLastTokenIndex, Token nextToken) {
        ReadResults<Integer, Expression> endIndex = readExpressionWithBrackets(tokens, potentialLastTokenIndex + 1);
        assertTokenIsPresent(endIndex, "Expecting an expression, did not encounter one");

        // we're collecting chained assignments from right to left, due to recursion call.
        // it is not a problem with basic unused variable usage analysis as order of operation is not important for it.
        // rotating helps to fix some discrepancies with operation order.
        // it is not perfect (should use left predecessor rule, not right child rule), but works
        String operator = nextToken.data;
        Expression rightSideExpression = endIndex.value();
        if (("*".equals(operator) || "/".equals(operator)) && rightSideExpression instanceof OperatorExpression operatorExpression) {
            Expression rotatedExpression = rotateExpressions(expression, operator, operatorExpression);
            return new ReadResults<>(endIndex.nextIndex(), rotatedExpression);
        }

        Expression chainedExpression = new OperatorExpression(expression, operator, rightSideExpression);
        return new ReadResults<>(endIndex.nextIndex(), chainedExpression);
    }

    private static Expression rotateExpressions(Expression leftSideExpression, String operator, OperatorExpression rightSideExpression) {
        Expression newLeftExpression = new OperatorExpression(leftSideExpression, operator, rightSideExpression.leftExpression());
        String newTopOperator = rightSideExpression.operator();
        Expression newRightExpression = rightSideExpression.rightExpression();

        return new OperatorExpression(newLeftExpression, newTopOperator, newRightExpression);
    }

    private Expression wrapInExpression(VariableToken variableToken) {
        return new VariableExpression(variableToken.data);
    }

    private Expression wrapInExpression(ConstantToken constantToken) {
        return new ValueExpression(Integer.parseInt(constantToken.data));
    }

    private Token tryReadingToken(List<Token> tokens, int index) {
        if (index >= tokens.size()) {
            return null;
        }
        return tokens.get(index);
    }

    private static void assertTokenIsPresent(ReadResults<Integer, ?> readingResult, String errorMessage) {
        if (readingResult.nextIndex() == -1) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private static void assertTokenIsOfType(Token tokenToCheck, Class<? extends Token> tokenClass) {
        if (!tokenClass.isInstance(tokenToCheck)) {
            throw new IllegalArgumentException("Expected " + tokenClass.getName() + ", got " + (tokenToCheck == null ? "null" : tokenToCheck.getClass().toString()));
        }
    }

    private static void assertTokenIsEndKeyword(Token tokenToCheck) {
        assertTokenIsOfType(tokenToCheck, KeywordToken.class);
        if (!Objects.equals(tokenToCheck.data, "end")) {
            throw new IllegalArgumentException("Expected a token with data: " + "end" + ", got " + tokenToCheck.data);
        }
    }
}
