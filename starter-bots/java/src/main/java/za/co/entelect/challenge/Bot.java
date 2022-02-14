package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.State;
import za.co.entelect.challenge.enums.Terrain;

import java.util.*;

import java.security.SecureRandom;

import static java.lang.Math.max;

public class Bot {

    private static final int initSpeed = 5;
    private static final int maxSpeed = 9;
    private static final int maxBoostSpeed = 15;
    private final List<Command> directionList = new ArrayList<>();

    private final Random random;
    private final GameState gameState;
    private final Car myCar;
    private final Car opponent;

    private final static Command ACCELERATE = new AccelerateCommand();
    private final static Command DECELERATE = new DecelerateCommand();
    private final static Command LIZARD = new LizardCommand();
    private final static Command OIL = new OilCommand();
    private final static Command BOOST = new BoostCommand();
    private final static Command EMP = new EmpCommand();
    private final static Command FIX = new FixCommand();

    private final static Command TURN_RIGHT = new ChangeLaneCommand(1);
    private final static Command TURN_LEFT = new ChangeLaneCommand(-1);

    public Bot(GameState gameState) {
        this.random = new SecureRandom();
        this.gameState = gameState;
        this.myCar = gameState.player;
        this.opponent = gameState.opponent;

        directionList.add(TURN_LEFT);
        directionList.add(TURN_RIGHT);
    }

    private int countPowerUp(PowerUps powerUpToCheck, PowerUps[] amount) {
        int count = 0;
        for (PowerUps powerUp: amount) {
            if (powerUp.equals(powerUpToCheck))  {
                count++;
            }
        }
        return count;
    }

    // Check if powerup exist
    private Boolean hasPowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                return true;
            }
        }
        return false;
    }

    private int getMaxLane() {
        return gameState.lanes.size();
    }

    /**
     * Returns map of blocks and the objects in the for the current lanes, returns the amount of blocks that can be
     * traversed at max speed.
     **/
    private List<Object> getBlocksInFront(int lane, int block) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        if (myCar.speed <= 9) {
            for (int i = max(block - startBlock, 0); i <= block - startBlock + Bot.maxSpeed; i++) {
                if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                    break;
                }

                blocks.add(laneList[i].terrain);
            }
        } else if (myCar.speed == 15 || myCar.state.equals(State.USED_BOOST)) {
            for (int i = max(block - startBlock, 0); i <= block - startBlock + Bot.maxBoostSpeed; i++) {
                if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                    break;
                }
                blocks.add(laneList[i].terrain);
            }
        }

        return blocks;
    }

    private List<Object> getManyBlocks(int lane, int block) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + 16; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);
        }

        return blocks;
    }

    private boolean isOppInRange(int lane, int block) {
        int range = opponent.position.block - myCar.position.block;
        if (myCar.position.lane == opponent.position.lane) {
            if (myCar.speed <= 9) {
                if (range <= 10) {
                    return true;
                }
            }
            if (myCar.speed == 15) {
                if (range <= 16) {
                    return true;
                }
            }
        }
        return false;
    }

    private int countTerrain(List<Object> blocks, Terrain terrainToCheck) {
        int count = 0;
        for (Object block: blocks) {
            if (block.equals(terrainToCheck)) {
                count++;
            }
        }
        return count;
    }

    private boolean hasCyberTruck(int lane) {
        int block = myCar.position.block;
        List<Lane[]> map = gameState.lanes;
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + Bot.maxSpeed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }
            if (laneList[i].isOccupiedByCyberTruck) {
                return true;
            }
        }
        return false;
    }

    private boolean checkEmpRange() {
        return myCar.position.lane == opponent.position.lane || opponent.position.lane - myCar.position.lane == 1 || myCar.position.lane - opponent.position.lane == 1;
    }

    private boolean obstacles(List<Object> blocks) {
        return blocks.contains(Terrain.MUD) || blocks.contains(Terrain.WALL) || blocks.contains(Terrain.OIL_SPILL);
    }

    private boolean containPowerUp(List<Object> blocks) {
        return blocks.contains(Terrain.BOOST) || blocks.contains(Terrain.LIZARD) || blocks.contains(Terrain.EMP) || blocks.contains(Terrain.TWEET) || blocks.contains(Terrain.OIL_POWER);
    }

