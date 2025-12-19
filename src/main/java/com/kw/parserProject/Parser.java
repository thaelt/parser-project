package com.kw.parserProject;

import com.kw.parserProject.statements.*;
import com.kw.parserProject.tokens.*;
import com.kw.parserProject.utility.ReadResults;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

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
            case "if" -> handleIfStatement(tokens, startIndex, token);
            case "while" -> handleWhileStatement(tokens, startIndex, token);
            case null, default -> new ReadResults<>(-1, null);
        };
    }

    private ReadResults<Integer, Statement> handleWhileStatement(List<Token> tokens, int startIndex, KeywordToken token) {
        ReadResults<Integer, Expression> whileExpressionReadResults = readExpressionWithBrackets(tokens, startIndex + 1);
        assertTokenIsPresent(whileExpressionReadResults, "Expecting expression after 'while' keyword, did not encounter one");

        //statementList
        ReadResults<Integer, List<Statement>> readStatementListResults = readStatementList(tokens, whileExpressionReadResults.nextIndex());
        assertTokenIsPresent(readStatementListResults, "Expecting statements in 'while' loop, did not encounter one");

        int endStatementIndex = readStatementListResults.nextIndex();
        // end keyword
        Token endKeyword = tryReadingToken(tokens, endStatementIndex);
        assertTokenIsEndKeyword(endKeyword);

        Statement whileStatement = new WhileStatement(whileExpressionReadResults.value(), readStatementListResults.value(), token.lineNumber);
        return new ReadResults<>(endStatementIndex + 1, whileStatement);
    }

    private ReadResults<Integer, Statement> handleIfStatement(List<Token> tokens, int startIndex, KeywordToken token) {
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

        Statement statement = new IfStatement(tryReadingIfCondition.value(), ifClauseStatements, elseIfClauseStatements, token.lineNumber);
        return new ReadResults<>(potentialEndKeywordIndex + 1, statement);
    }

    private ReadResults<Integer, Statement> handleAssignmentStatement(List<Token> tokens, int startIndex, VariableToken variableToken) {
        Token assignmentToken = tryReadingToken(tokens, startIndex + 1);
        assertTokenIsOfType(assignmentToken, AssignmentToken.class);

        ReadResults<Integer, Expression> assignmentReadResults = readExpressionWithBrackets(tokens, startIndex + 2);
        assertTokenIsPresent(assignmentReadResults, "Expecting expression to assign, did not encounter one");

        Statement statement = new Assignment(variableToken.data, assignmentReadResults.value(), variableToken.lineNumber);
        return new ReadResults<>(assignmentReadResults.nextIndex(), statement);
    }

    private ReadResults<Integer, Expression> readExpressionWithBrackets(List<Token> tokens, int startIndex) {
        Token token = tryReadingToken(tokens, startIndex);
        return switch (token) {
            case OpeningBracketToken _ -> handleOpeningBracket(tokens, startIndex);
            case VariableToken variableToken -> chainIfPossible(tokens, startIndex, wrapInExpression(variableToken));
            case ConstantToken constantToken -> chainIfPossible(tokens, startIndex, wrapInExpression(constantToken));
            case OperatorToken operatorToken -> handleOperatorToken(tokens, startIndex, operatorToken);
            case null, default -> new ReadResults<>(-1, null);
        };
    }

    private ReadResults<Integer, Expression> handleOperatorToken(List<Token> tokens, int startIndex, OperatorToken token) {
        if (!"-".equals(token.data)) {
            return new ReadResults<>(-1, null);
        }

        Token constantToken = tryReadingToken(tokens, startIndex + 1);
        ConstantToken constantToNegate = assertTokenIsOfType(constantToken, ConstantToken.class);
        ValueExpression valueExpression = new ValueExpression("-" + constantToNegate.data);
        return chainIfPossible(tokens, startIndex + 1, valueExpression);
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
        assertTokenIsPresent(endIndex, "Expecting an expression, did not encounter valid one");

        // we're collecting chained assignments from right to left, due to recursion call.
        // it is not a problem with basic unused variable usage analysis as order of operation is not important for it.
        // however let's try fixing operator order
        String operator = nextToken.data;
        Expression rightSideExpression = endIndex.value();
        if (rightSideExpression instanceof OperatorExpression operatorExpression) {
            Expression rotatedExpression = rotateExpressions(expression, operator, operatorExpression);
            return new ReadResults<>(endIndex.nextIndex(), rotatedExpression);
        }

        Expression chainedExpression = new OperatorExpression(expression, operator, rightSideExpression);
        return new ReadResults<>(endIndex.nextIndex(), chainedExpression);
    }

    private static Expression rotateExpressions(Expression leftSideExpression, String operator, OperatorExpression rightSideExpression) {
        // logic might be slightly easier if tree was mutable...
        // push operator and left input argument as deep into the operator tree as possible and connect it with rightSideExpression's left predecessor
        // this will transform "left * (right + right2)" into "(left * right) + right2"
        // and should respect operator precedence and multiple chain levels (hopefully)
        Stack<Expression> rightExpressions = new Stack<>();
        Stack<String> operators = new Stack<>();

        Expression leftPointer = rightSideExpression;

        while (leftPointer instanceof OperatorExpression(
                Expression leftExpression, String rightOperator, Expression rightExpression
        ) && operatorHasHigherPrecedence(operator, rightOperator)) {
            // go deeper: put operator on stack, put right child expression on stack
            operators.push(rightOperator);
            rightExpressions.push(rightExpression);
            // move pointers
            leftPointer = leftExpression;
        }

        // create new leaf node - attach left part of expression to the operator
        Expression childRightExpression = leftPointer;
        leftPointer = new OperatorExpression(leftSideExpression, operator, childRightExpression);

        // reconstruct right part of the tree
        while (!rightExpressions.isEmpty()) {
            Expression newRightExpression = rightExpressions.pop();
            String newOperator = operators.pop();
            Expression newLeftExpression = leftPointer;

            leftPointer = new OperatorExpression(newLeftExpression, newOperator, newRightExpression);
        }
        return leftPointer;
    }

    private static boolean operatorHasHigherPrecedence(String left, String right) {
        if ("<".equals(left) || ">".equals(left)) return true;
        return ("*".equals(left) || "/".equals(left)) && !("<".equals(right) || ">".equals(right));
    }

    private Expression wrapInExpression(VariableToken variableToken) {
        return new VariableExpression(variableToken.data);
    }

    private Expression wrapInExpression(ConstantToken constantToken) {
        return new ValueExpression(constantToken.data);
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

    private static <T extends Token> T assertTokenIsOfType(Token tokenToCheck, Class<T> tokenClass) {
        if (!tokenClass.isInstance(tokenToCheck)) {
            throw new IllegalArgumentException("Expected " + tokenClass.getName() + ", got " + (tokenToCheck == null ? "null" : tokenToCheck.getClass().toString()));
        } else {
            return tokenClass.cast(tokenToCheck);
        }
    }

    private static void assertTokenIsEndKeyword(Token tokenToCheck) {
        assertTokenIsOfType(tokenToCheck, KeywordToken.class);
        if (!Objects.equals(tokenToCheck.data, "end")) {
            throw new IllegalArgumentException("Expected a token with data: " + "end" + ", got " + tokenToCheck.data);
        }
    }
}
