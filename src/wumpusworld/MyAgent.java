package wumpusworld;

/**
 * Contains starting code for creating your own Wumpus World agent.
 * Currently the agent only make a random decision each turn.
 *
 * @author Johan Hagelbäck
 */

import sun.plugin2.message.BestJREAvailableMessage;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class MyAgent implements Agent
{
    private static final String databaseFileName = "database.botdb";

    //Contains unexplored squares that we can move to
    private ArrayList<Tile> openList = new ArrayList<>();
    // Index is bitmask for board, value is utility value.
    // The bitmask is a representation of a square and its 8 surrounding squares
    private HashMap<Integer, Integer> utilityValues = new HashMap<>();
    private World w;

    /**
     * Creates a new instance of your solver agent.
     *
     * @param world Current world state 
     */
    public MyAgent(World world)
    {
        w = world;
        ReadUtilityValuesFromFile();
    }

    private void ReadUtilityValuesFromFile()
    {
        // Read all lines and save into array list
        File file = new File(databaseFileName);
        // Check if file exists. If it doesn't, create it
        if (!file.exists())
        {
            try
            {
                file.createNewFile();
                return; // No need to read anything
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        try
        {
            //Read the whole database file and copy the contents to the arraylist
            DataInputStream inputStream = new DataInputStream(new FileInputStream(databaseFileName));

            int utilityValuesSize = inputStream.readInt();

            for(int i = 0; i < utilityValuesSize; ++i)
            {
                utilityValues.put(inputStream.readInt(), inputStream.readInt());
            }
            inputStream.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void WriteUtilityValuesToFile()
    {
        // Failsafe in case values haven't been read
        if(utilityValues.size() == 0)
            return;

        try
        {
            // Create temporary file in case shutdown happens while writing
            Path tempFilePath = Paths.get(databaseFileName + ".temp");
            if(Files.exists(tempFilePath))
                Files.delete(tempFilePath);

            Files.createFile(tempFilePath);
            DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(tempFilePath.toString()));

            // Write the number of utlityvalues, then write
            // every bitmask-utilityvalue pair to the file
            outputStream.writeInt(utilityValues.size());

            for(Map.Entry<Integer, Integer> entry : utilityValues.entrySet())
                {
                    // Write bitmask and utility value
                    outputStream.writeInt(entry.getKey());
                    outputStream.writeInt(entry.getValue());
                }

            outputStream.close();

            //Copy all contents from the temp file to the real database file
            Path filePath = Paths.get(databaseFileName);
            if(Files.exists(filePath))
                Files.delete(filePath);

            Files.move(tempFilePath, filePath);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Execution
     * 1) Build an open list of nodes that we can walk to
     * 2) Iterate over the open list and check in the map how the surrounding 8 squares
     * 3) Choose the square with the highest utility value
     * 4) If we fell into a pit or died (wumpus) we know this square is bad, decrease its utility value
     * 5) Write all utility values to a file, so the agent remembers which squares are good and bad.
     *
     * */
    public void doAction()
    {
        //Do nothing if the agent has completed the game or died
        if(w.gameOver())
            return;

        //Get location of the player
        int cX = w.getPlayerX();
        int cY = w.getPlayerY();


        /// Obvious stuff
        //Basic action:
        //Grab Gold if we can.
        if (w.hasGlitter(cX, cY))
        {
            w.doAction(World.A_GRAB);
            return;
        }

        //Basic action:
        //We are in a pit. Climb up.
        if (w.isInPit())
        {
            w.doAction(World.A_CLIMB);
            return;
        }


        // Fancy stuff
        // Expand open list. The open list contains unexplored squares that we can move to
        int offset[][] = new int[][]
                {
                        {-1, 0}
                        , {1, 0}
                        , {0, 1}
                        , {0, -1}
                };

        // Check all squares around the player, if it is
        // unknown and valid to walk to, and we haven't seen it before
        // add it to the open list
        for (int i = 0; i < 4; ++i)
        {
            int nextX = cX + offset[i][0];
            int nextY = cY + offset[i][1];
            if (w.isValidPosition(nextX, nextY) && w.isUnknown(nextX, nextY))
            {
                Tile currentTile = new Tile(nextX, nextY);
                if (!openList.contains(currentTile))
                {
                    openList.add(currentTile);
                }
            }
        }

        // Open list is updated. Iterate through it and check
        // which is best to go to, i.e. the one with the best utility
        int maxUtility = Integer.MIN_VALUE;
        Tile bestTile = new Tile(0,0);
        int bestMask = 0;
        for (Tile tile : openList)
        {
            int utility = GetUtility(tile);
            if (utility > maxUtility)
            {
                bestMask = MakeMask(tile);
                maxUtility = utility;
                bestTile = tile;
            }
        }

        // We now have the best tile.
        // Move the player to the best tile, this may take several steps, we will
        // do all of them immediately, so it will look like the agent is taking many steps at once
        Move(bestTile.x, bestTile.y);
        // Remove from open list, since it is explored now
        openList.remove(bestTile);

        // See how well this move was.
        // If nothing bad happens, it is a good move
        int utility = 500;
        // If we fell into a pit give bad score
        if(w.isInPit())
        {
            utility -= 1000;
        }
        // If player died, give really bad utility
        if(!w.hasGold() && w.gameOver())
        {
            utility -= 10000;
        }

        // Save the newly learned utility into the arraylist
        maxUtility += utility;
        utilityValues.put(bestMask, maxUtility);

        // Write all utility values of all the situations the agent has
        // been experienced to the database file,so it can remember next time the application runs
        WriteUtilityValuesToFile();
    }

    /**
     * Genertes a random instruction for the Agent.
     */
    public int decideRandomMove()
    {
        return (int) (Math.random() * 4);
    }

    // Get the utility of a specific tile
    private int GetUtility(Tile tile)
    {
        //Make a bitmask that represents the square and its surroundings
        int bitMask = MakeMask(tile);
        if(utilityValues.containsKey(bitMask))
            return utilityValues.get(bitMask);
        else
            // Note: If this situation is new, we will give it a utility of 0
            utilityValues.put(bitMask, 0);
        return 0;
    }

    private int MakeMask(Tile tile)
    {
        // When deciding how good a tile is, the agent will check it and the 8 surrounding tiles,
        // and consider if they has breeze, stench, and/or is unknown.
        // So the bit mask will track these things
        final int numStuffWeTrack = 3;
        final int breeze = 1;    //2^0
        final int stench = 2;    //2^1
        final int unknown = 4;   //2^2

        // Build bit mask for surrounding tiles
        int bitMask = 1 << 18;
        // For each tile in the 3x3 grid (i.e. the tile and 8 surrounding ones)
        for (int y = -1; y < 2; y++)
        {
            for (int x = -1; x < 2; x++)
            {
                int worldTileX = x + tile.x;
                int worldTileY = y + tile.y;

                // For each square, save in the bitmask whether it has breeze or not
                if (w.hasBreeze(worldTileX, worldTileY))
                {
                    bitMask |= breeze << (numStuffWeTrack * ((x + 1) + (y + 1) * 3));
                }
                // Also, save in the bitmask whether it has stench or not
                if (w.hasStench(worldTileX, worldTileY))
                {
                    bitMask |= stench << (numStuffWeTrack * ((x + 1) + (y + 1) * 3));
                }
                // Also, save in the bitmask whether it is unknown or not
                if(w.isUnknown(worldTileX,worldTileY))
                {
                    bitMask |= unknown << (numStuffWeTrack * ((x + 1) + (y + 1) * 3));
                }
            }
        }
        return bitMask;
    }

    // Move the player to tile at position (x,y)
    private void Move(int x, int y)
    {
        // Get player position
        int cX = w.getPlayerX();
        int cY = w.getPlayerY();

        // Find a path of tiles from the player to (x,y)
        ArrayList<Tile> path = FindPath(cX, cY, x, y);

        if(path == null)
        {
            // Bug checking, this should never happen if the code works
            System.out.println("No path found!");
            return;
        }

        // Go along the calculated path.
        for (int i = path.size() - 1; i >= 0; i--)
        {
            //Rotate so we are facing the current tile
            RotatePlayer(path.get(i).x, path.get(i).y);
            // If player is next to the only unexplored spot left on the map
            // and wumpus is alive, shoot it before entering the square
            if (i == 0 && w.wumpusAlive() && openList.size() == 1 && w.hasArrow())
                w.doAction(World.A_SHOOT);
            //Move forward
            w.doAction(World.A_MOVE);
        }
    }

    // Rotate the player so it faces tile (x,y)
    private void RotatePlayer(int x, int y)
    {
        int dx = x - w.getPlayerX();
        int dy = y - w.getPlayerY();

        // Calculate which way the player should be facing
        int targetDir;
        if (dx > 0)
            targetDir = World.DIR_RIGHT;
        else if (dx < 0)
            targetDir = World.DIR_LEFT;
        else if (dy > 0)
            targetDir = World.DIR_UP;
        else
            targetDir = World.DIR_DOWN;

        // Turn right if it gives us the desired direction
        if ((w.getDirection() + 1) % 4 == targetDir)
            w.doAction(World.A_TURN_RIGHT);
        else // Otherwise keep spinning left unless we face the right way
            while (w.getDirection() != targetDir)
            {
                w.doAction(World.A_TURN_LEFT);
            }
    }

    // Find a path of tiles from the player to (x,y) using A* pathfinding
    private ArrayList<Tile> FindPath(int startX, int startY, int goalX, int goalY)
    {
        // Used to iterate over all neighbours
        int offset[][] = new int[][]
                {
                        {-1, 0}
                        , {1, 0}
                        , {0, 1}
                        , {0, -1}
                };

        class Node
        {
            public int x;
            public int y;
            public int gValue;
            public int hValue;
            public Node parent;

            public Node(int x, int y, int gValue, int hValue)
            {
                this.x = x;
                this.y = y;
                this.gValue = gValue;
                this.hValue = hValue;
            }

            @Override
            public boolean equals(Object o)
            {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Node node = (Node) o;

                if (x != node.x) return false;
                return y == node.y;

            }
        }

        ArrayList<Node> openList = new ArrayList<>();
        ArrayList<Node> closedList = new ArrayList<>();

        openList.add(new Node(startX, startY, 0, 0));


        // Iterate until we found our goal
        while (!openList.isEmpty())
        {
            // Sort the open list by the sum of g- and h-values
            Collections.sort(openList, (o1, o2) -> o1.gValue + o1.hValue > o2.gValue + o2.hValue ? 1 : -1);

            Node currentNode = openList.get(0);
            openList.remove(0);
            closedList.add(currentNode);
            // Check if we've found our goal
            if (currentNode.x == goalX && currentNode.y == goalY)
            {
                // Construct list of tiles to traverse and return
                ArrayList<Tile> pathList = new ArrayList<>();
                while (currentNode.parent != null)
                {
                    pathList.add(new Tile(currentNode.x, currentNode.y));
                    currentNode = currentNode.parent;
                }
                return pathList;
            }

            for (int i = 0; i < 4; ++i)
            {
                int nextX = currentNode.x + offset[i][0];
                int nextY = currentNode.y + offset[i][1];
                // Check if next is the goal, and add to list right away
                if(nextX == goalX && nextY == goalY)
                {
                    openList.add(new Node(nextX, nextY, 0, 0));
                    openList.get(openList.size()-1).parent = currentNode;
                    break;
                }
                // Make sure its an interesting node to add to open list
                if (w.isValidPosition(nextX, nextY) && // Needs to be valid
                        !w.isUnknown(nextX, nextY) && // Needs to be known
                        !closedList.contains(new Node(nextX, nextY, 0, 0))) // Needs to not already be in closed list
                {
                    // Calculate gvalue
                    int gValue = 1;
                    if (w.hasPit(nextX, nextY))
                        gValue = 50; // Value could be tweaked, but pits are bad
                    int hValue = Math.abs(goalX - nextX) + Math.abs(goalY - nextY);

                    Node newNode = new Node(nextX, nextY, currentNode.gValue + gValue, hValue);
                    boolean found = false;
                    for (int j = 0; j < openList.size(); j++)
                    {
                        if (openList.get(j).equals(newNode))
                        {
                            // Check if current node is better parent
                            if (currentNode.gValue + currentNode.hValue < openList.get(j).parent.gValue + openList.get(j).parent.hValue)
                            {
                                openList.get(j).parent = currentNode;
                            }
                            found = true;
                        }
                    }
                    if (!found)
                    {
                        openList.add(newNode);
                        newNode.parent = currentNode;
                    }
                }
            }
        }
        // No path was found. Return null
        return null;
    }

}

