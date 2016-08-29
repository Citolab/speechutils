package ee.ioc.phon.android.speechutils.editor;

import android.content.Context;
import android.support.test.runner.AndroidJUnit4;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class InputConnectionCommandEditorTest {

    private static final List<Command> COMMANDS;

    static {
        List<Command> list = new ArrayList<>();
        list.add(new Command("DELETE ME", ""));
        list.add(new Command("old_word", "new_word"));
        list.add(new Command("r2_old", "r2_new"));
        list.add(new Command("s/(.*)/(.*)/", "", "replace", new String[]{"$1", "$2"}));
        list.add(new Command("connect (.*) and (.*)", "", "replace", new String[]{"$1 $2", "$1-$2"}));
        list.add(new Command("delete (.+)", "", "delete", new String[]{"$1"}));
        list.add(new Command("delete2 (.*)", "D2", "replace", new String[]{"$1", ""}));
        list.add(new Command("underscore (.*)", "", "replace", new String[]{"$1", "_$1_"}));
        list.add(new Command("select (.*)", "", "select", new String[]{"$1"}));
        list.add(new Command("selection_replace (.*)", "", "replaceSel", new String[]{"$1"}));
        list.add(new Command("selection_underscore", "", "replaceSel", new String[]{"_{}_"}));
        list.add(new Command("selection_quote", "", "replaceSel", new String[]{"\"{}\""}));
        list.add(new Command("selection_double", "", "replaceSel", new String[]{"{}{}"}));
        list.add(new Command("selection_inc", "", "incSel"));
        list.add(new Command("selection_uc", "", "ucSel"));
        list.add(new Command("step back", "", "goBackward")); // no args means arg1 = 1
        list.add(new Command("prev_sent", "", "selectReBefore", new String[]{"[.?!]()[^.?!]+[.?!][^.?!]+"}));
        list.add(new Command("code (\\d+)", "", "keyCode", new String[]{"$1"}));
        list.add(new Command("code letter (.)", "", "keyCodeStr", new String[]{"$1"}));
        list.add(new Command("undo (\\d+)", "", "undo", new String[]{"$1"}));
        list.add(new Command("combine (\\d+)", "", "combine", new String[]{"$1"}));
        list.add(new Command("apply (\\d+)", "", "apply", new String[]{"$1"}));
        COMMANDS = Collections.unmodifiableList(list);
    }

    private InputConnectionCommandEditor mEditor;

    @Before
    public void before() {
        Context context = getInstrumentation().getContext();
        EditText view = new EditText(context);
        //view.setText("elas metsas mutionu, keset kuuski noori-vanu");
        EditorInfo editorInfo = new EditorInfo();
        //editorInfo.initialSelStart = 12;
        //editorInfo.initialSelEnd = 19;
        InputConnection connection = view.onCreateInputConnection(editorInfo);
        //InputConnection connection = new BaseInputConnection(view, true);
        mEditor = new InputConnectionCommandEditor();
        mEditor.setInputConnection(connection);
        mEditor.setUtteranceRewriter(new UtteranceRewriter(COMMANDS));
    }

    @Test
    public void test01() {
        assertNotNull(mEditor.getInputConnection());
        assertTrue(Op.NO_OP.isNoOp());
    }

    @Test
    public void test02() {
        add("start12345 67890");
        assertThatTextIs("Start12345 67890");
        runOp(mEditor.deleteLeftWord());
        assertThatTextIs("Start12345");
        runOp(mEditor.delete("12345"));
        assertThatTextIs("Start");
    }

    @Test
    public void test03() {
        assertTrue(true);
    }

    @Test
    public void test04() {
        addPartial("...123");
        addPartial("...124");
        add("...1245");
        runOp(mEditor.goToCharacterPosition(4));
        assertThat(getTextBeforeCursor(10), is("...1"));
        add("-");
        assertThatTextIs("...1-245");
        add("undo 2", "-");
        assertThatTextIs("...1245-");
    }

    @Test
    public void test05() {
        add("a12345 67890_12345");
        runOp(mEditor.select("12345"));
        assertThat(getTextBeforeCursor(2), is("0_"));
        runOp(mEditor.deleteLeftWord());
        assertThatTextIs("A12345 67890_");
        runOp(mEditor.deleteLeftWord());
        assertThatTextIs("A12345");
    }

    @Test
    public void test06() {
        add("a12345 67890_12345");
        runOp(mEditor.replace("12345", "abcdef"));
        runOp(mEditor.replaceSel(" "));
        runOp(mEditor.replace("12345", "ABC"));
        assertThat(getTextBeforeCursor(2), is("BC"));
        runOp(mEditor.replaceSel("\n"));
        runOp(mEditor.replaceSel(" "));
        runOp(mEditor.goToCharacterPosition(9));
        assertThat(getTextBeforeCursor(2), is("67"));
    }

    @Test
    public void test07() {
        add("123456789");
        runOp(mEditor.goToCharacterPosition(2));
        assertThat(getTextBeforeCursor(2), is("12"));
        assertThatTextIs("123456789");
    }

    @Test
    public void test08() {
        add("old_word");
        assertThatTextIs("New_word");
    }

    @Test
    public void test09() {
        add("r2_old");
        assertThatTextIs("R2_new");
    }

    @Test
    public void test10() {
        add("test old_word test");
        assertThatTextIs("Test new_word test");
    }

    @Test
    public void test11() {
        add("test word1");
        add("s/word1/word2/");
        assertThatTextIs("Test word2");
    }

    @Test
    public void test12() {
        add("test word1 word2");
        add("connect word1 and word2");
        assertThatTextIs("Test word1-word2");
    }

    @Test
    public void test13() {
        add("test word1 word2");
        add("connect word1");
        add("and");
        assertThatUndoIs("[delete 4, delete 14, delete 16]");
        assertThat(mEditor.commitFinalResult("word2").toString(), is("+replace(word1 word2,word1-word2)"));
        assertThatUndoIs("[undo replace2, delete 16]");
        assertThatTextIs("Test word1-word2");
    }

    @Test
    public void test14() {
        add("test word1");
        runOp(mEditor.replaceSel(" "));
        add("word2");
        assertThatTextIs("Test word1 word2");
        add("connect word1 and word2");
        assertThatTextIs("Test word1-word2");
    }

    @Test
    public void test15() {
        add("test word1");
        runOp(mEditor.replaceSel(" "));
        add("word2");
        assertThat(getTextBeforeCursor(11), is("word1 word2"));
        runOp(mEditor.deleteAll());
        assertThat(getTextBeforeCursor(1), is(""));
    }

    /**
     * If command does not fully match then its replacement is ignored.
     */
    @Test
    public void test16() {
        add("I will delete something");
        assertThatTextIs("I will delete something");
    }

    @Test
    public void test17() {
        add("there are word1 and word2...");
        add("select word1 and word2");
        runOp(mEditor.goToEnd());
        assertThatTextIs("There are word1 and word2...");
    }

    @Test
    public void test18() {
        add("there are word1 and word2...", "select word1 and word2", "selection_replace REPL");
        assertThatTextIs("There are REPL...");
    }

    @Test
    public void test19() {
        add("there are word1 and word2...", "select word1 and word2", "selection_underscore");
        assertThatTextIs("There are _word1 and word2_...");
        undo();
        assertThatTextIs("There are word1 and word2...");
    }

    @Test
    public void test20() {
        add("a", "select a", "selection_double", "selection_double");
        runOp(mEditor.goToEnd());
        assertThat(getTextBeforeCursor(5), is("AA"));
    }

    @Test
    public void test21() {
        add("123456789", "select 3", "selection_inc");
        runOp(mEditor.goForward(3));
        add("select 5", "selection_inc");
        assertThatTextIs("124466789");
    }

    @Test
    public void test22() {
        add("this is some word");
        add("select is some");
        add("selection_uc");
        assertThatTextIs("This IS SOME word");
    }

    @Test
    public void test23() {
        add("this is some word");
        runOp(mEditor.selectAll());
        add("selection_replace REPL");
        assertThat(mEditor.getText().toString(), is("REPL"));
    }

    @Test
    public void test24() {
        add("test word1 word2");
        add("connect word1 and not_exist");
        assertThatTextIs("Test word1 word2");
    }

    @Test
    public void test25() {
        add("test word1 word2");
        undo();
        assertThatTextIs("");
    }

    @Test
    public void test26() {
        add("1234567890");
        add("step back");
        runOp(mEditor.goBackward(1));
        undo();
        runOp(mEditor.deleteLeftWord());
        assertThatTextIs("0");
        undo();
        assertThatTextIs("1234567890");
    }

    /**
     * TODO: goLeft does not work
     */
    //@Test
    public void test27() {
        add("1234567890");
        runOp(mEditor.goLeft());
        runOp(mEditor.goLeft());
        undo();
        runOp(mEditor.deleteLeftWord());
        assertThatTextIs("0");
    }

    @Test
    public void test28() {
        add("1234567890");
        runOp(mEditor.goBackward(5));
        runOp(mEditor.goForward(2));
        undo(2);
        runOp(mEditor.goBackward(1));
        runOp(mEditor.deleteLeftWord());
        undo();
        assertThatTextIs("1234567890");
    }

    /**
     * old_word is rewritten into new_word and then changed using a command to NEWER_WORD
     */
    @Test
    public void test29() {
        add("test old_word");
        assertThatTextIs("Test new_word");
        add("s/new_word/NEWER_WORD/");
        assertThatUndoIs("[undo replace2, delete 13]");
        assertThatTextIs("Test NEWER_WORD");
        undo();
        assertThatUndoIs("[delete 13]");
        assertThatTextIs("Test new_word");
        undo();
        assertThatTextIs("");
    }

    @Test
    public void test30() {
        runOp(mEditor.replaceSel(" "));
        assertThatTextIs(" ");
        undo();
        assertThatTextIs("");
    }

    @Test
    public void test31() {
        add("there are word1 and word2...", "select word1 and word2", "selection_replace REPL");
        assertThatTextIs("There are REPL...");
        assertThatUndoIs("[deleteSurroundingText+commitText, setSelection, delete 28]");
        undo();
        assertThatTextIs("There are word1 and word2...");
    }

    /**
     * Failed command must not change the editor content.
     */
    @Test
    public void test32() {
        add("there are word1 and word2...");
        add("select nonexisting_word");
        undo();
        assertThatTextIs("");
    }

    /**
     * Failed command must not change the editor content.
     */
    @Test
    public void test33() {
        add("this is a text");
        add("this is another text");
        add("select nonexisting_word");
        undo();
        assertThatTextIs("This is a text");
    }

    /**
     * Apply a command with a non-empty rewrite and undo it.
     * First the command is undone, then the rewrite.
     */
    @Test
    public void test34() {
        add("this_is_a_text");
        add("delete2 is_a");
        assertThatTextIs("This__text D2");
        assertThatUndoIs("[undo replace1, delete 3, delete 14]");
        undo();
        assertThatTextIs("This_is_a_text D2");
        undo();
        assertThatTextIs("This_is_a_text");
    }

    /**
     * Undo a multi-segment command that succeeds.
     */
    @Test
    public void test35() {
        add("test word1 word2");
        add("connect word1");
        add("and");
        assertThat(mEditor.commitFinalResult("word2").toString(), is("+replace(word1 word2,word1-word2)"));
        assertThatUndoIs("[undo replace2, delete 16]");
        assertThatTextIs("Test word1-word2");
        undo();
        assertThatTextIs("Test word1 word2");
    }

    /**
     * Undo a multi-segment command that fails.
     */
    @Test
    public void test36() {
        add("test word1 word2");
        add("connect word1");
        add("and");
        assertThat(mEditor.commitFinalResult("nonexisting_word").toString(), is("-replace(word1 nonexisting_word,word1-nonexisting_word)"));
        assertThatUndoIs("[delete 16]");
        assertThatTextIs("Test word1 word2");
        undo();
        assertThatTextIs("");
    }

    /**
     * Dictating over a selection
     */
    @Test
    public void test37() {
        add("this is a text");
        add("select is a");
        add("is not a");
        assertThatTextIs("This is not a text");
    }

    @Test
    public void test38() {
        add("this is a text");
        add("select is a");
        add("selection_replace is not a");
        assertThatTextIs("This is not a text");
    }

    @Test
    public void test39() {
        add("this is a text");
        add("select is a");
        add("selection_replace is not a");
        assertThatUndoIs("[deleteSurroundingText+commitText, setSelection, delete 14]");
        assertThatTextIs("This is not a text");
        undo();
        assertThatTextIs("This is a text");
    }

    @Test
    public void test40() {
        add("this is a text");
        add("select is a");
        add("selection_replace");
        assertThatUndoIs("[delete 17, setSelection, delete 14]");
        assertThatTextIs("This selection_replace text");
        add("is not a");
        assertThatUndoIs("[deleteSurroundingText+commitText, setSelection, delete 14]");
        assertThatTextIs("This is not a text");
        undo();
        assertThatUndoIs("[setSelection, delete 14]");
        assertThatTextIs("This is a text");
        undo();
        assertThatTextIs("This is a text");
        undo();
        assertThatTextIs("");
    }

    /**
     * deleteLeftWord deletes the selection
     */
    @Test
    public void test41() {
        add("1234567890");
        add("select 456");
        runOp(mEditor.deleteLeftWord());
        assertThatTextIs("1237890");
        undo();
        assertThatTextIs("1234567890");
    }


    @Test
    public void test42() {
        add("test word1 word2");
        assertTrue(mEditor.commitPartialResult("connect word1 and word2"));
        add("connect word1 and word2");
        assertThatTextIs("Test word1-word2");
        undo();
        assertThatTextIs("Test word1 word2");
    }

    /**
     * An existing selection should not matter if the command is not about selection
     */
    @Test
    public void test43() {
        add("test word1 word2 word3");
        add("select word3");
        assertThatTextIs("Test word1 word2 word3");
        // Returns false if there is a selection
        assertFalse(mEditor.commitPartialResult("connect word1 and word2"));
        add("connect word1 and word2");
        assertThatTextIs("Test word1-word2 word3");
        undo();
        assertThatTextIs("Test word1 word2 word3");
    }

    /**
     * Partial results should not have an effect on the command.
     */
    @Test
    public void test44() {
        add("test word1", ".");
        addPartial("s/word1");
        addPartial("s/word1/word2/");
        add("s/word1/word2/");
        assertThatTextIs("Test word2.");
    }

    @Test
    public void test45() {
        add("sentence", ".");
        add("sentence");
        assertThatTextIs("Sentence. Sentence");
    }

    @Test
    public void test46() {
        add("Sentence", ".");
        addPartial("DELETE");
        assertThatTextIs("Sentence. DELETE");
        add("DELETE ME");
        assertThatTextIs("Sentence.");
    }

    /**
     * Auto-capitalization
     */
    @Test
    public void test47() {
        addPartial("this is 1st test.");
        add("this is 1st test. this is 2nd test.");
        addPartial("this is 3rd");
        add("this is 3rd test.");
        assertThatTextIs("This is 1st test. This is 2nd test. This is 3rd test.");
        add("delete this");
        assertThatTextIs("This is 1st test. This is 2nd test.  is 3rd test.");
        undo();
        // TODO: capitalization is not restored
        assertThatTextIs("This is 1st test. This is 2nd test. This is 3rd test.");
    }

    /**
     * Undoing final texts
     */
    @Test
    public void test48() {
        addPartial("this is 1st test.");
        add("this is 1st test. This is 2nd test.");
        addPartial("this is 3rd");
        add("this is 3rd test.");
        assertThatTextIs("This is 1st test. This is 2nd test. This is 3rd test.");
        undo();
        assertThatTextIs("This is 1st test. This is 2nd test.");
        undo();
        assertThatTextIs("");
    }

    /**
     * Regex based selection.
     */
    @Test
    public void test49() {
        add("This is number 1. This is number 2.");
        runOp(mEditor.selectReBefore("number "));
        add("#");
        assertThatTextIs("This is number 1. This is #2.");
    }

    /**
     * Regex based selection using capturing groups.
     */
    @Test
    public void test50() {
        add("This is number 1. This is number 2.");
        runOp(mEditor.selectReBefore("(\\d+)\\."));
        add("II");
        assertThatTextIs("This is number 1. This is number II.");
    }

    /**
     * Regex based selection using an empty capturing group.
     */
    @Test
    public void test51() {
        add("This is number 1. This is number 2? This is", "prev_sent");
        add("yes,");
        assertThatTextIs("This is number 1. Yes, This is number 2? This is");
        add("undo 2");
        add("3");
        assertThatTextIs("This is number 1. This is number 2? This is 3");
    }

    /**
     * Numeric keycode.
     * TODO: Works in the app but not in the test.
     */
    //@Test
    public void test52() {
        add("This is a test", "code 66");
        runOp(mEditor.keyCode(66));
        runOp(mEditor.keyCodeStr("A"));
        assertThatTextIs("This is a testA");
    }

    /**
     * Symbolic keycode
     * TODO: Works in the app but not in the test.
     */
    //@Test
    public void test53() {
        add("This is a test", "code letter B");
        assertThatTextIs("This is a testB");
    }

    /**
     * Apply a command multiple times.
     */
    @Test
    public void test54() {
        add("6543210", "step back", "apply 4", "-");
        assertThatTextIs("65-43210");
        assertThatUndoIs("[delete 1, undo apply 4, setSelection, delete 7]");
        undo();
        assertThatTextIs("6543210");
        undo();
        assertThatUndoIs("[setSelection, delete 7]");
        add("-");
        assertThatTextIs("654321-0");
    }

    /**
     * Delete a string multiple times.
     */
    @Test
    public void test55() {
        add("6 5 4 3 2 1 0", "delete  ", "apply 4", "-");
        assertThatTextIs("6 5-43210");
        add("undo 1");
        assertThatTextIs("6 543210");
        add("undo 1");
        assertThatTextIs("6 5 4 3 2 10");
    }

    /**
     * Combine last 3 commands and apply the result 2 times.
     */
    @Test
    public void test56() {
        add("0 a a a a b a", "select a", "selection_uc", "step back");
        assertThatTextIs("0 a a a a b A");
        add("-");
        assertThatTextIs("0 a a a a b -A");
        undo();
        assertThatTextIs("0 a a a a b A");
        assertThatOpStackIs("[move, ucSel, select a]");
        add("combine 3");
        assertThatTextIs("0 a a a a b A");
        assertThatOpStackIs("[[select a, ucSel, move]]");
        add("apply 2");
        assertThatTextIs("0 a a A A b A");
    }

    /**
     * Search for a string multiple times (i.e. apply "select" multiple times).
     * Change the 5th space with a hyphen.
     */
    @Test
    public void test57() {
        add("6 5 4 3 2 1 0", "select  ", "apply 4", "-");
        assertThatTextIs("6 5-4 3 2 1 0");
        undo();
        assertThatTextIs("6 5 4 3 2 1 0");
        undo();
        assertThatTextIs("6 5 4 3 2 1 0");
        add("-");
        assertThatTextIs("6 5 4 3 2 1-0");
    }

    @Test
    public void test60() {
        add("there are word1 and word2...");
        add("select word1 and word2");
        add("selection_uc");
        assertThatTextIs("There are WORD1 AND WORD2...");
        runOp(mEditor.goToEnd());
        add("select word1 and word2");
        add("selection_quote");
        assertThatTextIs("There are \"WORD1 AND WORD2\"...");
    }

    /**
     * TODO: incorrectly replaces with "_some_" instead of "_SOME_"
     */
    //@Test
    public void test61() {
        add("this is SOME word");
        add("underscore some");
        assertThatTextIs("This is _SOME_ word");
    }

    /**
     * Same as before but using selection.
     */
    @Test
    public void test62() {
        add("this is SOME word");
        add("select some");
        add("selection_underscore");
        assertThatTextIs("This is _SOME_ word");
    }

    /**
     * TODO: Can't create handler inside thread that has not called Looper.prepare()
     */
    //@Test
    public void test63() {
        add("test word1");
        runOp(mEditor.replaceSel(" "));
        add("word2");
        assertThatTextIs("Test word1 word2");
        runOp(mEditor.cutAll());
        assertThat(getTextBeforeCursor(1), is(""));
        runOp(mEditor.paste());
        assertThatTextIs("Test word1 word2");
    }

    /**
     * Undoing a move restores the selection.
     */
    @Test
    public void test64() {
        add("123 456 789");
        add("select 456");
        add("step back", "step back");
        undo(2);
        add("selection_underscore");
        assertThatTextIs("123 _456_ 789");
    }

    /**
     * Repeat the last utterance twice.
     * TODO: not implemented
     */
    @Test
    public void test65() {
        add("123");
        add("apply 2");
        assertThatTextIs("123123123");
    }

    /**
     * Combine last 3 commands and apply the result 2 times.
     */
    @Test
    public void test66() {
        add("0 a _ a _ a _", "s/a/b/", "s/_/*/");
        assertThatTextIs("0 a _ a * b _");
        add("combine 2");
        assertThatOpStackIs("[[replace, replace]]");
        add("apply 2");
        assertThatTextIs("0 b * b * b _");
    }

    // TODO: @Test
    // Can't create handler inside thread that has not called Looper.prepare()
    public void test80() {
        runOp(mEditor.copy());
        runOp(mEditor.paste());
        runOp(mEditor.paste());
    }

    private String getTextBeforeCursor(int n) {
        return mEditor.getInputConnection().getTextBeforeCursor(n, 0).toString();
    }

    private void addPartial(String... texts) {
        for (String text : texts) {
            assertTrue(mEditor.commitPartialResult(text));
        }
    }

    private void add(String... texts) {
        for (String text : texts) {
            assertNotNull(mEditor.commitFinalResult(text));
        }
    }

    private void undo() {
        undo(1);
    }

    private void undo(int steps) {
        runOp(mEditor.undo(steps));
    }

    private void assertThatUndoIs(String str) {
        assertThat(mEditor.getUndoStack().toString(), is(str));
    }

    private void assertThatOpStackIs(String str) {
        assertThat(mEditor.getOpStack().toString(), is(str));
    }

    private void assertThatTextIs(String str) {
        assertThat(mEditor.getText().toString(), is(str));
    }

    private void runOp(Op op) {
        assertNotNull(op);
        Op undo = op.run();
        assertNotNull(undo);
        // TODO: do we need to add an op that cannot be undone?
        if (!undo.isNoOp()) {
            mEditor.pushOp(op);
            mEditor.pushOpUndo(undo);
        }
        // TODO: we could check for each op if undo works as expected
        // do + undo
        //assertNotNull(op.run().run());
        // do
        //assertNotNull(op.run());
    }
}