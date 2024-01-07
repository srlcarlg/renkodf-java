package renkodf;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import renkodf.wrappers.OHLCV;
import renkodf.wrappers.RSD;

public class Renko {

	private List<RSD> rsd = new ArrayList<>();
	private Double brickSize;
	
	private Double wickMinInLoop;
	private Double wickMaxInLoop;
	private Double volumeInLoop;
	
	private final Logger logger = Logger.getLogger(getClass().getName());
	private static final List<String> MODE_LIST = List.of("normal","wicks", "nongap",
			"reverse-wicks", "reverse-nongap", "fake-r-wicks", "fake-r-nongap");

	public Renko (List<OHLCV> dfList, Double brickSize) {

		Double firstClose = dfList.get(0).getClose();
		Double initialPrice = (Math.floor(firstClose/brickSize)) * brickSize;
		
		// Renko Single Data
		rsd.add(new RSD(0, dfList.get(0).getDatetime(), initialPrice, 0D, initialPrice, 1D));

		this.brickSize = brickSize;
        wickMinInLoop = initialPrice;
        wickMaxInLoop = initialPrice;
        volumeInLoop = 1D;
        
        Integer dfSize = dfList.size();
        for (int i = 1; i < dfSize; i++) {
        	addPrices(i, dfList);
		}
	}
	
	private boolean addPrices(Integer i, List<OHLCV> dfList) {

		Double dfClose = dfList.get(i).getClose();
		Object dfDatetime = dfList.get(i).getDatetime();

		wickMinInLoop = dfClose.compareTo(wickMinInLoop) < 0 ? dfClose : wickMinInLoop;
		wickMaxInLoop = dfClose.compareTo(wickMaxInLoop) > 0 ? dfClose : wickMaxInLoop;
		volumeInLoop += 1D;

		Double lastPrice = rsd.get(rsd.size()-1).getPrice();
		Double currentNumberBricks = (dfClose - lastPrice) / brickSize;
		Double currentDirection = Math.signum(currentNumberBricks);
		if (currentDirection == 0) {
        	return false;
        }  
		Double lastDirection = rsd.get(rsd.size()-1).getDirection();
		boolean isSameDirection = (currentDirection > 0 && lastDirection >= 0) || (currentDirection < 0 && lastDirection <= 0);
        
		// CURRENT PRICE in same direction of the LAST RENKO
		Double totalSameBricks = isSameDirection ? currentNumberBricks : 0;
        /* >= 2 can be a 'GAP' or 'OPPOSITE DIRECTION'.
         * In both cases we add the current wick/volume to the first brick and 'reset' the value of both, since:
         * If it's a GAP: 
		 *	- The following bricks after first brick will be 'artificial' since the price has 'skipped' that price region.
		 *	- (the reason of 'totalSameBricks')
		 * If it's a OPPOSITE DIRECTION:
		 *	- Only the first brick will be kept. (the reason of '2' multiply) 
		 */
        if (!isSameDirection && Math.abs(currentNumberBricks) >= 2) {
        	addBrickLoop(i, dfDatetime, 2, currentDirection, currentNumberBricks);
        	totalSameBricks =  currentNumberBricks - (2 * currentDirection);
        }
        
        // Add all bricks in the same direction
        for (int noUse = 0; noUse < Math.abs(totalSameBricks.intValue()); noUse++) {
        	addBrickLoop(i, dfDatetime, 1, currentDirection, currentNumberBricks);
		}

        return true;
	}
	private void addBrickLoop(Integer i, Object dfDatetime, Integer renkoMultiply, Double currentDirection, Double currentNumberBricks) {
		// Need update value because of 'same direction' inner loop
		Double lastPrice = rsd.get(rsd.size()-1).getPrice();
        Double renkoPrice = lastPrice + (currentDirection * renkoMultiply * brickSize);
        Double wick = currentNumberBricks > 0 ? wickMinInLoop  : wickMaxInLoop;
        
        rsd.add(new RSD(i, dfDatetime, renkoPrice, currentDirection, wick, volumeInLoop));
        
        // Reset
        volumeInLoop = 1D;
        wickMinInLoop = currentNumberBricks > 0 ? renkoPrice : wickMinInLoop;
        wickMaxInLoop = currentNumberBricks < 0 ? renkoPrice : wickMaxInLoop;
	}