//  Find powerup if there is no obstacles
    private Command findPowerUp(List<Object> blocks) {
        if (!obstacles(blocks) && !hasCyberTruck(myCar.position.lane)) {
            if (myCar.position.lane == 1) {
                List<Object> blocksRight = getBlocksInFront(myCar.position.lane + 1, myCar.position.block);
                if (containPowerUp(blocks)) {
                    return ACCELERATE;
                }
                if (containPowerUp(blocksRight)) {
                    if (!obstacles(blocksRight) && !hasCyberTruck(myCar.position.lane + 1)) {
                        return TURN_RIGHT;
                    }
                }
            }
            if (myCar.position.lane == getMaxLane()) {
                List<Object> blocksLeft = getBlocksInFront(myCar.position.lane - 1, myCar.position.block);
                if (containPowerUp(blocks)) {
                    return ACCELERATE;
                }
                if (containPowerUp(blocksLeft)) {
                    if (!obstacles(blocksLeft) && !hasCyberTruck(myCar.position.lane - 1)) {
                        return TURN_LEFT;
                    }
                }
            }
            if (myCar.position.lane > 1 && myCar.position.lane < getMaxLane()) {
                List<Object> blocksLeft = getBlocksInFront(myCar.position.lane - 1, myCar.position.block);
                List<Object> blocksRight = getBlocksInFront(myCar.position.lane + 1, myCar.position.block);

                if (containPowerUp(blocks)) {
                    return ACCELERATE;
                }

                if (containPowerUp(blocksLeft)) {
                    if (!obstacles(blocksLeft) && !hasCyberTruck(myCar.position.lane - 1)) {
                        return TURN_LEFT;
                    }
                }

                if (containPowerUp(blocksRight)) {
                    if (!obstacles(blocksRight) && !hasCyberTruck(myCar.position.lane + 1)) {
                        return TURN_RIGHT;
                    }
                }

                if (containPowerUp(blocksLeft) && containPowerUp(blocksRight)) {
                    if (!obstacles(blocksLeft) && !obstacles(blocksRight) && !hasCyberTruck(myCar.position.lane - 1) && !hasCyberTruck(myCar.position.lane + 1)) {
                        int i = random.nextInt(directionList.size());
                        return directionList.get(i);
                    }
                }

            }
        }

        return ACCELERATE;
    }

    // Avoiding logic when there is obstacles in front
    private Command changeLane(List<Object> blocks) {

        if (!obstacles(blocks) && !hasCyberTruck(myCar.position.lane)) {
            return ACCELERATE;
        }

        if (countTerrain(blocks, Terrain.MUD) >= 3 || blocks.contains(Terrain.WALL) || hasCyberTruck(myCar.position.lane)) {
            if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                return LIZARD;
            }
        }


        if (myCar.position.lane == 1) {
            List<Object> blocksRight = getBlocksInFront(myCar.position.lane + 1, myCar.position.block);
            if (!obstacles(blocksRight) && !hasCyberTruck(myCar.position.lane + 1)) {
                return TURN_RIGHT;
            }
            if (hasCyberTruck(myCar.position.lane)) {
                return TURN_RIGHT;
            }
            if (hasCyberTruck(myCar.position.lane + 1)) {
                return ACCELERATE;
            }
            if (blocksRight.contains(Terrain.WALL) && !blocks.contains(Terrain.WALL)) {
                return ACCELERATE;
            }
            if (!blocksRight.contains(Terrain.WALL) && blocks.contains(Terrain.WALL)){
                return TURN_RIGHT;
            }
            if (blocksRight.contains(Terrain.MUD)) {
                if (countTerrain(blocks, Terrain.MUD) > countTerrain(blocksRight, Terrain.MUD)) {
                    return TURN_RIGHT;
                }
                return ACCELERATE;
            }
            if (isOppInRange(myCar.position.lane, myCar.position.block)) {
                return TURN_RIGHT;
            }
            return TURN_RIGHT;
        }
        if (myCar.position.lane == getMaxLane()) {
            List<Object> blocksLeft = getBlocksInFront(myCar.position.lane - 1, myCar.position.block);
            if (!obstacles(blocksLeft) && !hasCyberTruck(myCar.position.lane - 1)) {
                return TURN_LEFT;
            }
            if (hasCyberTruck(myCar.position.lane)) {
                return TURN_LEFT;
            }
            if (hasCyberTruck(myCar.position.lane - 1)) {
                return ACCELERATE;
            }
            if (blocksLeft.contains(Terrain.WALL) && !blocks.contains(Terrain.WALL)) {
                return ACCELERATE;
            }
            if (!blocksLeft.contains(Terrain.WALL) && blocks.contains(Terrain.WALL)){
                return TURN_LEFT;
            }
            if (blocksLeft.contains(Terrain.MUD)) {
                if (countTerrain(blocks, Terrain.MUD) > countTerrain(blocksLeft, Terrain.MUD)) {
                    return TURN_LEFT;
                }
                return ACCELERATE;
            }
            if (isOppInRange(myCar.position.lane, myCar.position.block)) {
                return TURN_LEFT;
            }
            return TURN_LEFT;
        }
        if (myCar.position.lane > 1 && myCar.position.lane < getMaxLane()) {
            List<Object> blocksRight = getBlocksInFront(myCar.position.lane + 1, myCar.position.block);
            List<Object> blocksLeft = getBlocksInFront(myCar.position.lane - 1, myCar.position.block);

            if (!obstacles(blocksLeft) && !hasCyberTruck(myCar.position.lane - 1)) {
                return TURN_LEFT;
            }
            if (!obstacles(blocksRight) && !hasCyberTruck(myCar.position.lane + 1)) {
                return TURN_RIGHT;
            }
            if (hasCyberTruck(myCar.position.lane) || hasCyberTruck(myCar.position.lane + 1) || hasCyberTruck(myCar.position.lane - 1)) {
                if (hasCyberTruck(myCar.position.lane) && !hasCyberTruck(myCar.position.lane + 1) && !hasCyberTruck(myCar.position.lane - 1)) {
                    if (!blocksLeft.contains(Terrain.WALL)) {
                        return TURN_LEFT;
                    }
                    return TURN_RIGHT;
                }
                if (hasCyberTruck(myCar.position.lane + 1) && !hasCyberTruck(myCar.position.lane) && !hasCyberTruck(myCar.position.lane - 1)) {
                    if (!blocks.contains(Terrain.WALL)) {
                        return ACCELERATE;
                    }
                    return TURN_LEFT;
                }
                if (hasCyberTruck(myCar.position.lane - 1) && !hasCyberTruck(myCar.position.lane) && !hasCyberTruck(myCar.position.lane + 1)) {
                    if (!blocks.contains(Terrain.WALL)) {
                        return ACCELERATE;
                    }
                    return TURN_RIGHT;
                }
            }

            if (blocks.contains(Terrain.WALL) || blocksLeft.contains(Terrain.WALL) || blocksRight.contains(Terrain.WALL)) {
                if (!blocks.contains(Terrain.WALL)) {
                    return ACCELERATE;
                }
                if (!blocksLeft.contains(Terrain.WALL)) {
                    return TURN_LEFT;
                }
                if (!blocksRight.contains(Terrain.WALL)) {
                    return TURN_RIGHT;
                }
            }


            if (countTerrain(blocksLeft, Terrain.MUD) < countTerrain(blocks, Terrain.MUD)) {
                if (countTerrain(blocksRight, Terrain.MUD) < countTerrain(blocksLeft, Terrain.MUD))  {
                    return TURN_RIGHT;
                }
                return TURN_LEFT;
            }

            if (isOppInRange(myCar.position.lane, myCar.position.block)) {
                int i = random.nextInt(directionList.size());
                return directionList.get(i);
            }

            int i = random.nextInt(directionList.size());
            return directionList.get(i);
        }
        return ACCELERATE;
    }


    public Command run() {

        // Mengambil block di depan dalam jangkauan speed mobil
        List<Object> blocks = getBlocksInFront(myCar.position.lane, myCar.position.block);
        List<Object> nextBlocks = blocks.subList(0, 1);
        List<Object> manyBlocks = getManyBlocks(myCar.position.lane, myCar.position.block);

        // Fix Car
        if (myCar.damage >= 2) {
            return FIX;
        }

        // Accelerate if too slow
        if (myCar.speed == 0) {
            return ACCELERATE;
        }

        if (obstacles(blocks) || hasCyberTruck(myCar.position.lane) || myCar.boosting) {
            return changeLane(blocks);
        }

        if (hasPowerUp(PowerUps.BOOST, myCar.powerups)) {
            if (!obstacles(manyBlocks) && !hasCyberTruck(myCar.position.lane)) {
                return BOOST;
            }
        }

        if (myCar.speed <= 5) {
            return ACCELERATE;
        }

        if(myCar.position.block < opponent.position.block) {
            if (hasPowerUp(PowerUps.EMP, myCar.powerups)) {
                if (checkEmpRange()) {
                    return EMP;
                }
            }
        }

        if (hasPowerUp(PowerUps.TWEET, myCar.powerups)) {
            return new TweetCommand(opponent.position.lane, opponent.position.block + opponent.speed + 2);
        }

        if (myCar.position.block > opponent.position.block) {
            if (hasPowerUp(PowerUps.OIL, myCar.powerups)) {
                return OIL;
            }
        }



        return findPowerUp(blocks);
    }

}
