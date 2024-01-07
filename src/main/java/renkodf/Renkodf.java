package renkodf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Renkodf { 
	
	private static final List<String> MODE_LIST = List.of("normal", "wicks", "nongap", "reverse-wicks", "reverse-nongap", "fake-r-wicks", "fake-r-nongap");
	
	Logger logger = Logger.getLogger(getClass().getName());

	private Map<String, ArrayList<Object>> rsd = new LinkedHashMap<>();
	private Double brickSize;
	private Double wickMinInLoop;
	private Double wickMaxInLoop;
	private Integer volumeInLoop;
	private Map<String, ArrayList<Object>> dfMap;
	
	public Renkodf (Map<String, ArrayList<Object>> dfMap, Double brickSize) {
	
		this.brickSize = brickSize;
		this.dfMap = dfMap;
		
		Double firstClose = Double.valueOf(dfMap.get("close").get(0).toString());
		Double initialPrice = (Math.floor(firstClose/brickSize)) * brickSize;
	
		List<String> headers = Arrays.asList(
			"origin_index", "date", "price", "direction", "wick", "volume"
		);
		headers.forEach(name -> rsd.put(name, new ArrayList<>()));
		
		// Renko Single Data
		rsd.get("origin_index").add(0);
		rsd.get("date").add(dfMap.get("datetime").get(0));
		rsd.get("price").add(initialPrice);
		rsd.get("direction").add(0);
		rsd.get("wick").add(initialPrice);
		rsd.get("volume").add(0);
		
        wickMinInLoop = initialPrice;
        wickMaxInLoop = initialPrice;
        volumeInLoop = 1;
        
        for (int i = 1; i < dfMap.get("close").size(); i++) {
        	addPrices(i);
		}
        
	}
	
	private Object addPrices(Integer i) {

		Double dfClose = Double.valueOf(dfMap.get("close").get(i).toString());
		Object dfDatetime = dfMap.get("datetime").get(i);
		
		if (dfClose.compareTo(wickMinInLoop) < 0) {
			wickMinInLoop = dfClose;
		}
		if (dfClose.compareTo(wickMaxInLoop) > 0) {
			wickMaxInLoop = dfClose;
		}
		volumeInLoop += 1;
		
		Double lastPriceRenko = Double.valueOf(rsd.get("price").get(rsd.get("price").size()-1).toString());		
		Double currentNumberBricks = (dfClose - lastPriceRenko) / brickSize;
		Double currentDirection = Math.signum(currentNumberBricks);
		
		if (currentDirection == 0) {
        	return null;
        }
		
        // CURRENT PRICE in same direction of the LAST RENKO
		Double lastDirection = Double.valueOf(rsd.get("direction").get(rsd.get("direction").size()-1).toString());
		Double totalBricks = 0D;
        if ((currentDirection > 0 && lastDirection >= 0) || (currentDirection < 0 && lastDirection <= 0)) {
            totalBricks = currentNumberBricks;
        }
        // >= 2 can be a 'GAP' or 'OPPOSITE DIRECTION'.
        // In both cases we add the current wick/volume to the first brick and 'reset' the value of both, since:
        // If it is GAP: The following bricks will be 'artificial' since the price has 'skipped' that price region.
        // If it is OPPOSITE DIRECTION: Only the first brick will be kept.
        else if (Math.abs(currentNumberBricks) >= 2) {
        	totalBricks =  currentNumberBricks - 2 * currentDirection;
            Double renkoPrice = lastPriceRenko + (currentDirection * 2 *brickSize);
            Double wick = currentNumberBricks > 0 ? wickMinInLoop  : wickMaxInLoop;
            Integer volume = volumeInLoop;
            
            rsd.get("origin_index").add(i);
            rsd.get("date").add(dfDatetime);
            rsd.get("price").add(renkoPrice);
            rsd.get("direction").add(currentDirection);
            rsd.get("wick").add(wick);
            rsd.get("volume").add(volume);
            
            // Reset
            volumeInLoop = 1;
            if (currentNumberBricks > 0) {
                wickMinInLoop = renkoPrice;
            }
            else {
                wickMaxInLoop = renkoPrice;
            }
        }

        // Add all bricks in the same direction
        for (int noUse = 0; noUse < Math.abs(totalBricks.intValue()); noUse++) {
        	
        	// Need update value because of 'this' inner loop
    		lastPriceRenko = Double.valueOf(rsd.get("price").get(rsd.get("price").size()-1).toString());
            Double renkoPrice = lastPriceRenko + (currentDirection * 1 * brickSize);
            Double wick = currentNumberBricks > 0 ? wickMinInLoop  : wickMaxInLoop;
            Integer volume = volumeInLoop;

            rsd.get("origin_index").add(i);
            rsd.get("date").add(dfDatetime);
            rsd.get("price").add(renkoPrice);
            rsd.get("direction").add(currentDirection);
            rsd.get("wick").add(wick);
            rsd.get("volume").add(volume);
            
            // Reset
            volumeInLoop = 1;
            if (currentNumberBricks > 0) {
                wickMinInLoop = renkoPrice;
            }
            else {
                wickMaxInLoop = renkoPrice;
            }
		}
        
        return null;
	}

	public Map<String, List<String>> renkodf(String mode) {
		
		if (!MODE_LIST.contains(mode)) {
			logger.log(Level.WARNING, String.format(
					"Mode: %s does not exist, using \"normal\" instead", mode));
			mode = "normal";
		}
		
		ArrayList<Object> dates = rsd.get("date");
		List<Double> prices = rsd.get("price").stream().map(price -> Double.valueOf(price.toString())).collect(Collectors.toList());
		List<Double> directions = rsd.get("direction").stream().map(number -> Double.valueOf(number.toString())).collect(Collectors.toList());
		ArrayList<Object> wicks = rsd.get("wick");
		ArrayList<Object> volumes = rsd.get("volume");

		Map<String, ArrayList<Object>> renkoMap = new LinkedHashMap<>();
		List<String> headers = Arrays.asList(
			"datetime", "open", "high", "low", "close", "volume"
		);
		headers.forEach(column -> renkoMap.put(column, new ArrayList<>()));
		
		Double prevCloseHigh = 0D;
        Double prevCloseLow = 0D;
        Double prevDirection = 0D;
        Double reverseHigh = 0D;
        Double reverseLow = 0D;
        for (int i = 0; i < prices.size(); i++) {
        	
        	Double price = prices.get(i);
        	Double direction = directions.get(i);
        	Object date = dates.get(i);
        	Object wick = wicks.get(i);
        	Object volume = volumes.get(i);

            // UP/Bull Renko
        	if (direction == 1.0) {
                renkoMap.get("datetime").add(date);
                renkoMap.get("high").add(price);
                renkoMap.get("close").add(price);
                renkoMap.get("volume").add(volume);
                
                reverseHigh = price - brickSize;

                // PREV UP/Bull Renko
                if (prevDirection == 1) {
                    renkoMap.get("open").add(mode.equals("nongap") ? wick : prevCloseHigh);
                    renkoMap.get("low").add(Set.of("wicks","nongap").contains(mode) ? wick : prevCloseHigh);
                }
                // PREV DOWN/Bear Renko
                else {
                    if (Set.of("normal","wicks","reverse-wicks","fake-r-wicks").contains(mode)) {
                        renkoMap.get("open").add(reverseLow);
                    }
                    else if (mode.equals("fake-r-nongap")) {
                        renkoMap.get("open").add(prevCloseLow);
                    }
                    else {
                        renkoMap.get("open").add(wick);
                    }
                    
                    if (mode.equals("normal")) {
                        renkoMap.get("low").add(reverseLow);
                    }
                    else if (Set.of("fake-r-nongap", "fake-r-wicks").contains(mode)) {
                        renkoMap.get("low").add(prevCloseLow);
                    }
                    else {
                        renkoMap.get("low").add(wick);
                    }
        		}
                prevCloseHigh = price;
        	}
            // DOWN/Bear Renko
        	else if (direction == -1) {
                renkoMap.get("datetime").add(date);
                renkoMap.get("low").add(price);
                renkoMap.get("close").add(price);
                renkoMap.get("volume").add(volume);

                reverseLow = price + brickSize;

                // PREV DOWN/Bear Renko
                if (prevDirection == -1) {
                    renkoMap.get("open").add( mode.equals("nongap") ? wick : prevCloseLow);
                    renkoMap.get("high").add(Set.of("wicks", "nongap").contains(mode) ? wick : prevCloseLow);
                }
                // PREV UP/Bull Renko
                else {
                    if (Set.of("normal","wicks","reverse-wicks","fake-r-wicks").contains(mode)) {
                        renkoMap.get("open").add(reverseHigh);
                    }
                    else if (mode.equals("fake-r-nongap")) {
                        renkoMap.get("open").add(prevCloseHigh);
                    }
                    else {
                        renkoMap.get("open").add(wick);
                    }

                    if (mode.equals("normal")) {
                        renkoMap.get("high").add(reverseHigh);
                    }
                    else if (Set.of("fake-r-nongap","fake-r-wicks").contains(mode)) {
                        renkoMap.get("high").add(prevCloseHigh);
                    }
                    else {
                        renkoMap.get("high").add(wick);
                    }
        		}
                prevCloseLow = price;
        	}
        	
            prevDirection = direction;
		}
        
        // Removing first row
        renkoMap.keySet().stream()
        .forEach(key -> renkoMap.get(key).remove(0));

        List<String> datetime = renkoMap.get("datetime").stream().map(Object::toString).collect(Collectors.toList());
        List<String> open = renkoMap.get("open").stream().map(Object::toString).collect(Collectors.toList());
        List<String> high = renkoMap.get("high").stream().map(Object::toString).collect(Collectors.toList());
        List<String> low = renkoMap.get("low").stream().map(Object::toString).collect(Collectors.toList());
        List<String> close = renkoMap.get("close").stream().map(Object::toString).collect(Collectors.toList());
        List<String> volume = renkoMap.get("volume").stream().map(x -> Double.valueOf(x.toString()).toString()).collect(Collectors.toList());

        Map<String, List<String>> renkoStringMap = new LinkedHashMap<>();
		renkoStringMap.put("datetime", datetime);
		renkoStringMap.put("open", open);
		renkoStringMap.put("high", high);
		renkoStringMap.put("low", low);
		renkoStringMap.put("close", close);
		renkoStringMap.put("volume", volume);
		
        return renkoStringMap;
	}
}