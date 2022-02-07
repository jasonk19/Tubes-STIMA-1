package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
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
        if (myCar.speed <= 9 && myCar.speed >= 5) {
            for (int i = max(block - startBlock, 0); i <= block - startBlock + Bot.maxSpeed + 1; i++) {
                if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                    break;
                }

                blocks.add(laneList[i].terrain);
            }
        } else if (myCar.speed == 15) {
            for (int i = max(block - startBlock, 0); i <= block - startBlock + Bot.maxBoostSpeed + 1; i++) {
                if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                    break;
                }
                blocks.add(laneList[i].terrain);
            }
        } else if (myCar.speed < 5) {
            for (int i = max(block - startBlock, 0); i <= block - startBlock + Bot.initSpeed; i++) {
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

    private boolean obstacles(List<Object> blocks) {
        return blocks.contains(Terrain.MUD) || blocks.contains(Terrain.WALL) || blocks.contains(Terrain.OIL_SPILL) || blocks.contains(Terrain.CYBERTRUCK);
    }

    private boolean speedPowerUp(List<Object> blocks) {
        return blocks.contains(Terrain.BOOST) || blocks.contains(Terrain.LIZARD);
    }

    private boolean otherPowerUp(List<Object> blocks) {
        return blocks.contains(Terrain.TWEET) || blocks.contains(Terrain.OIL_POWER) || blocks.contains(Terrain.EMP);
    }

    // Avoiding logic when there is obstacles in front
    private Command changeLane(List<Object> blocks) {

        if (countTerrain(blocks, Terrain.MUD) == 1 || blocks.contains(Terrain.WALL)) {
            if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                return LIZARD;
            }
        }


        if (myCar.position.lane == 1) {
            List<Object> blocksRight = getBlocksInFront(myCar.position.lane + 1, myCar.position.block);
            if (!obstacles(blocksRight)) {
                return TURN_RIGHT;
            }
            if (blocksRight.contains(Terrain.WALL) && !blocks.contains(Terrain.WALL)) {
                return ACCELERATE;
            }
            if (!blocksRight.contains(Terrain.WALL) && blocks.contains(Terrain.WALL)){
                return TURN_RIGHT;
            }
            if (blocksRight.contains(Terrain.MUD)) {
                if (countTerrain(blocks, Terrain.MUD) > countTerrain(blocksRight, Terrain.MUD)) {
                    if (blocksRight.contains(Terrain.WALL)) {
                        return ACCELERATE;
                    }
                    return TURN_RIGHT;
                } else {
                    return ACCELERATE;
                }
            }
            if (isOppInRange(myCar.position.lane, myCar.position.block)) {
                return TURN_RIGHT;
            }
            return TURN_RIGHT;
        }
        if (myCar.position.lane == getMaxLane()) {
            List<Object> blocksLeft = getBlocksInFront(myCar.position.lane - 1, myCar.position.block);
            if (!obstacles(blocksLeft)) {
                return TURN_LEFT;
            }
            if (blocksLeft.contains(Terrain.WALL) && !blocks.contains(Terrain.WALL)) {
                return ACCELERATE;
            }
            if (!blocksLeft.contains(Terrain.WALL) && blocks.contains(Terrain.WALL)){
                return TURN_LEFT;
            }
            if (blocksLeft.contains(Terrain.MUD)) {
                if (countTerrain(blocks, Terrain.MUD) > countTerrain(blocksLeft, Terrain.MUD)) {
                    if (blocksLeft.contains(Terrain.WALL)) {
                        return ACCELERATE;
                    }
                    return TURN_LEFT;
                } else {
                    return ACCELERATE;
                }
            }
            if (isOppInRange(myCar.position.lane, myCar.position.block)) {
                return TURN_LEFT;
            }
            return TURN_LEFT;
        }
        if (myCar.position.lane > 1 || myCar.position.lane < getMaxLane()) {
            List<Object> blocksRight = getBlocksInFront(myCar.position.lane + 1, myCar.position.block);
            List<Object> blocksLeft = getBlocksInFront(myCar.position.lane - 1, myCar.position.block);

            if (!obstacles(blocksLeft)) {
                return TURN_LEFT;
            }
            if (!obstacles(blocksRight)) {
                return TURN_RIGHT;
            }
            if (blocksLeft.contains(Terrain.WALL) && !blocks.contains(Terrain.WALL) && blocksRight.contains(Terrain.WALL)) {
                return ACCELERATE;
            } else if (!blocksLeft.contains(Terrain.WALL) && blocks.contains(Terrain.WALL) && blocksRight.contains(Terrain.WALL)) {
                return TURN_LEFT;
            } else if (blocksLeft.contains(Terrain.WALL) && blocks.contains(Terrain.WALL) && !blocksRight.contains(Terrain.WALL)) {
                return TURN_RIGHT;
            }
            int i = random.nextInt(directionList.size());
            return directionList.get(i);
        }
        return ACCELERATE;
    }


    public Command run() {

        // Mengambil block di depan dalam jangkauan speed mobil
        List<Object> blocks = getBlocksInFront(myCar.position.lane, myCar.position.block);
        List<Object> nextBlock = blocks.subList(0,1);

        // Fix Car
        if (myCar.damage >= 2) {
            return FIX;
        }

        if (hasPowerUp(PowerUps.BOOST, myCar.powerups)) {
            if (myCar.boosting || obstacles(blocks)) {
                if (obstacles(blocks)) {
                    return changeLane(blocks);
                }
            }
            return BOOST;
        }

        // Accelerate if too slow
        if (myCar.speed == 0) {
            return ACCELERATE;
        }

        if (obstacles(blocks)) {
            return changeLane(blocks);
        }

        if (myCar.speed <= 5) {
            return ACCELERATE;
        }

        // Check if powerups too many, then use powerup

        if (hasPowerUp(PowerUps.TWEET, myCar.powerups)) {
            if (myCar.position.block > opponent.position.block) {
                return new TweetCommand(opponent.position.lane, opponent.position.block + opponent.speed + 2);
            }
        }


        if (myCar.position.lane == opponent.position.lane) {
            if (myCar.position.block > opponent.position.block) {
                if (hasPowerUp(PowerUps.OIL, myCar.powerups)) {
                    return OIL;
                }
            }
        }

        if(myCar.position.block < opponent.position.block) {
            if (hasPowerUp(PowerUps.EMP, myCar.powerups)) {
                return EMP;
            }
        }

        return ACCELERATE;
    }

}
