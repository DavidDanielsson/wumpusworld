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

/**
 * Execution
 * 1) Bygg open list av alla möjliga rutor som man kan gå till
 * 2) Iterera över open list och kolla mot map för hur närliggande 8 rutor ser ut
 * 2.1) Är wumpus funnen så kollar man mot map för när wumpus inte finns i stället.
 * 3) Välj ruta med högst utiliti värde
 *
 *
 * Learning
 * Låt nissen springa runt randomly. Han kommer ihåg hur bra ett individuellt drag
 * var och sprarar ner det värdet för mapen för hur draget såg ut.
 * */



public class MyAgent implements Agent
{
    private static final String databaseFileName = "database.botdb";
    
    private ArrayList<Tile> openList = new ArrayList<>();
    // Index is bitmask for board, value is utility value
    private HashMap<Integer, Integer> utilityValues = new HashMap<>();
    private World w;

    int rnd;

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

    public void SetWorld(World world)
    {
        w = world;
        openList = new ArrayList<>();
    }



    private void ReadUtilityValuesFromFile()
    {
        // Read all lines and save into array list
        ArrayList<String> lines = new ArrayList<>();
        String line;
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
            DataInputStream inputStream = new DataInputStream(new FileInputStream(databaseFileName));

            int utilityValuesSize = inputStream.readInt();

            for(int i = 0; i < utilityValuesSize; ++i)
            {
                utilityValues.put(inputStream.readInt(), inputStream.readInt());
            }
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

            outputStream.writeInt(utilityValues.size());

            for(Map.Entry<Integer, Integer> entry : utilityValues.entrySet())
                {
                    // Write bitmask and utility value
                    outputStream.writeInt(entry.getKey());
                    outputStream.writeInt(entry.getValue());
                }

            outputStream.close();

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
     * Asks your solver agent to execute an action.
     */

    public void doAction()
    {

        if(w.gameOver())
            return;

        //Location of the player
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
        // Expand open list
        int offset[][] = new int[][]
                {
                        {-1, 0}
                        , {1, 0}
                        , {0, 1}
                        , {0, -1}
                };

        // Open list expands around the player each time he makes a move.
        // Nodes are only removed when player moves to them.
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

        // Open list is updated. Iterate through it
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
        // Move the player
        Move(bestTile.x, bestTile.y);
        openList.remove(bestTile);

        // Check results
        int results = 500;
        // Fell into pit. This is bad
        if(w.isInPit())
        {
            results -= 1000;
        }
        // Player died. This is really bad
        if(!w.hasGold() && w.gameOver())
        {
            results -= 10000;
        }

        maxUtility += results;

        utilityValues.put(bestMask, maxUtility);

        // This doesn't actually have to be done after each move
        WriteUtilityValuesToFile();
    }

    /**
     * Genertes a random instruction for the Agent.
     */
    public int decideRandomMove()
    {
        return (int) (Math.random() * 4);
    }

    private static final int numStuffWeTrack = 3;
    private static final int breeze = 1;
    private static final int stench = 2;
    private static final int unknown = 4;


    // Somewhat pointless function. Happens when refactoring
    private int GetUtility(Tile tile)
    {
        int bitMask = MakeMask(tile);
        if(utilityValues.containsKey(bitMask))
            return utilityValues.get(bitMask);
        else
            utilityValues.put(bitMask, 0);
        return 0;
    }

    private int MakeMask(Tile tile)
    {
        // Build bit mask for surrounding board
        int bitMask = 1 << 18;
        for (int y = -1; y < 2; y++)
        {
            for (int x = -1; x < 2; x++)
            {
                int worldTileX = x + tile.x;
                int worldTileY = y + tile.y;

                if (w.hasBreeze(worldTileX, worldTileY))
                {
                    // Hax
                    bitMask |= breeze << (numStuffWeTrack * ((x + 1) + (y + 1) * 3));
                }
                if (w.hasStench(worldTileX, worldTileY))
                {
                    // Hax
                    bitMask |= stench << (numStuffWeTrack * ((x + 1) + (y + 1) * 3));
                }
                // Might be worth tracking unknown and out of bounds
                if(w.isUnknown(worldTileX,worldTileY))
                {
                    // Hax
                    bitMask |= unknown << (numStuffWeTrack * ((x + 1) + (y + 1) * 3));
                }
                // Maybe check if we're out of map
            }
        }
        return bitMask;
    }

    private void WriteUtilityToFile(int bitMask)
    {
        final String databaseFileName = "database.txt";
        // open op file and start reading
        try (
                InputStream fis = new FileInputStream(databaseFileName);
                InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr);
        )
        {
        }

        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }


    private int GetUtilityFromFile(int bitMask)
    {
        final String databaseFileName = "database.txt";
        //File file = new File("database.txt"); // pleb version
        // Read all lines and save into array list
        ArrayList<String> lines = new ArrayList<>();
        String line;
        File file = new File(databaseFileName);
        // Check if file exists. If it doesn't, create it
        if (!file.exists())
            try
            {
                file.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        // open op file and start reading
        try (
                InputStream fis = new FileInputStream(databaseFileName);
                InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr);
        )
        {
            // Read all lines from the file
            while ((line = br.readLine()) != null)
            {
                lines.add(line);
            }
            // Catch because we need it
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        // now we have an array list of a bunch of lines
        for (String readLine : lines)
        {
            // Split line into bitmask and value
            String[] splitLine = readLine.split("-");
            int parsedInt = Integer.parseInt(splitLine[0]);
            // See if this is the bitmask we're looking for
            if (parsedInt == bitMask)
            {
                int parsedVal = Integer.parseInt(splitLine[1]);
                return parsedVal;
            }
        }
        // This board was previously unencountered. Add it to database
        String writeString = Integer.toString(bitMask) + "-0\n";
        try
        {
            Files.write(Paths.get(databaseFileName), writeString.getBytes(), StandardOpenOption.APPEND);
        }
        catch (IOException e)
        {
            //exception handling left as an exercise for the reader
        }

        return 0;
    }

    private void Move(int x, int y)
    {
        int cX = w.getPlayerX();
        int cY = w.getPlayerY();

        ArrayList<Tile> path = FindPath(cX, cY, x, y);

        if(path == null)
        {
            System.out.println("No path found!");
            path = FindPath(cX, cY, x, y);
        }

        for (int i = path.size() - 1; i >= 0; i--)
        {
            RotatePlayer(path.get(i).x, path.get(i).y);
            // If player is next to the only unexplored spot left on the map
            // and wumpus is alive, shoot it before entering the square
            if (i == 0 && w.wumpusAlive() && openList.size() == 1 && w.hasArrow())
                w.doAction(World.A_SHOOT);
            w.doAction(World.A_MOVE);
        }
    }

    private void RotatePlayer(int x, int y)
    {
        int dx = x - w.getPlayerX();
        int dy = y - w.getPlayerY();

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
        else // Keep spinning left until we face the right way
            while (w.getDirection() != targetDir)
            {
                w.doAction(World.A_TURN_LEFT);
            }
    }


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
            // Hax
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