	public List<OHLCV> renkodf(String mode) {
		
		String msg = String.format("Mode: \"%s\" does not exist, using \"normal\" instead", mode);
		if (!MODE_LIST.contains(mode)) {
			logger.log(Level.WARNING, msg);
			mode = MODE_LIST.get(0);
		}

		List<OHLCV> renkoList = new ArrayList<>(rsd.size());

        boolean reverseModeRule = Set.of("normal","wicks","reverse-wicks","fake-r-wicks").contains(mode);
        boolean fakeReverseModeRule = Set.of("fake-r-nongap", "fake-r-wicks").contains(mode);
        boolean sameDirectionRule = Set.of("wicks","nongap").contains(mode);

        Double prevDirection = 0D;
        Double prevClose = 0D;
		Double prevCloseUP = 0D;
        Double prevCloseDOWN = 0D;
        for (int i = 0; i < rsd.size(); i++) {

        	Double price = rsd.get(i).getPrice();
        	Double direction = rsd.get(i).getDirection();
        	Object date = rsd.get(i).getDate();
        	Double wick = rsd.get(i).getWick();
        	Double volume = rsd.get(i).getVolume();
        	
            OHLCV toAdd = new OHLCV(date, price, volume);

            // Current Renko (UP)
        	if (direction == 1.0) {
        		
                toAdd.setHigh(price);
                // Previous same direction(UP)
                if (prevDirection == 1) {
                	toAdd.setOpen(mode.equals("nongap") ? wick : prevClose);
                	toAdd.setLow(sameDirectionRule ? wick : prevClose);
                }
                // Previous reverse direction(DOWN) 
                else {
                    if (reverseModeRule) {
                    	toAdd.setOpen(prevClose + brickSize);
                    } else if (mode.equals("fake-r-nongap")) {
                    	toAdd.setOpen(prevCloseDOWN);
                    } else {
                    	toAdd.setOpen(wick);
                    }
                    if (mode.equals("normal")) {
                    	toAdd.setLow(prevClose + brickSize);
                    } else if (fakeReverseModeRule) {
                    	toAdd.setLow(prevCloseDOWN);
                    } else {
                    	toAdd.setLow(wick);
                    }
        		}
                
                renkoList.add(toAdd);
                prevCloseUP = price;
        	}
            // Current Renko (DOWN)
        	else if (direction == -1) {
        		
                toAdd.setLow(price);
                // Previous same direction(DOWN)
                if (prevDirection == -1) {
                	toAdd.setOpen(mode.equals("nongap") ? wick : prevClose);
                	toAdd.setHigh(sameDirectionRule ? wick : prevClose);
                }
                // Previous reverse direction(UP) 
                else {
                    if (reverseModeRule) {
                    	toAdd.setOpen(prevClose - brickSize);
                    } else if (mode.equals("fake-r-nongap")) {
                    	toAdd.setOpen(prevCloseUP);
                    } else {
                    	toAdd.setOpen(wick);
                    }

                    if (mode.equals("normal")) {
                    	toAdd.setHigh(prevClose - brickSize);
                    } else if (fakeReverseModeRule) {
                    	toAdd.setHigh(prevCloseUP);
                    } else {
                    	toAdd.setHigh(wick);
                    }
        		}
                
                renkoList.add(toAdd);
                prevCloseDOWN = price;
        	}

            prevDirection = direction;
            prevClose = price;
		}

        // Removing first row
        renkoList.remove(0);

        return renkoList;
	}
}
