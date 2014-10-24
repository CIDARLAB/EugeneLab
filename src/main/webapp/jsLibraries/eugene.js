CodeMirror.defineMode("eugene", function(config, parserConfig) {
    var indentUnit = config.indentUnit,
            statementIndentUnit = parserConfig.statementIndentUnit || indentUnit,
            dontAlignCalls = parserConfig.dontAlignCalls,
            keywords = parserConfig.keywords || {},
            builtin = parserConfig.builtin || {},
            blockKeywords = parserConfig.blockKeywords || {},
            atoms = parserConfig.atoms || {},
            hooks = parserConfig.hooks || {},
            multiLineStrings = parserConfig.multiLineStrings;
    var isOperatorChar = /[+\-*&%=<>!?|\/]/;

    var curPunc;

    function tokenBase(stream, state) {
        var ch = stream.next();
        if (hooks[ch]) {
            var result = hooks[ch](stream, state);
            if (result !== false)
                return result;
        }
        if (ch == '"' || ch == "'") {
            state.tokenize = tokenString(ch);
            return state.tokenize(stream, state);
        }
        if (/[\[\]{}\(\),;\:\.]/.test(ch)) {
            curPunc = ch;
            return null;
        }
        if (/\d/.test(ch)) {
            stream.eatWhile(/[\w\.]/);
            return "number";
        }
        if (ch == "/") {
            if (stream.eat("*")) {
                state.tokenize = tokenComment;
                return tokenComment(stream, state);
            }
            if (stream.eat("/")) {
                stream.skipToEnd();
                return "comment";
            }
        }
        if (isOperatorChar.test(ch)) {
            stream.eatWhile(isOperatorChar);
            return "operator";
        }
        stream.eatWhile(/[\w\$_]/);
        var cur = stream.current();
        if (keywords.propertyIsEnumerable(cur)) {
            if (blockKeywords.propertyIsEnumerable(cur))
                curPunc = "newstatement";
            return "keyword";
        }
        if (builtin.propertyIsEnumerable(cur)) {
            if (blockKeywords.propertyIsEnumerable(cur))
                curPunc = "newstatement";
            return "builtin";
        }
        if (atoms.propertyIsEnumerable(cur))
            return "atom";
        return "variable";
    }

    function tokenString(quote) {
        return function(stream, state) {
            var escaped = false, next, end = false;
            while ((next = stream.next()) != null) {
                if (next == quote && !escaped) {
                    end = true;
                    break;
                }
                escaped = !escaped && next == "\\";
            }
            if (end || !(escaped || multiLineStrings))
                state.tokenize = null;
            return "string";
        };
    }

    function tokenComment(stream, state) {
        var maybeEnd = false, ch;
        while (ch = stream.next()) {
            if (ch == "/" && maybeEnd) {
                state.tokenize = null;
                break;
            }
            maybeEnd = (ch == "*");
        }
        return "comment";
    }

    function Context(indented, column, type, align, prev) {
        this.indented = indented;
        this.column = column;
        this.type = type;
        this.align = align;
        this.prev = prev;
    }
    function pushContext(state, col, type) {
        var indent = state.indented;
        if (state.context && state.context.type == "statement")
            indent = state.context.indented;
        return state.context = new Context(indent, col, type, null, state.context);
    }
    function popContext(state) {
        var t = state.context.type;
        if (t == ")" || t == "]" || t == "}")
            state.indented = state.context.indented;
        return state.context = state.context.prev;
    }

    // Interface

    return {
        startState: function(basecolumn) {
            return {
                tokenize: null,
                context: new Context((basecolumn || 0) - indentUnit, 0, "top", false),
                indented: 0,
                startOfLine: true
            };
        },
        token: function(stream, state) {
            var ctx = state.context;
            if (stream.sol()) {
                if (ctx.align == null)
                    ctx.align = false;
                state.indented = stream.indentation();
                state.startOfLine = true;
            }
            if (stream.eatSpace())
                return null;
            curPunc = null;
            var style = (state.tokenize || tokenBase)(stream, state);
            if (style == "comment" || style == "meta")
                return style;
            if (ctx.align == null)
                ctx.align = true;

            if ((curPunc == ";" || curPunc == ":" || curPunc == ",") && ctx.type == "statement")
                popContext(state);
            else if (curPunc == "{")
                pushContext(state, stream.column(), "}");
            else if (curPunc == "[")
                pushContext(state, stream.column(), "]");
            else if (curPunc == "(")
                pushContext(state, stream.column(), ")");
            else if (curPunc == "}") {
                while (ctx.type == "statement")
                    ctx = popContext(state);
                if (ctx.type == "}")
                    ctx = popContext(state);
                while (ctx.type == "statement")
                    ctx = popContext(state);
            }
            else if (curPunc == ctx.type)
                popContext(state);
            else if (((ctx.type == "}" || ctx.type == "top") && curPunc != ';') || (ctx.type == "statement" && curPunc == "newstatement"))
                pushContext(state, stream.column(), "statement");
            state.startOfLine = false;
            return style;
        },
        indent: function(state, textAfter) {
            if (state.tokenize != tokenBase && state.tokenize != null)
                return CodeMirror.Pass;
            var ctx = state.context, firstChar = textAfter && textAfter.charAt(0);
            if (ctx.type == "statement" && firstChar == "}")
                ctx = ctx.prev;
            var closing = firstChar == ctx.type;
            if (ctx.type == "statement")
                return ctx.indented + (firstChar == "{" ? 0 : statementIndentUnit);
            else if (ctx.align && (!dontAlignCalls || ctx.type != ")"))
                return ctx.column + (closing ? 0 : 1);
            else if (ctx.type == ")" && !closing)
                return ctx.indented + statementIndentUnit;
            else
                return ctx.indented + (closing ? 0 : indentUnit);
        },
        electricChars: "{}",
        blockCommentStart: "/*",
        blockCommentEnd: "*/",
        lineComment: "//"
    };
});

