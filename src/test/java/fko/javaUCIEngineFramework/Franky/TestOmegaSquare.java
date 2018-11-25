/**
 * The MIT License (MIT)
 *
 * "Chessly by Frank Kopp"
 *
 * mail-to:frank@familie-kopp.de
 *
 * Copyright (c) 2016 Frank Kopp
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package fko.chessly.player.computer.Omega;


import fko.chessly.game.GamePosition;
import fko.chessly.player.computer.Omega.OmegaSquare.File;
import fko.chessly.player.computer.Omega.OmegaSquare.Rank;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Frank
 *
 */
public class TestOmegaSquare {

    /**
     * Tests basic OmegaSquare operations
     */
    @Test
    public void test() {
        // Square addressing
        assertTrue(OmegaSquare.getSquare(0).equals(OmegaSquare.a1));
        assertTrue(OmegaSquare.getSquare(119).equals(OmegaSquare.h8));
        assertTrue(OmegaSquare.getSquare(8).equals(OmegaSquare.NOSQUARE));
        assertTrue(OmegaSquare.getSquare(-1).equals(OmegaSquare.NOSQUARE));
        assertTrue(OmegaSquare.getSquare(128).equals(OmegaSquare.NOSQUARE));
        assertTrue(OmegaSquare.h8.isValidSquare());
        assertFalse(OmegaSquare.i8.isValidSquare());
        assertFalse(OmegaSquare.NOSQUARE.isValidSquare());

        // addressing with file and rank
        assertTrue(OmegaSquare.getSquare(1,1).equals(OmegaSquare.a1));
        assertTrue(OmegaSquare.getSquare(8,8).equals(OmegaSquare.h8));
        assertTrue(OmegaSquare.getSquare(1,9).equals(OmegaSquare.NOSQUARE));
        assertTrue(OmegaSquare.getSquare(0,8).equals(OmegaSquare.NOSQUARE));
        assertTrue(OmegaSquare.getSquare(9,9).equals(OmegaSquare.NOSQUARE));

        // getFile
        assertTrue(OmegaSquare.a1.getFile().equals(File.a));
        assertTrue(OmegaSquare.h8.getFile().equals(File.h));
        assertTrue(OmegaSquare.j1.getFile().equals(File.NOFILE));
        assertTrue(OmegaSquare.getSquare(0).getFile().equals(File.a));
        assertTrue(OmegaSquare.getSquare(8).getFile().equals(File.NOFILE));
        assertTrue(OmegaSquare.getSquare(128).getFile().equals(File.NOFILE));

        // getRank
        assertTrue(OmegaSquare.a1.getRank().equals(Rank.r1));
        assertTrue(OmegaSquare.h8.getRank().equals(Rank.r8));
        assertTrue(OmegaSquare.j1.getRank().equals(Rank.NORANK));
        assertTrue(OmegaSquare.getSquare(0).getRank().equals(Rank.r1));
        assertTrue(OmegaSquare.getSquare(8).getRank().equals(Rank.NORANK));
        assertTrue(OmegaSquare.getSquare(128).getRank().equals(Rank.NORANK));

        // base rows
        OmegaSquare square = OmegaSquare.a2;
        assertTrue(square.isWhitePawnBaseRow());
        assertFalse(square.isBlackPawnBaseRow());
        assertTrue(square.isPawnBaseRow(OmegaColor.WHITE));
        assertFalse(square.isPawnBaseRow(OmegaColor.BLACK));
        square = OmegaSquare.e7;
        assertFalse(square.isWhitePawnBaseRow());
        assertTrue(square.isBlackPawnBaseRow());
        assertFalse(square.isPawnBaseRow(OmegaColor.WHITE));
        assertTrue(square.isPawnBaseRow(OmegaColor.BLACK));

        // iteration
        int counter = 0;
        for ( OmegaSquare sq : OmegaSquare.values ) {
            if (!sq.isValidSquare()) continue;
            counter++;
        }
        assertTrue(counter==64);

        // access through getValueList()
        List<OmegaSquare> list = OmegaSquare.getValueList();
        assertTrue(list.size() == 64);
        assertTrue(list.get(0).equals(OmegaSquare.a1));
        assertTrue(list.get(63).equals(OmegaSquare.h8));

        // check order by creating string
        StringBuilder sb = new StringBuilder();
        list.forEach(c -> sb.append(c));
        assertTrue(sb.toString().equals("a1b1c1d1e1f1g1h1a2b2c2d2e2f2g2h2a3b3c3d3e3f3g3h3a4b4c4d4e4f4g4h4a5b5c5d5e5f5g5h5a6b6c6d6e6f6g6h6a7b7c7d7e7f7g7h7a8b8c8d8e8f8g8h8"));

        counter = 0;
        for ( OmegaSquare.File f : OmegaSquare.File.values() ) {
            if (f == File.NOFILE) continue;
            counter++;
        }
        assertTrue(counter==8);

        counter = 0;
        for ( OmegaSquare.Rank r : OmegaSquare.Rank.values() ) {
            if (r == Rank.NORANK) continue;
            counter++;
        }
        assertTrue(counter==8);

        // convert to GamePosition
        GamePosition gp = OmegaSquare.a1.convertToGamePosition();
        assertTrue(gp.getName().equals(OmegaSquare.a1.name()));
        gp = OmegaSquare.h8.convertToGamePosition();
        assertTrue(gp.getName().equals(OmegaSquare.h8.name()));
        gp = OmegaSquare.e5.convertToGamePosition();
        assertTrue(gp.getName().equals(OmegaSquare.e5.name()));
        gp = OmegaSquare.e5.convertToGamePosition();
        assertFalse(gp.getName().equals(OmegaSquare.e6.name()));

        //convert from GamePosition
        gp = OmegaSquare.a1.convertToGamePosition();
        OmegaSquare os = OmegaSquare.convertFromGamePosition(gp);
        assertTrue(os.name().equals(gp.getName()));
        gp = GamePosition.getGamePosition("e4");
        os = OmegaSquare.convertFromGamePosition(gp);
        assertTrue(os.name().equals("e4"));
    }

    /**
     *
     */
    @Test
    public void testDirections() {
        OmegaSquare e4 = OmegaSquare.e4;
        assertTrue(e4.getNorth() == OmegaSquare.e5);
        assertTrue(e4.getSouth() == OmegaSquare.e3);
        assertTrue(e4.getEast() == OmegaSquare.f4);
        assertTrue(e4.getWest() == OmegaSquare.d4);
    }
}
