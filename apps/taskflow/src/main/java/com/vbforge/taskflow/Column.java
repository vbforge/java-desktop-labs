package com.vbforge.taskflow;

/**
 * A board column. The id is stable for the lifetime of the board; the
 * name is what the user sees.
 */
public record Column(long id, String name) {}