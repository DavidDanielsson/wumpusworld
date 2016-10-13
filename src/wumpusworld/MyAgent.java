package wumpusworld;

/**
 * Contains starting code for creating your own Wumpus World agent.
 * Currently the agent only make a random decision each turn.
 * 
 * @author Johan Hagelbäck
 */

import java.util.ArrayList;

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
            // generate "hash" for board surrounding current tile
            // Flip wumpus bits if he is found
            // Lookup board "hash" and gather utility number
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
    
    
}

