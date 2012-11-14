package com.sergiandreplace.inflatertest;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getLayoutInflater().setFactory(new FontInflaterFactory());
		setContentView(R.layout.activity_main);
		
	}

	
}
