package com.duckduckgo.mobile.android.views;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.duckduckgo.mobile.android.R;

public class WelcomeScreenView extends LinearLayout {
	
	Context context;
	
	ImageView closeWelcomeBut;
	Button getStartedBut;
	
	OnClickListener closeListener;
	
	public WelcomeScreenView(Context context) {
		super(context);
		this.context = context;
		initView(context);
	}

	public WelcomeScreenView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
		initView(context);	
	}
	
	private void initView(Context context) {
		LayoutInflater.from(context).inflate(R.layout.welcome, this, true);		
		closeWelcomeBut = (ImageView) findViewById(R.id.closeWelcomeBut);
    	getStartedBut = (Button) findViewById(R.id.getStartedBut); 
	}
	
	public void setOnCloseListener(OnClickListener closeListener) {
		this.closeListener = closeListener;
		if(closeListener != null) {
			closeWelcomeBut.setOnClickListener(closeListener);
	    	getStartedBut.setOnClickListener(closeListener);
		}
	}

}
