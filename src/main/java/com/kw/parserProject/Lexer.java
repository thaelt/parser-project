package com.kw.parserProject;

import com.kw.parserProject.tokens.*;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class Lexer {
    private static final List<Character> RECOGNIZED_OPERATORS = List.of('+', '-', '*', '/', '<', '>');
    private static final List<String> RECOGNIZED_KEYWORDS = List.of("while", "else", "end", "if");

    public List<Token> extractTokens(String programCode) {
        return programCode.lines()
                .map(this::parseLine)
                .flatMap(Collection::stream)
                .toList();
    }

    List<Token> parseLine(String line) {
        List<Token> tokens = new LinkedList<>();

        char[] charArray = line.toCharArray();
        int startingPos = 0;

        while (startingPos != -1) {
            startingPos = readNextToken(charArray, startingPos, tokens);
        }

        return tokens;
    }

    private int readNextToken(char[] input, int startingPos, List<Token> tokens) {
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
            int readKeywordResults = readReservedKeyword(input, startingPos, tokens);
            if (readKeywordResults != -1) return readKeywordResults;
            String variableName = readIdentifier(input, startingPos);
            tokens.add(new VariableToken(variableName));
            return startingPos + 1;
        }
        if (RECOGNIZED_OPERATORS.contains(character)) {
            tokens.add(new OperatorToken(Character.toString(character)));
            return startingPos + 1;
        }
        if (character == '=') {
            tokens.add(new AssignmentToken());
            return startingPos + 1;
        }
        if (character == '(') {
            tokens.add(new OpeningBracketToken());
            return startingPos + 1;
        }
        if (character == ')') {
            tokens.add(new ClosingBracketToken());
            return startingPos + 1;
        }
        throw new IllegalArgumentException("Cannot recognize token at position: "+startingPos);
    }

    private String readIdentifier(char[] input, int startingPos) {
        int identifierLength = 0;
        int readTokensLimit = 5; // to avoid reading extremely long identifier which we'll fail anyway
        int lastIndex = startingPos;
        while (identifierLength < readTokensLimit &&
                input.length > lastIndex
                && input[lastIndex] >= 'a' && input[lastIndex] <= 'z') {
            lastIndex++;
            identifierLength++;
        }

        String identifier = new String(input, startingPos, identifierLength);
        if (identifierLength != 1) {
            throw new IllegalArgumentException("Identifiers are expected to be one-characters only, " +
                    "encountered identifier starting with: " + identifier);
        }
        return identifier;
    }

    private int readReservedKeyword(char[] input, int startingPos, List<Token> tokens) {
        if (startingPos >= input.length) return -1;
        for (String recognizedKeyword : RECOGNIZED_KEYWORDS) {
            int keywordLength = recognizedKeyword.length();

            if (startingPos + keywordLength <= input.length) {
                String keyword = new String(input, startingPos, keywordLength);
                if (recognizedKeyword.equals(keyword)) {
                    tokens.add(new KeywordToken(recognizedKeyword));
                    return startingPos + keywordLength;
                }
            }
        }
        return -1;
    }
}
