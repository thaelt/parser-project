package com.kw.parserProject;

import com.kw.parserProject.statements.Statement;
import com.kw.parserProject.tokens.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled("Naive test class, used for manual triggering and not in mvn test due to potential long duration")
public class PerformanceTest {

    Lexer lexer;
    Parser parser;
    UnusedStatementChecker unusedStatementChecker;

    @BeforeEach
    void setUp() {
        lexer = new Lexer();
        parser = new Parser();
        unusedStatementChecker = new UnusedStatementChecker();
    }

    @Test
    void handleManyChainedOperations() {
        StringBuilder builder = new StringBuilder();

        System.out.println("GENERATING DATA");
        for (int j = 0; j < 1000_000; j++) {
            char variable = (char) ('a' + Math.random() * ('z' - 'a'));
            builder.append(variable).append("=");
            for (int i = 0; i < 4; i++) {
                builder.append(i).append("*").append(variable).append("/");
            }
            builder.append("1 + 21");
        }
        String input = builder.toString();
        System.out.println("GENERATING DATA - DONE");
        // when
        Instant start = Instant.now();
        List<Token> tokens = lexer.extractTokens(input);
        Instant afterTokens = Instant.now();
        System.out.println("STEP 1 " + ChronoUnit.MILLIS.between(start, afterTokens) + "ms");
        Program parsedProgram = parser.parse(tokens);
        Instant afterParsing = Instant.now();
        System.out.println("STEP 2 " + ChronoUnit.MILLIS.between(afterTokens, afterParsing) + "ms");
        List<Statement> actualOutput = unusedStatementChecker.getUnusedStatements(parsedProgram);
        Instant afterAnalysis = Instant.now();
        System.out.println("STEP 3 " + ChronoUnit.MILLIS.between(afterParsing, afterAnalysis) + "ms");

        // then
        assertNotNull(actualOutput);
    }

    @Test
    void handleManyStatements() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10_000; i++) {
            builder.append("x=").append(i).append('\n');
        }
        String input = builder.toString();
        // when
        Instant start = Instant.now();
        List<Token> tokens = lexer.extractTokens(input);
        Instant afterTokens = Instant.now();
        Program parsedProgram = parser.parse(tokens);
        Instant afterParsing = Instant.now();
        List<Statement> actualOutput = unusedStatementChecker.getUnusedStatements(parsedProgram);
        Instant afterAnalysis = Instant.now();

        System.out.println("STEP 1 " + ChronoUnit.MILLIS.between(start, afterTokens) + "ms");
        System.out.println("STEP 2 " + ChronoUnit.MILLIS.between(afterTokens, afterParsing) + "ms");
        System.out.println("STEP 3 " + ChronoUnit.MILLIS.between(afterParsing, afterAnalysis) + "ms");

        // then
        assertEquals(10000, actualOutput.size());
    }

}
