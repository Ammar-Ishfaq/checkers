package com.cyberbot.checkers.game;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.cyberbot.checkers.preferences.Preferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class Grid implements Iterable<GridEntry> {
    private final int size;
    private final int playerRows;
    private final ArrayList<GridEntry> gridEntries;

    // Preferences
    private boolean canMoveBackwards = false;
    private boolean canCaptureBackwards = true;
    private boolean flyingKing = true;

    public int getSize() {
        return size;
    }

    public int getPlayerRows() {
        return playerRows;
    }

    public Grid() {
        this(10, 4);
    }

    public Grid(int size, int playerRows) {
        if(size - (playerRows * 2) < 2) {
            throw new IllegalArgumentException("There should be 2 no-player rows on the board at minimum");
        }

        this.size = size;
        this.playerRows = playerRows;
        gridEntries = new ArrayList<>();

        for(int i = 0; i < size*size; ++i) {
            int y = i / size;
            GridEntry entry = new GridEntry(i % size, y);

            if(y < playerRows && entry.legal()) {
                entry.setPlayer(PlayerNum.FIRST);
                entry.setPieceType(PieceType.ORDINARY);
            }
            else if(y >= size - playerRows && entry.legal()) {
                entry.setPlayer(PlayerNum.SECOND);
                entry.setPieceType(PieceType.ORDINARY);
            }

            gridEntries.add(entry);
        }
    }

    public Grid(int size, int playerRows, boolean canMoveBackwards, boolean canCaptureBackwards, boolean flyingKing) {
        this(size, playerRows);
        this.canMoveBackwards = canMoveBackwards;
        this.canCaptureBackwards = canCaptureBackwards;
        this.flyingKing = flyingKing;
    }

    public static Grid fromPreferences(@NotNull Preferences prefs) {
        return new Grid(
                prefs.getGridSize(),
                prefs.getPlayerRows(),
                prefs.getCanMoveBackwards(),
                prefs.getCanCaptureBackwards(),
                prefs.getFlyingKing()
        );
    }

    public GridEntry getEntryByCoords(int x, int y) throws IndexOutOfBoundsException {
        if(x >= size || y >= size) {
            throw new IndexOutOfBoundsException("Coordinates (" + x + ", " + y + ") out of bounds for grid with size " + size);
        }

        for(GridEntry e: gridEntries) {
            if(e.getX() == x && e.getY() == y) return e;
        }

        throw new RuntimeException("Entry (" + x + ", " + y + ") not found in Grid");
    }

    public boolean moveAllowed(GridEntry src, GridEntry dst) {
        return src == dst || calculateAllowedMoves(src, false).contains(dst);
    }

    public boolean attemptMove(GridEntry src, GridEntry dst) {
        if(dst == src || !moveAllowed(src, dst)) return false;

        final int srcIdx = gridEntries.indexOf(src);
        final int dstIdx = gridEntries.indexOf(dst);

        if(srcIdx == -1 || dstIdx == -1) {
            throw new RuntimeException("GridEntry destination or source not part of the Grid");
        }

        gridEntries.get(dstIdx).setPlayer(src.getPlayer());
        gridEntries.get(dstIdx).setPieceType(src.getPieceType());

        gridEntries.get(srcIdx).setPlayer(PlayerNum.NOPLAYER);
        gridEntries.get(srcIdx).setPieceType(PieceType.UNASSIGNED);

        // TODO: Clear entries' cache

        return true;
    }

    @NonNull
    @Override
    public Iterator<GridEntry> iterator() {
        return gridEntries.iterator();
    }

    private HashSet<GridEntry> getAdjacentEntries(@NotNull GridEntry entry) {
        HashSet<GridEntry> adjacentEntries = new HashSet<>();

        // Short name for code readability
        final int x = entry.getX();
        final int y = entry.getY();

        // Bottom corner
        if(x == 0 && y == size-1) {
            adjacentEntries.add(getEntryByCoords(1, size-2));
        }
        // Upper corner
        else if(y == 0 && x == size-1) {
            adjacentEntries.add(getEntryByCoords(size-2, 1));
        }
        // Upper edge
        else if(x == 0) {
            adjacentEntries.add(getEntryByCoords(1, y-1));
            adjacentEntries.add(getEntryByCoords(1, y+1));
        }
        // Left edge
        else if(y == 0) {
            adjacentEntries.add(getEntryByCoords(x-1, 1));
            adjacentEntries.add(getEntryByCoords(x+1, 1));
        }
        // Bottom edge
        else if(x == size-1) {
            adjacentEntries.add(getEntryByCoords(x-1, y-1));
            adjacentEntries.add(getEntryByCoords(x-1, y+1));
        }
        // Right edge
        else if(y == size-1) {
            adjacentEntries.add(getEntryByCoords(x-1, y-1));
            adjacentEntries.add(getEntryByCoords(x+1, y-1));
        }
        // Mid entries
        else {
            adjacentEntries.add(getEntryByCoords(x-1, y-1));
            adjacentEntries.add(getEntryByCoords(x-1, y+1));
            adjacentEntries.add(getEntryByCoords(x+1, y-1));
            adjacentEntries.add(getEntryByCoords(x+1, y+1));
        }

        return adjacentEntries;
    }

    public HashSet<GridEntry> calculateAllowedMoves(@NotNull GridEntry entry, boolean storeInCache) {
        HashSet<GridEntry> allowedMoves = new HashSet<>();
        for(GridEntry adjEntry: getAdjacentEntries(entry)) {
            if(adjEntry.getPlayer() == PlayerNum.NOPLAYER) {
                if(canMoveBackwards
                        || (entry.getPlayer() == PlayerNum.FIRST && entry.getY() < adjEntry.getY())
                        || (entry.getPlayer() == PlayerNum.SECOND && entry.getY() > adjEntry.getY())) {
                    allowedMoves.add(adjEntry);
                }
            }
        }

        if(storeInCache) entry.setAllowedMovesCache(allowedMoves);

        return allowedMoves;
    }
}