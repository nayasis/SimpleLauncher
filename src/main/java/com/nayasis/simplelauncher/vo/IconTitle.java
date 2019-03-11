package com.nayasis.simplelauncher.vo;

import javafx.scene.image.Image;

public class IconTitle {

	private Image   icon;
	private String  title;
	
	public IconTitle( Image icon, String title ) {
		this.icon  = icon;
		this.title = title;
	}
	
	public Image getIcon() {
		return icon;
	}
	
	public String getTitle() {
		return title;
	}
	
}
