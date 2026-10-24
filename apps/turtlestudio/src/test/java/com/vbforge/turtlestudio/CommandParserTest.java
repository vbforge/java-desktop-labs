package com.vbforge.turtlestudio;

import com.vbforge.turtlestudio.engine.Command;
import com.vbforge.turtlestudio.engine.CommandParser;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {

    @Test
    void parsesValidCommands() {
        String script = "FORWARD 100\nRIGHT 90\nPEN UP";
        List<Command> commands = CommandParser.parse(script);
        
        assertEquals(3, commands.size());
        assertEquals(Command.Type.FORWARD, commands.get(0).type());
        assertEquals(100, commands.get(0).value());
        assertEquals(Command.Type.RIGHT, commands.get(1).type());
        assertEquals(Command.Type.PEN_UP, commands.get(2).type());
    }

    @Test
    void throwsOnInvalidCommand() {
        assertThrows(IllegalArgumentException.class, () -> CommandParser.parse("FLY 100"));
    }

    @Test
    void throwsOnMissingNumber() {
        assertThrows(IllegalArgumentException.class, () -> CommandParser.parse("FORWARD"));
    }
}