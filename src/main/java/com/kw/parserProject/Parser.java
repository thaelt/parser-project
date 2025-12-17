package com.kw.parserProject;

import com.kw.parserProject.statements.*;
import com.kw.parserProject.tokens.*;
import com.kw.parserProject.utility.ReadResults;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Parser {

    public List<Statement> parse(List<Token> tokens) {
        ReadResults<Integer, List<Statement>> program = readStatementList(new ArrayList<>(tokens), 0);

        return program.value();
    }

    private ReadResults<Integer, List<Statement>> readStatementList(List<Token> tokens, int startIndex) {
        List<Statement> statements = new ArrayList<>();
        ReadResults<Integer, Statement> resultIndex = readConditionStatement(tokens, startIndex);
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

        int elseIfStatementIndex = readStatementListResults.nextIndex();
        // end keyword
        Token endKeyword = tokens.get(elseIfStatementIndex);
        assertTokenIsEndKeyword(endKeyword);

        Statement whileStatement = new WhileStatement(whileExpressionReadResults.value(), readStatementListResults.value(), tokens.subList(startIndex, elseIfStatementIndex + 1).toString());
        return new ReadResults<>(elseIfStatementIndex + 1, whileStatement);
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

        Statement statement = new IfStatement(tryReadingIfCondition.value(), ifClauseStatements, elseIfClauseStatements, tokens.subList(startIndex, potentialEndKeywordIndex + 1).toString());
        return new ReadResults<>(potentialEndKeywordIndex + 1, statement);
    }

    private ReadResults<Integer, Statement> handleAssignmentStatement(List<Token> tokens, int startIndex, VariableToken variableToken) {
        Token assignmentToken = tryReadingToken(tokens, startIndex + 1);
        assertTokenIsOfType(assignmentToken, AssignmentToken.class);

        ReadResults<Integer, Expression> assignmentReadResults = readExpressionWithBrackets(tokens, startIndex + 2);
        assertTokenIsPresent(assignmentReadResults, "Expecting expression to assign, did not encounter one");

        Statement statement = new Assignment(variableToken.data, assignmentReadResults.value(), tokens.subList(startIndex, assignmentReadResults.nextIndex()).stream().map(Token::toString).collect(Collectors.joining()));
        return new ReadResults<>(assignmentReadResults.nextIndex(), statement);
    }

    private ReadResults<Integer, Expression> readExpressionWithBrackets(List<Token> tokens, int startIndex) {
        Token token = tryReadingToken(tokens, startIndex);
        return switch (token) {
            case OpeningBracketToken _ -> handleOpeningBracket(tokens, startIndex);
            case VariableToken _, ConstantToken _ -> handleVariableOrConstant(tokens, startIndex);
            case null, default -> new ReadResults<>(-1, null);
        };
    }

    private ReadResults<Integer, Expression> handleOpeningBracket(List<Token> tokens, int startIndex) {
        ReadResults<Integer, Expression> expressionReadResults = readExpressionWithBrackets(tokens, startIndex + 1);
        assertTokenIsPresent(expressionReadResults, "Expecting an expression in brackets, did not encounter one");

        Token closingBracket = tokens.get(expressionReadResults.nextIndex());
        assertTokenIsOfType(closingBracket, ClosingBracketToken.class);

        List<String> variableTokensUsed = findVariableTokensUsed(tokens.subList(startIndex, expressionReadResults.nextIndex()));
        Expression e = new Expression(variableTokensUsed, tokens.subList(startIndex, expressionReadResults.nextIndex()).toString());
        return new ReadResults<>(expressionReadResults.nextIndex() + 1, e);
    }

    private ReadResults<Integer, Expression> handleVariableOrConstant(List<Token> tokens, int startIndex) {
        int potentialLastTokenIndex = startIndex + 1;
        Token nextToken = tryReadingToken(tokens, startIndex + 1);
        if (nextToken instanceof OperatorToken) {
            ReadResults<Integer, Expression> endIndex = readExpressionWithBrackets(tokens, startIndex + 2);
            // to do merge, rethink BODMAS rules here - it doesn't matter for recognizing usages, but might be required to help with dead code pruning
            assertTokenIsPresent(endIndex, "Expecting an expression, did not encounter one");
            potentialLastTokenIndex = endIndex.nextIndex();
        }
        List<String> variableTokensUsed = findVariableTokensUsed(tokens.subList(startIndex, potentialLastTokenIndex));
        Expression e = new Expression(variableTokensUsed, tokens.subList(startIndex, potentialLastTokenIndex).toString());
        return new ReadResults<>(potentialLastTokenIndex, e);
    }

    private Token tryReadingToken(List<Token> tokens, int index) {
        if (index >= tokens.size()) {
            return null;
        }
        return tokens.get(index);
    }

    private List<String> findVariableTokensUsed(List<Token> tokens) {
        return tokens.stream()
                .filter(token -> token instanceof VariableToken)
                .map(token -> token.data)
                .toList();
    }

    private static void assertTokenIsPresent(ReadResults<Integer, ?> readingResult, String errorMessage) {
        if (readingResult.nextIndex() == -1) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private static void assertTokenIsOfType(Token tokenToCheck, Class<? extends Token> tokenClass) {
        if (!tokenClass.isInstance(tokenToCheck)) {
            throw new IllegalArgumentException("Expected " + tokenClass.getName() + ", got " + tokenToCheck.getClass());
        }
    }

    private static void assertTokenIsEndKeyword(Token tokenToCheck) {
        assertTokenIsOfType(tokenToCheck, KeywordToken.class);
        if (!Objects.equals(tokenToCheck.data, "end")) {
            throw new IllegalArgumentException("Expected a token with data: " + "end" + ", got " + tokenToCheck.data);
        }
    }
}