(function() {
    function words(str) {
        var obj = {}, words = str.split(" ");
        for (var i = 0; i < words.length; ++i)
            obj[words[i]] = true;
        return obj;
    }
    function cppHook(stream, state) {
        if (!state.startOfLine)
            return false;
        for (; ; ) {
            if (stream.skipTo("\\")) {
                stream.next();
                if (stream.eol()) {
                    state.tokenize = cppHook;
                    break;
                }
            } else {
                stream.skipToEnd();
                state.tokenize = null;
                break;
            }
        }
        return "meta";
    }

    // C#-style strings where "" escapes a quote.
    function tokenAtString(stream, state) {
        var next;
        while ((next = stream.next()) != null) {
            if (next == '"' && !stream.eat('"')) {
                state.tokenize = null;
                break;
            }
        }
        return "string";
    }

    CodeMirror.defineMIME("eugene", {
        name: "eugene",
        keywords: words("boolean bool num txt PartType Property Rule Device Collection Array " +
                "return ON on NOT not AND and OR or XOR xor " +
                "MORETHAN morethan CONTAINS contains EXACTLY exactly EQUALS equals SAME_COUNT same_count "+
                "WITH with THEN then " +
                "AFTER after ALL_AFTER all_after SOME_AFTER some_after "+
                "BEFORE before ALL_BEFORE all_before SOME_BEFORE some_before "+
                "NEXTTO nextto ALL_NEXTTO all_nextto SOME_NEXTTO some_nextto "+
                "REPRESSES represses INDUCES induces DRIVES drives "+
                "FORWARD forward ALL_FORWARD all_forward SOME_FORWARD some_forward "+
                "REVERSE reverse ALL_REVERSE all_reverse SOME_REVERSE some_reverse "+
                "ALTERNATE_ORIENTATION alternate_orientation "),
        // built-in functions and imperative features of Eugene
        builtin: words("if IF elseif ELSEIF else ELSE for FOR while WHILE include INCLUDE import IMPORT "+
        		"SAVE save STORE store RANDOM random PIGEON pigeon SIZEOF sizeof SIZE size "+
        		"print PRINT println PRINTLN product PRODUCT permute PERMUTE " +
        		"import IMPORT export EXPORT"),
        blockKeywords: words("if elseif else for"),
        atoms: words("true false"),
        hooks: {
            "@": function(stream) {
                stream.eatWhile(/[\w\$_]/);
                return "meta";
            }
        }
    });

}());
