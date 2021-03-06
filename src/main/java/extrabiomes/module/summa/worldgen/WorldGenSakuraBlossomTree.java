package extrabiomes.module.summa.worldgen;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import extrabiomes.helpers.LogHelper;
import extrabiomes.lib.Element;
import extrabiomes.module.summa.TreeSoilRegistry;
import org.bogdang.modifications.random.XSTR;

public class WorldGenSakuraBlossomTree extends WorldGenNewTreeBase
{
    
    private enum TreeBlock
    {
        LEAVES(new ItemStack(Blocks.leaves, 1, 1)), TRUNK(new ItemStack(Blocks.log, 1, 1));
        
        private ItemStack      stack;
        private static boolean loadedCustomBlocks = false;
        
        private static void loadCustomBlocks()
        {
            if (Element.LEAVES_SAKURA_BLOSSOM.isPresent())
                LEAVES.stack = Element.LEAVES_SAKURA_BLOSSOM.get();
            if (Element.LOG_SAKURA_BLOSSOM.isPresent())
                TRUNK.stack = Element.LOG_SAKURA_BLOSSOM.get();
            
            loadedCustomBlocks = true;
        }
        
        TreeBlock(ItemStack stack)
        {
            this.stack = stack;
        }
        
        public ItemStack get()
        {
            if (!loadedCustomBlocks)
                loadCustomBlocks();
            return this.stack;
        }
        
    }
    
    public WorldGenSakuraBlossomTree(boolean par1)
    {
        super(par1);
    }
    
    // Store the last seed that was used to generate a tree
    private static long lastSeed = 1234;
    
    @Override
    public boolean generate(World world, Random rand, int x, int y, int z)
    {
        // Store the seed
        lastSeed = rand.nextLong();
        
        Random tempR = new XSTR(lastSeed);
        // Make sure the tree can generate
        if (!checkTree(world, tempR, x, y, z))
            return false;
        
        return generateTree(world, tempR, x, y, z);
    }
    
    public boolean generate(World world, long seed, int x, int y, int z)
    {
        // Store the seed
        lastSeed = seed;
        
        Random tempR = new XSTR(lastSeed);
        // Make sure the tree can generate
        if (!checkTree(world, tempR, x, y, z))
            return false;
        
        return generateTree(world, tempR, x, y, z);
    }
    
    //Variables to control the generation
    private static final int    BASE_HEIGHT           = 8;    // The base height for trees
    private static final int    BASE_HEIGHT_VARIANCE  = 4;    // The Max extra branches that a tree can have
    private static final double TRUNK_HEIGHT_PERCENT  = 0.30D; // What percent of the total height the main trunk extends
    private static final int    BRANCHES_BASE_NUMBER  = 2;    // The total number of branches on the tree
    private static final int    BRANCHES_EXTRA        = 4;    // The how many extra branches can occur on the tree
    private static final int    CANOPY_WIDTH          = 8;    // How many blocks will this tree cover
    private static final int    CANOPY_WIDTH_VARIANCE = 4;    // How many extra blocks may this tree cover
                                                               
    static int                  last                  = 0;
    
    private boolean checkTree(World world, Random rand, int x, int y, int z)
    {
        final int height = rand.nextInt(BASE_HEIGHT_VARIANCE) + BASE_HEIGHT;
        final double radius = (CANOPY_WIDTH + rand.nextInt(CANOPY_WIDTH_VARIANCE)) / 2.0D;
        final int chunkCheck = (int) Math.ceil(radius) + 5;
        
        // make sure that we have room to grow the tree
        if (y >= 256 - height - 4 || y < 1 || y + height + 4 > 256)
            return false;
        
        // Make sure that the tree can fit in the world
        //if (y < 1 || y + height + 4 > 256)
            //return false;
        
        // Make sure that a tree can grow on the soil
        if (!TreeSoilRegistry.isValidSoil(world.getBlock(x, y - 1, z)))
            return false;
        
        // Make sure that all the needed chunks are loaded
        if (!world.checkChunksExist(x - chunkCheck, y - chunkCheck, z - chunkCheck, x + chunkCheck, y + chunkCheck, z + chunkCheck))
            return false;
        
        // Draw the main trunk
        if (!check1x1Trunk(x, y, z, (int) (height * TRUNK_HEIGHT_PERCENT), TreeBlock.TRUNK.get(), world))
            return false;
        // Generate the branches
        if (!checkBranches(world, rand, x, y + (int) (height * TRUNK_HEIGHT_PERCENT), z, height - (int) (height * TRUNK_HEIGHT_PERCENT) - 2, radius))
            return false;
        
        return true;
    }
    
