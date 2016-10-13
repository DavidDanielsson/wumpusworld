package wumpusworld;

/**
 * Contains starting code for creating your own Wumpus World agent.
 * Currently the agent only make a random decision each turn.
 * 
 * @author Johan Hagelbäck
 */

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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
    private ArrayList<Tile> openList = new ArrayList<>();
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
    }
   
            
    /**
     * Asks your solver agent to execute an action.
     */

    public void doAction()
    {
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
            { -1, 0 }
            , { 1, 0 }
            , { 0, 1 }
            , { 0, -1 }
        };


        // Open list expands around the player each time he makes a move.
        // Nodes are only removed when player moves to them.
        for(int i = 0; i < 4; ++i)
        {
            int nextX = cX + offset[i][0];
            int nextY = cY + offset[i][1];
            if(w.isValidPosition(nextX, nextY) && w.isUnknown(nextX, nextY))
            {
                Tile currentTile = new Tile(nextX, nextY);
                if(!openList.contains(currentTile))
                {
                    openList.add(currentTile);
                }
            }
        }


        // Open list is updated. Iterate through it
        int maxUtility = Integer.MIN_VALUE;
        Tile bestTile;
        for (Tile tile:openList)
        {
            int utility = GetUtility(tile);
            if(utility > maxUtility)
            {
                maxUtility = utility;
                bestTile = tile;
            }
            maxUtility = Integer.max(maxUtility, GetUtility(tile));
        }

        // We now have the best tile.
        // Move the player

            // Flip wumpus bits if he is found
        // List iterated. Make move with best utility
        // Once move is done, update utility







        // Once move is done, check if we know where wumpus is

        
        ////Test the environment
        //if (w.hasBreeze(cX, cY))
        //{
        //    System.out.println("I am in a Breeze");
        //}
        //if (w.hasStench(cX, cY))
        //{
        //    System.out.println("I am in a Stench");
        //}
        //if (w.hasPit(cX, cY))
        //{
        //    System.out.println("I am in a Pit");
        //}
        //if (w.getDirection() == World.DIR_RIGHT)
        //{
        //    System.out.println("I am facing Right");
        //}
        //if (w.getDirection() == World.DIR_LEFT)
        //{
        //    System.out.println("I am facing Left");
        //}
        //if (w.getDirection() == World.DIR_UP)
        //{
        //    System.out.println("I am facing Up");
        //}
        //if (w.getDirection() == World.DIR_DOWN)
        //{
        //    System.out.println("I am facing Down");
        //}
        //
        ////decide next move
        //rnd = decideRandomMove();
        //if (rnd==0)
        //{
        //    w.doAction(World.A_TURN_LEFT);
        //    w.doAction(World.A_MOVE);
        //}
        //
        //if (rnd==1)
        //{
        //    w.doAction(World.A_MOVE);
        //}
        //
        //if (rnd==2)
        //{
        //    w.doAction(World.A_TURN_LEFT);
        //    w.doAction(World.A_TURN_LEFT);
        //    w.doAction(World.A_MOVE);
        //}
        //
        //if (rnd==3)
        //{
        //    w.doAction(World.A_TURN_RIGHT);
        //    w.doAction(World.A_MOVE);
        //}

    }    
    
     /**
     * Genertes a random instruction for the Agent.
     */
    public int decideRandomMove()
    {
      return (int)(Math.random() * 4);
    }

    private static final int numStuffWeTrack = 2;
    private static final int breeze = 1;
    private static final int stench = 2;
    private static final int unknown = 4;



    private int GetUtility(Tile tile)
    {
        // Build bit mask for surrounding board
        int bitMask = 1 << 18;
        for (int y = -1; y < 2; y++)
            for(int x = -1; x < 2;x++)
            {
                int worldTileX = x + tile.x;
                int worldTileY = y + tile.y;

                if(w.hasBreeze(worldTileX, worldTileY))
                {
                    // Hax
                    bitMask |= breeze << (numStuffWeTrack * ((x + 1) + (y + 1) * 3));
                }
                if(w.hasStench(worldTileX,worldTileY))
                {
                    // Hax
                    bitMask |= stench << (numStuffWeTrack * ((x + 1) + (y + 1) * 3));
                }
                // Might be worth tracking unknown and out of bounds
                //if(w.isUnknown(worldTileX,worldTileY))
                //{
                //    // Hax
                //    bitMask |= unknown << (3 * ((x + 1) + (y + 1) * 3));
                //}
                // Maybe check if we're out of map
            }
        // Debug string for us to visualize
        String bitMaskString = Integer.toBinaryString(bitMask);
        // We now have our bit mask. Look up utility value in file
        int utilityValue = GetUtilityFromFile(bitMask);

        return utilityValue;
    }

    private int GetUtilityFromFile(int bitMask)
    {
        final String databaseFileName = "database.txt";
        //File file = new File("database.txt"); // pleb version
        // Read all lines and save into array list
        ArrayList<String> lines = new ArrayList<>();
        String line;
        // open op file and start reading
        try (
                InputStream fis = new FileInputStream(databaseFileName);
                InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr);
        )
        {
            // Read all lines from the file
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            // Catch because we need it
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // now we have an array list of a bunch of lines
        for (String readLine:lines)
        {
            // Split line into bitmask and value
            String[] splitLine = readLine.split("-");
            int parsedInt = Integer.parseInt(splitLine[0]);
            // See if this is the bitmask we're looking for
            if(parsedInt == bitMask)
            {
                int parsedVal = Integer.parseInt(splitLine[1]);
                return parsedVal;
            }
        }
        // This board was previously unencountered. Add it to database
        String writeString = Integer.toString(bitMask) + "-0";
        try {
            Files.write(Paths.get(databaseFileName), writeString.getBytes(), StandardOpenOption.APPEND);
        }catch (IOException e) {
            //exception handling left as an exercise for the reader
        }

        return 0;
    }

    private void Move(int x, int y)
    {



        int cX = w.getPlayerX();
        int cY = w.getPlayerY();






    }

    private ArrayList<Tile> FindPath(int startX, int startY, int goalX, int goalY)
    {
        // Used to iterate over all neighbours
        int offset[][] = new int[][]
                {
                        { -1, 0 }
                        , { 1, 0 }
                        , { 0, 1 }
                        , { 0, -1 }
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
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Node node = (Node) o;

                if (x != node.x) return false;
                return y == node.y;

            }
        };

        ArrayList<Node> openList = new ArrayList<>();
        ArrayList<Node> closedList = new ArrayList<>();

        openList.add(new Node(startX,startY,0, 0));


        // Iterate until we found our goal
        while(!openList.isEmpty())
        {
            // Hax
            Collections.sort(openList, (o1, o2) -> o1.gValue+o1.hValue < o2.gValue+o2.hValue ? 1 : -1);

            Node currentNode = openList.get(0);
            openList.remove(0);
            if(currentNode.x == goalX && currentNode.y == goalY)
            {
                // Build up return list (IMPLEMENT)
                break;
            }

            for (int i = 0; i < 4; ++i) {

                int nextX = currentNode.x + offset[i][0];
                int nextY = currentNode.y + offset[i][1];
                // Make sure its an interesting node to add to open list
                if (w.isValidPosition(nextX, nextY) && // Needs to be valid
                        !w.isUnknown(nextX, nextY) && // Needs to be known

                        closedList.contains(new Node(nextX, nextY, 0, 0))) // Needs to not already be in closed list
                {
                    // Calculate gvalue
                    int gValue = 1;
                    if(w.hasPit(nextX, nextY))
                        gValue = 50; // Value could be tweaked, but pits are bad
                    int hValue = goalX - 
                    openList.add(new Node(nextX, nextY, currentNode.gValue+gValue));
                }
            }

            Tile bestTile;
            // Check each surrounding node

        }
    }



}

