package wumpusworld;

/**
 * Created by BTH on 2016-10-06.
 */
public class Tile
{
    public Tile(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tile tile = (Tile) o;

        if (x != tile.x) return false;
        return y == tile.y;

    }

    public int x;
    public int y;
}
