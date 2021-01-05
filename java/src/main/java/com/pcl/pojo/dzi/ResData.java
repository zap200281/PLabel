package com.pcl.pojo.dzi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResData {
	@JsonProperty(value="xmlns")
    private String xmlns;
	
	@JsonProperty(value="Url")
    private String url;
	
	@JsonProperty(value="Overlap")
    private String overlap;
    
	@JsonProperty(value="TileSize")
    private String tileSize;
	
	@JsonProperty(value="Format")
    private String format;
	
	@JsonProperty(value="Size")
    private SizeData size;

	public String getXmlns() {
		return xmlns;
	}

	public void setXmlns(String xmlns) {
		this.xmlns = xmlns;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getOverlap() {
		return overlap;
	}

	public void setOverlap(String overlap) {
		this.overlap = overlap;
	}

	public String getTileSize() {
		return tileSize;
	}

	public void setTileSize(String tileSize) {
		this.tileSize = tileSize;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public SizeData getSize() {
		return size;
	}

	public void setSize(SizeData size) {
		this.size = size;
	}
 
  
}