    private boolean generateTree(World world, Random rand, int x, int y, int z)
    {
        final int height = rand.nextInt(BASE_HEIGHT_VARIANCE) + BASE_HEIGHT;
        final double radius = (CANOPY_WIDTH + rand.nextInt(CANOPY_WIDTH_VARIANCE)) / 2.0D;
        final int chunkCheck = (int) Math.ceil(radius) + 1;
        
        // make sure that we have room to grow the tree
        if (y >= 256 - height - 4 || y < 1 || y + height + 4 > 256)
            return false;
        
        // Make sure that the tree can fit in the world
        //if (y < 1 || y + height + 4 > 256)
            //return false;
        
        // Make sure that a tree can grow on the soil
        if (!TreeSoilRegistry.isValidSoil(world.getBlock(x, y - 1, z)))
            return false;
        
        // Make sure that all the needed chunks are loaded
        if (!world.checkChunksExist(x - chunkCheck, y - chunkCheck, z - chunkCheck, x + chunkCheck, y + chunkCheck, z + chunkCheck))
            return false;
        
        // Draw the main trunk
        if (place1x1Trunk(x, y, z, (int) (height * TRUNK_HEIGHT_PERCENT), TreeBlock.TRUNK.get(), world))
        {
            // Generate the branches
            generateBranches(world, rand, x, y + (int) (height * TRUNK_HEIGHT_PERCENT), z, height - (int) (height * TRUNK_HEIGHT_PERCENT) - 2, radius);
            
            return true;
        }
        
        return false;
    }
    
    public boolean checkBranches(World world, Random rand, int x, int y, int z, int height, double radius)
    {
        final int branchCount = BRANCHES_BASE_NUMBER + rand.nextInt(BRANCHES_EXTRA);
        float curAngle = 0.0f;
        
        final float[] average = { 0, 0, 0 };
        int[] start = { x, y, z };
        //Queue<int[]> branches = new LinkedList<int[]>();
        List<int[]> branches = new ArrayList<int[]>();
        
        // Generate the branches
        for (int i = 0; i < branchCount; i++)
        {
            // Get the branch radius and height
            final float angle = (rand.nextInt(50) + 35) / 90.0f;
            final float thisHeight = (height + 1) * (float)Math.sin(angle) / 1.3f;
            final float thisRadius = (float)(radius * Math.cos(angle));
            
            // Get the branch rotation
            curAngle += (rand.nextInt(360 / branchCount) + (360 / branchCount)) / 90.0f;//  + (360.0D/branchCount) / 180.0D ;
            
            final int x1 = (int) ((thisRadius) * Math.cos(curAngle));
            final int z1 = (int) ((thisRadius) * Math.sin(curAngle));
            
            // Add the the average count
            average[0] += x1;
            average[1] += thisHeight;
            average[2] += z1;
            
            // Add to the branch list
            final int[] node = new int[] { x1 + x, (int) thisHeight + y, z1 + z };
            
            // Add the branch end for leaf generation
            branches.add(node);
            
            // Generate the branch
            if (!checkBlockLine(start, node, TreeBlock.TRUNK.get(), world))
                return false;
        }
        
        // Place the branch tips
        Iterator<int[]> itt = branches.iterator();
        while (itt.hasNext())
        {
            int[] cluster = itt.next();
            if (!checkLeafCluster(world, cluster[0], cluster[1], cluster[2], 2, 2))
                return false;
        }
        
        // Calculate the center position
        average[0] /= branchCount;
        average[1] = (branchCount / average[1]) + 2.3f;
        average[2] /= branchCount;
        
        // Generate the canopy
        if (!checkCanopy(world, (int)average[0] + x, y, (int)average[2] + z, radius, height))
            return false;
        
        return true;
        
    }
    
