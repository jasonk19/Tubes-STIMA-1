package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.State;
import za.co.entelect.challenge.enums.Terrain;

import java.util.*;

import javax.swing.tree.TreeCellRenderer;

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
        } else if (myCar.speed == 15) {
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

    private boolean isOppInRange() {
        // Return true apabila bot musuh berada dalam jarak yang dapat dikejar 
        // False jika tidak
        int range = opponent.position.block - myCar.position.block;
        if (myCar.position.lane == opponent.position.lane) {
            if (myCar.speed <= 9) {
                if (range <= 9) {
                    return true;
                }
            }
            if (myCar.speed == 15) {
                if (range <= 15) {
                    return true;
                }
            }
        }
        return false;
    }

    private int countTerrain(List<Object> blocks, Terrain terrainToCheck) {
        // Menghitung jumlah terrain spesifik pada suatu blocks
        // terrainToCheck dapat bernilai OIL_SPILL, MUD, WALL, dll 
        int count = 0;
        for (Object block: blocks) {
            if (block.equals(terrainToCheck)) {
                count++;
            }
        }
        return count;
    }

    private boolean hasCyberTruck(int lane) {
        // Mengirimkan true jika terdapat cybertruck pada lane 
        // False jika tidak
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
        // Mengirimkan true jika jarak mobil musuh cukup dekat untuk di EMP 
        // False jika tidak
        return myCar.position.lane == opponent.position.lane || opponent.position.lane - myCar.position.lane == 1 || myCar.position.lane - opponent.position.lane == 1;
    }

    private boolean obstacles(List<Object> blocks) {
        // Mengirimkan true jika terdapat obstacle berupa mud, wall, ataupun oil spill
        // False jika tidak 
        return blocks.contains(Terrain.MUD) || blocks.contains(Terrain.WALL) || blocks.contains(Terrain.OIL_SPILL);
    }

    private boolean containPowerUp(List<Object> blocks) {
        // Mengirimkan true jika terdapat power up pada blocks 
        // False jika tidak
        return blocks.contains(Terrain.BOOST) || blocks.contains(Terrain.LIZARD) || blocks.contains(Terrain.EMP) || blocks.contains(Terrain.TWEET) || blocks.contains(Terrain.OIL_POWER);
    }
    private boolean definiteCrash(int lane){
        // Mengirimkan true jika pada round selanjutnya mobil pasti akan menabrak obstacle 
        // Pengecekan lane tergantung posisi mobil
        
        if (lane == 1){
            return (hasCyberTruck(myCar.position.lane) || obstacles(getBlocksInFront(myCar.position.lane, myCar.position.block))) && (hasCyberTruck(myCar.position.lane + 1) || obstacles(getBlocksInFront(myCar.position.lane + 1, myCar.position.block)));
        } else if (lane == 2 || lane == 3){
            return (hasCyberTruck(myCar.position.lane) || obstacles(getBlocksInFront(myCar.position.lane, myCar.position.block))) && (hasCyberTruck(myCar.position.lane + 1) || obstacles(getBlocksInFront(myCar.position.lane + 1, myCar.position.block))) && (hasCyberTruck(myCar.position.lane - 1) || obstacles(getBlocksInFront(myCar.position.lane-1, myCar.position.block))); 
        } else {
            return (hasCyberTruck(myCar.position.lane) || obstacles(getBlocksInFront(myCar.position.lane, myCar.position.block))) && (hasCyberTruck(myCar.position.lane - 1) || obstacles(getBlocksInFront(myCar.position.lane-1, myCar.position.block)));
        }
    }


    private int getDamageBlocks(List<Object> blocks, int lane){
        // Mengirimkan jumlah total damage yang akan diberikan obstacle kepada mobil jika mobile melaju pada lane 
        // Nilai damage disesuaikan dengan game spec Overdrive
        int damage = 0;
        damage += countTerrain(blocks, Terrain.OIL_SPILL);
        damage += countTerrain(blocks, Terrain.MUD);
        damage += countTerrain(blocks, Terrain.WALL) * 2;
        if (hasCyberTruck(lane)){
            damage += 2;
        }
        return damage;
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
        // Greedy by Obstacle
        if (!obstacles(blocks) && !hasCyberTruck(myCar.position.lane)) {
            return ACCELERATE;
        }


        if (myCar.position.lane == 1) {
            List<Object> blocksRight = getBlocksInFront(myCar.position.lane + 1, myCar.position.block);
            if (!obstacles(blocksRight) && !hasCyberTruck(myCar.position.lane + 1)) {
                return TURN_RIGHT;
            }
            if (countTerrain(blocks, Terrain.MUD) >= 2 || blocks.contains(Terrain.WALL) || hasCyberTruck(myCar.position.lane) || isOppInRange()) {
                if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                    return LIZARD;
                }
            }
            if (definiteCrash(1)){ // Greedy by Damage
                int damageRight = getDamageBlocks(blocksRight, myCar.position.lane + 1);
                int damageStraight = getDamageBlocks(blocks, myCar.position.lane);
                if (damageRight < damageStraight){
                    return TURN_RIGHT;
                } else {
                    return ACCELERATE;
                }
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

            if (isOppInRange()) {
                return TURN_RIGHT;
            }
            return TURN_RIGHT;
        }
        if (myCar.position.lane == getMaxLane()) {
            List<Object> blocksLeft = getBlocksInFront(myCar.position.lane - 1, myCar.position.block);
            if (!obstacles(blocksLeft) && !hasCyberTruck(myCar.position.lane - 1)) {
                return TURN_LEFT;
            }
            if (countTerrain(blocks, Terrain.MUD) >= 3 || blocks.contains(Terrain.WALL) || hasCyberTruck(myCar.position.lane) || isOppInRange()) {
                if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                    return LIZARD;
                }
            }
            if (definiteCrash(getMaxLane())){ // Greedy by Damage 
                int damageLeft = getDamageBlocks(blocksLeft, myCar.position.lane - 1);
                int damageStraight = getDamageBlocks(blocks, myCar.position.lane);
                if (damageLeft < damageStraight){
                    return TURN_LEFT;
                } else {
                    return ACCELERATE;
                }
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

            if (isOppInRange()) {
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
            if (countTerrain(blocks, Terrain.MUD) >= 3 || blocks.contains(Terrain.WALL) || hasCyberTruck(myCar.position.lane) || isOppInRange()) {
                if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                    return LIZARD;
                }
            }
            if (myCar.position.lane == 2){ // Greedy by Damage
                if (definiteCrash(2)){
                    int damageRight = getDamageBlocks(blocksRight, myCar.position.lane + 1);
                    int damageStraight = getDamageBlocks(blocks, myCar.position.lane);
                    int damageLeft = getDamageBlocks(blocksLeft, myCar.position.lane - 1);
                    int min = Math.min(Math.min(damageRight, damageLeft), damageStraight);
                    if (min == damageStraight){
                        return ACCELERATE;
                    } else if (min == damageRight){
                        return TURN_RIGHT;
                    } else {
                        return TURN_LEFT;
                    }
                }
            } else { //lane = 3
                if (definiteCrash(3)){ // Greedy by Damage 
                    int damageRight = getDamageBlocks(blocksRight, myCar.position.lane + 1);
                    int damageStraight = getDamageBlocks(blocks, myCar.position.lane);
                    int damageLeft = getDamageBlocks(blocksLeft, myCar.position.lane - 1);
                    int min = Math.min(Math.min(damageRight, damageLeft), damageStraight);
                    if (min == damageStraight){
                        return ACCELERATE;
                    } else if (min == damageRight){
                        return TURN_RIGHT;
                    } else {
                        return TURN_LEFT;
                    }
                }
            }
            if (hasCyberTruck(myCar.position.lane) || hasCyberTruck(myCar.position.lane + 1) || hasCyberTruck(myCar.position.lane - 1)) {
                if (!hasCyberTruck(myCar.position.lane)) {
                    return ACCELERATE;
                }
                if (!hasCyberTruck(myCar.position.lane + 1)) {
                    return TURN_RIGHT;
                }
                if (!hasCyberTruck(myCar.position.lane - 1)) {
                    return TURN_LEFT;
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


            if (isOppInRange()) {
                int i = random.nextInt(directionList.size());
                return directionList.get(i);
            }
        }
        return ACCELERATE;
    }


    public Command run() {

        // Mengambil block di depan dalam jangkauan speed mobil
        List<Object> blocks = getBlocksInFront(myCar.position.lane, myCar.position.block);
        List<Object> nextBlocks = blocks.subList(0,1);
        List<Object> manyBlocks = getManyBlocks(myCar.position.lane, myCar.position.block);

        // Fix Car
        if (myCar.damage >= 2) {
            return FIX;
        }

        // Accelerate if too slow
        if (myCar.speed == 0) {
            return ACCELERATE;
        }

        if (obstacles(blocks) || hasCyberTruck(myCar.position.lane)) {
            return changeLane(blocks);
        }

        if (hasPowerUp(PowerUps.BOOST, myCar.powerups)) {
            if (!obstacles(manyBlocks) && !hasCyberTruck(myCar.position.lane)) {
                if (!myCar.boosting) {
                    if (myCar.damage != 0) {
                        return FIX;
                    }
                    return BOOST;
                }
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

        if (myCar.position.block > opponent.position.block) {
            if (hasPowerUp(PowerUps.TWEET, myCar.powerups)) {
                if ((myCar.position.block - opponent.position.block) >= Bot.maxBoostSpeed) {
                    return new TweetCommand(opponent.position.lane, opponent.position.block + opponent.speed + 5);
                }
            }

            if (hasPowerUp(PowerUps.OIL, myCar.powerups)) {
                return OIL;
            }
        }

        

        return findPowerUp(blocks);
    }

}
