package renkodf.wrappers;

public class RSD {
	
	private Integer originalIndex;
	private Object date;
	private Double price;
	private Double direction;
	private Double wick;
	private Double volume;
	
	public RSD() {}
	
	public RSD(Integer originalIndex, Object date, Double price, Double direction, Double wick, Double volume) {
		super();
		this.originalIndex = originalIndex;
		this.date = date;
		this.price = price;
		this.direction = direction;
		this.wick = wick;
		this.volume = volume;
	}
	
	public Integer getOriginalIndex() {
		return originalIndex;
	}
	public Object getDate() {
		return date;
	}
	public Double getPrice() {
		return price;
	}
	public Double getDirection() {
		return direction;
	}
	public Double getWick() {
		return wick;
	}
	public Double getVolume() {
		return volume;
	}
	
	public void setOriginalIndex(Integer originalIndex) {
		this.originalIndex = originalIndex;
	}
	public void setDate(Object date) {
		this.date = date;
	}
	public void setPrice(Double price) {
		this.price = price;
	}
	public void setDirection(Double direction) {
		this.direction = direction;
	}
	public void setWick(Double wick) {
		this.wick = wick;
	}
	public void setVolume(Double volume) {
		this.volume = volume;
	}
	
}