    public void generateBranches(World world, Random rand, int x, int y, int z, int height, double radius)
    {
        final int branchCount = BRANCHES_BASE_NUMBER + rand.nextInt(BRANCHES_EXTRA);
        float curAngle = 0.0f;
        
        final float[] average = { 0, 0, 0 };
        final int[] start = { x, y, z };
        //Queue<int[]> branches = new LinkedList<int[]>();
        List<int[]> branches = new ArrayList<int[]>();
        
        // Generate the branches
        for (int i = 0; i < branchCount; i++)
        {
            // Get the branch radius and height
            final float angle = (rand.nextInt(50) + 35) / 90.0f;
            final float thisHeight = (height + 1) * (float)Math.sin(angle) / 1.3f;
            final float thisRadius = (float)(radius * Math.cos(angle));
            
            // Get the branch rotation
            curAngle += (rand.nextInt(360 / branchCount) + (360 / branchCount)) / 90.0f;//  + (360.0D/branchCount) / 180.0D ;
            
            final int x1 = (int) ((thisRadius) * Math.cos(curAngle));
            final int z1 = (int) ((thisRadius) * Math.sin(curAngle));
            
            // Add the the average count
            average[0] += x1;
            average[1] += thisHeight;
            average[2] += z1;
            
            // Add to the branch list
            final int[] node = new int[] { x1 + x, (int) thisHeight + y, z1 + z };
            
            // Add the branch end for leaf generation
            branches.add(node);
            
            // Generate the branch
            placeThinBlockLine(start, node, TreeBlock.TRUNK.get(), world);
        }
        
        // Place the branch tips
        Iterator<int[]> itt = branches.iterator();
        while (itt.hasNext())
        {
            int[] cluster = itt.next();
            generateLeafCluster(world, cluster[0], cluster[1], cluster[2], 2, 2, TreeBlock.LEAVES.get());
        }
        
        // Calculate the center position
        average[0] /= branchCount;
        average[1] = (branchCount / average[1]) + 2.3f;
        average[2] /= branchCount;
        
        // Generate the canopy
        generateCanopy(world, rand, (int)average[0] + x, y, (int)average[2] + z, radius, height, TreeBlock.LEAVES.get());
        
        // Generate the center cone
        generateVerticalCone(world, x, y, z, height - 1, .75, 2, TreeBlock.LEAVES.get());
        
    }
    
    public boolean checkCanopy(World world, int x, int y, int z, double radius, int height)
    {
        final int layers = height + 2;
        for (int y1 = y, layer = 0; layer < layers; layer++, y1++)
        {
            if (!checkCanopyLayer(world, x, y1, z, radius * Math.cos((layer) / (height / 1.3))))
                return false;
        }
        
        return true;
    }
    
    public void generateCanopy(World world, Random rand, int x, int y, int z, double radius, int height, ItemStack leaves)
    {
        final int layers = height + 2;
        for (int y1 = (int) y, layer = 0; layer < layers; layer++, y1++)
        {
            if (layer < 2)
            {
                generateCanopyLayer(world, rand, x, y1, z, radius * Math.cos((layer) / (height / 1.3)), 2 + (layer * 5), leaves);
            }
            else
            {
                generateCanopyLayer(world, rand, x, y1, z, radius * Math.cos((layer) / (height / 1.3)), 1000, leaves);
            }
        }
    }
    
    public void generateVerticalCone(World world, int x, int y, int z, int height, double r1, double r2, ItemStack leaves)
    {
        final double ratio = (r2 - r1) / (height - 1);
        
        for (int offset = 0; offset < height; offset++)
        {
            placeLeavesCircle(x, y + offset, z, (int)((ratio * offset) + r1), leaves, world);
        }
    }
    
    public boolean checkCanopyLayer(World world, int x, int y, int z, double radius)
    {
        final float minDist = (radius - 3 > 0) ? (float)((radius - 3) * (radius - 3)) : -1;
        final float maxDist = (float)(radius * radius);
        
        try { for (int z1 = (int) -radius; z1 < (radius + 1); z1++)
        {
            for (int x1 = (int) -radius; x1 < (radius + 1); x1++)
            {
                final Block block = world.getBlock(x1 + x, y, z1 + z);
                
                if ((((x1 * x1) + (z1 * z1)) <= maxDist) && (((x1 * x1) + (z1 * z1)) >= minDist))
                {
                    if (block != null && !block.isReplaceable(world,x1 + x, y, z1 + z))
                    {
                        return false;
                    }
                }
            }
        }} catch (Exception e) {LogHelper.info("Sakura tree tried to generate in an ungenerated chunk.");return false;}
        
        return true;
    }
    
    public void generateCanopyLayer(World world, Random rand, int x, int y, int z, double radius, int skipChance, ItemStack leaves)
    {
        final float minDist = (radius - 3 > 0) ? (float)((radius - 3) * (radius - 3)) : -1;
        final float maxDist = (float)(radius * radius);
        
        for (int z1 = (int) -radius; z1 < (radius + 1); z1++)
        {
            for (int x1 = (int) -radius; x1 < (radius + 1); x1++)
            {
                final Block block = world.getBlock(x1 + x, y, z1 + z);
                
                if ((((x1 * x1) + (z1 * z1)) <= maxDist) && (((x1 * x1) + (z1 * z1)) >= minDist))
                {
                    if (block == null || block.canBeReplacedByLeaves(world, x1 + x, y, z1 + z))
                    {
                        if (rand.nextInt(skipChance) != 0)
                        {
                            setLeafBlock(world, x1 + x, y, z1 + z, leaves);
                        }
                    }
                }
            }
        }
    }
    
    public static long getLastSeed()
    {
        return lastSeed;
    }
    
}
