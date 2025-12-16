package com.kw.parserProject;

import com.kw.parserProject.statements.*;
import com.kw.parserProject.tokens.*;
import com.kw.parserProject.utility.Pair;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Parser {

    List<Token> tokens = new LinkedList<>();

    List<Statement> parse(String programCode) {
        // read input into list of tokens
        InputStream inputStream = new ByteArrayInputStream(programCode.getBytes());

        List<String> lines;
        try (Reader reader = new InputStreamReader(inputStream, Charset.defaultCharset());
             BufferedReader buffer = new BufferedReader(reader)) {
            lines = buffer.readAllLines();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        lines.forEach(line -> {
            char[] charArray = line.toCharArray();
            int startingPos = 0;

            while (startingPos != -1) {
                startingPos = readNextToken(charArray, startingPos);
            }
        });

        // transform tokens into statements
        Pair<Integer, List<Statement>> program = readStatementList(new LinkedList<>(tokens), 0);

        return program.right();
    }

    private Pair<Integer, List<Statement>> readStatementList(List<Token> tokens, int startIndex) {
        List<Statement> statements = new ArrayList<>();
        Pair<Integer, Statement> resultIndex = readConditionStatement(tokens, startIndex);
        int previousSuccessIndex = -1;
        while (resultIndex.left() != -1) {
            previousSuccessIndex = resultIndex.left();
            statements.add(resultIndex.right());
            resultIndex = readConditionStatement(tokens, resultIndex.left());
        }
        return new Pair<>(previousSuccessIndex, statements);
    }


    private Pair<Integer, Statement> readConditionStatement(List<Token> tokens, int startIndex) {
        // variable = expression
        // if expression statementList end
        // if expression statementList else statementList end
        // while expression statementList end

        Token token = tryReadingToken(tokens, startIndex);
        if (token instanceof KeywordToken) {
            if ("if".equals(token.data)) {
                Pair<Integer, Expression> i = readExpressionWithBrackets(tokens, startIndex + 1);
                if (i.left() == -1) {
                    throw new IllegalStateException("Bad IF statement");
                }
                Pair<Integer, List<Statement>> readStatementListIfClauseResults = readStatementList(tokens, i.left());
                int endStatementIndex = readStatementListIfClauseResults.left();
                // expect a keyword
                if (endStatementIndex == -1) {
                    throw new IllegalStateException("Couldn't read com.kw.parserProject.statements");
                }
                Token secondToken = tokens.get(endStatementIndex);
                if (!(secondToken instanceof KeywordToken)) {
                    throw new IllegalStateException("Bad IF statement");
                }

                if ("else".equals(secondToken.data)) {
                    Pair<Integer, List<Statement>> readStatementListResults = readStatementList(tokens, endStatementIndex + 1);
                    int elseIfStatementIndex = readStatementListResults.left();
                    if (elseIfStatementIndex == -1) {
                        throw new IllegalStateException("BAD ELSEIF");
                    }
                    Token endKeyword = tokens.get(elseIfStatementIndex);
                    if (!("end".equals(endKeyword.data))) {
                        throw new IllegalStateException("No end keyword after if-else");
                    }
                    Statement statement = new IfStatement(i.right(), readStatementListIfClauseResults.right(), readStatementListResults.right(), tokens.subList(startIndex, elseIfStatementIndex + 1).toString());
                    return new Pair<>(elseIfStatementIndex + 1, statement);
                } else if (("end".equals(secondToken.data))) {
                    Statement ifStatement = new IfStatement(i.right(), readStatementListIfClauseResults.right(), List.of(), tokens.subList(startIndex, endStatementIndex + 1).toString());
                    return new Pair<>(endStatementIndex + 1, ifStatement);
                }
            } else if ("while".equals(token.data)) {
                Pair<Integer, Expression> i = readExpressionWithBrackets(tokens, startIndex + 1);
                if (i.left() == -1) {
                    throw new IllegalStateException("Bad while statement");
                }
                //statementList

                Pair<Integer, List<Statement>> readStatementListResults = readStatementList(tokens, i.left());
                int elseIfStatementIndex = readStatementListResults.left();
                if (elseIfStatementIndex == -1) {
                    throw new IllegalStateException("BAD ELSEIF");
                }
                // end keyword
                Token endKeyword = tokens.get(elseIfStatementIndex);
                if (!("end".equals(endKeyword.data))) {
                    throw new IllegalStateException("No end keyword after while");
                }

                Statement whileStatement = new WhileStatement(i.right(), readStatementListResults.right(), tokens.subList(startIndex, elseIfStatementIndex + 1).toString());
                return new Pair<>(elseIfStatementIndex + 1, whileStatement);
            }
        } else if (token instanceof VariableToken) {
            // might be a simple assignment
            return readAssignmentStatement(tokens, startIndex);
        }
        // no token found or token of different type
        return new Pair<>(-1, null);
    }

    private Pair<Integer, Statement> readAssignmentStatement(List<Token> tokens, int startIndex) {
        // variable = expression
        // if expression statementList end
        // if expression statementList else statementList end
        // while expression statementList end

        Token token = tokens.get(startIndex);
        if (token instanceof VariableToken) {
            Token assignmentToken = tokens.get(startIndex + 1);
            if (!(assignmentToken instanceof AssignmentToken)) {
                throw new IllegalStateException("Expecting = sign");
            }

            Pair<Integer, Expression> cursor = readExpressionWithBrackets(tokens, startIndex + 2);
            if (cursor.left() == -1) {
                throw new IllegalStateException("Couldn't read the expression to assign");
            }

            Statement statement = new Assignment(token.data, cursor.right(), tokens.subList(startIndex, cursor.left()).stream().map(Token::toString).collect(Collectors.joining()));
            return new Pair<>(cursor.left(), statement);
        }
        return new Pair<>(-1, null);
    }

    List<String> findVariableTokensUsed(List<Token> tokens) {
        return tokens.stream()
                .filter(token -> token instanceof VariableToken)
                .map(token -> token.data)
                .toList();
    }

    Pair<Integer, Expression> readExpressionWithBrackets(List<Token> tokens, int startIndex) {
        Token token = tokens.get(startIndex);
        if (token instanceof OpeningBracketToken) {
            Pair<Integer, Expression> cursor = readExpressionWithBrackets(tokens, startIndex + 1);
            if (!(tokens.get(cursor.left()) instanceof ClosingBracketToken)) {
                throw new IllegalStateException("No closing brackets");
            }
            List<String> variableTokensUsed = findVariableTokensUsed(tokens.subList(startIndex, cursor.left()));
            Expression e = new Expression(variableTokensUsed, tokens.subList(startIndex, cursor.left()).toString());
            return new Pair<>(cursor.left() + 1, e);
        }
        return readExpression(tokens, startIndex);
    }

    Pair<Integer, Expression> readExpression(List<Token> tokens, int startIndex) {
        Token token = tryReadingToken(tokens, startIndex);
        if (token instanceof VariableToken || token instanceof ConstantToken) {
            // we're good

            Token nextToken = tryReadingToken(tokens, startIndex + 1);
            if (nextToken instanceof OperatorToken) {
                // call readExpressionWithBrackets
                Pair<Integer, Expression> endIndex = readExpressionWithBrackets(tokens, startIndex + 2);
                // merge
                // rethink BODMAS rules here when merging - it doesn't matter for recognizing usages, but might be required to help with dead code pruning
                List<String> variableTokensUsed = findVariableTokensUsed(tokens.subList(startIndex, endIndex.left()));
                Expression e = new Expression(variableTokensUsed, tokens.subList(startIndex, endIndex.left()).toString());
                return new Pair<>(endIndex.left(), e);
            } else {
                List<String> variableTokensUsed = findVariableTokensUsed(tokens.subList(startIndex, startIndex + 1));
                Expression e = new Expression(variableTokensUsed, tokens.subList(startIndex, startIndex + 1).toString());
                return new Pair<>(startIndex + 1, e);
            }
        }
        return new Pair<>(-1, null);
    }

    Token tryReadingToken(List<Token> tokens, int index) {
        if (index >= tokens.size()) {
            return null;
        }
        return tokens.get(index);
    }


    int readNextToken(char[] input, int startingPos) {
        if (startingPos >= input.length) return -1;
        char character = input[startingPos];
        if (Character.isWhitespace(character)) {
            return startingPos + 1;
        }

        if (Character.isDigit(character)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(character);

            int i = startingPos + 1;
            while (i < input.length && Character.isDigit(input[i])) {
                stringBuilder.append(input[i]);
                i++;
            }

            tokens.add(new ConstantToken(stringBuilder.toString()));
            return i;
        }
        if (character >= 'a' && character <= 'z') {
            // check for reserved keywords
            int i = readReservedKeyword(input, startingPos);
            if (i != -1) return i;
            tokens.add(new VariableToken(Character.toString(character)));
            return startingPos + 1;
        }
        if (character == '=') {
            tokens.add(new AssignmentToken("="));
            return startingPos + 1;
        }
        if (character == '+' || character == '-' || character == '/' || character == '*') {
            tokens.add(new OperatorToken(Character.toString(character)));
            return startingPos + 1;
        }
        if (character == '(') {
            tokens.add(new OpeningBracketToken("("));
            return startingPos + 1;
        }
        if (character == ')') {
            tokens.add(new ClosingBracketToken(")"));
            return startingPos + 1;
        }
        if (character == '<') {
            tokens.add(new OperatorToken(Character.toString(character)));
            return startingPos + 1;
        }
        if (character == '>') {
            tokens.add(new OperatorToken(Character.toString(character)));
            return startingPos + 1;
        }
        return -1;
    }

    int readReservedKeyword(char[] input, int startingPos) {
        if (startingPos >= input.length) return -1;
        if (startingPos + 1 < input.length && input[startingPos] == 'i' && input[startingPos + 1] == 'f') {
            tokens.add(new KeywordToken("if"));
            return startingPos + 2;
        }
        if (startingPos + 2 < input.length && input[startingPos] == 'e' && input[startingPos + 1] == 'n' && input[startingPos + 2] == 'd') {
            tokens.add(new KeywordToken("end"));
            return startingPos + 3;
        }
        if (startingPos + 3 < input.length && input[startingPos] == 'e' && input[startingPos + 1] == 'l' && input[startingPos + 2] == 's' && input[startingPos + 3] == 'e') {
            tokens.add(new KeywordToken("else"));
            return startingPos + 4;
        }
        if (startingPos + 4 < input.length && input[startingPos] == 'w' && input[startingPos + 1] == 'h' && input[startingPos + 2] == 'i' && input[startingPos + 3] == 'l' && input[startingPos + 4] == 'e') {
            tokens.add(new KeywordToken("while"));
            return startingPos + 5;
        }
        return -1;
    }

    static void main() {
        String programCode = """
                a = 1
                b = a
                x = 3
                y = 4
                while (b < 5)
                  z = x
                  b = b + 1
                  x = 9
                  y = 10
                end
                """;
        List<Statement> program = new Parser().parse(programCode);

        List<Statement> statementStream2 = new UnusedStatementChecker().getUnusedStatements(program);
        System.out.println("UNUSED VARIABLES FROM STATEMENTS: " + statementStream2.size());
        System.out.println(statementStream2);
    }
}
