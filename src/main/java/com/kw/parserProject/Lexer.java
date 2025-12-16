package com.kw.parserProject;

import com.kw.parserProject.tokens.*;

import java.util.LinkedList;
import java.util.List;

public class Lexer {
    private static final List<Character> RECOGNIZED_OPERATORS = List.of('+', '-', '*', '/', '<', '>');
    private static final List<String> RECOGNIZED_KEYWORDS = List.of("while", "else", "end", "if");

    public List<Token> extractTokens(String programCode) {
        List<Token> tokens = new LinkedList<>();
        char[] charArray = programCode.toCharArray();
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
            tokens.add(new VariableToken(Character.toString(character)));
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
        return -1;
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
