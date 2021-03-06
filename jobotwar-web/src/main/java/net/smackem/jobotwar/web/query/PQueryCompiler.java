package net.smackem.jobotwar.web.query;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;

/**
 * Compiles the filter grammar PQuery.g4.
 */
public class PQueryCompiler {
    private PQueryCompiler() {}

    /**
     * Compiles the given {@code source} into an object generated by the specified emitter.
     *
     * @param source The source to parse.
     * @param emitter The {@link PQueryVisitor} that emits the target format.
     * @return The emitted object graph if compilation was successful.
     * @throws ParseException if the given source was not in the correct format.
     */
    public static <T> T compile(String source, PQueryVisitor<T> emitter) throws ParseException {
        final CharStream input = CharStreams.fromString(source);
        final PQueryLexer lexer = new PQueryLexer(input);
        final ErrorListener errorListener = new ErrorListener();
        lexer.addErrorListener(errorListener);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final PQueryParser parser = new PQueryParser(tokens);
        parser.addErrorListener(errorListener);
        final PQueryParser.QueryContext tree = parser.query();
        if (errorListener.errors.isEmpty() == false) {
            throw new ParseException(String.join("\n", errorListener.errors), 0);
        }
        return tree.accept(emitter);
    }

    private static class ErrorListener implements ANTLRErrorListener {
        private final Collection<String> errors = new ArrayList<>();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String s, RecognitionException e) {
            this.errors.add(String.format("line %d:%d: %s", line, pos, s));
        }

        @Override
        public void reportAmbiguity(Parser parser, DFA dfa, int i, int i1, boolean b, BitSet bitSet, ATNConfigSet atnConfigSet) {
        }

        @Override
        public void reportAttemptingFullContext(Parser parser, DFA dfa, int i, int i1, BitSet bitSet, ATNConfigSet atnConfigSet) {
        }

        @Override
        public void reportContextSensitivity(Parser parser, DFA dfa, int i, int i1, int i2, ATNConfigSet atnConfigSet) {
        }
    }
}
