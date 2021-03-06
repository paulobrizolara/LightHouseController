package com.example.lighthousecontroller.view;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.lighthousecontroller.R;
import com.example.lighthousecontroller.controller.ConsumptionObserver;
import com.example.lighthousecontroller.controller.LampController;
import com.example.lighthousecontroller.controller.LampMonitor;
import com.example.lighthousecontroller.controller.LampObserver;
import com.example.lighthousecontroller.model.ConsumptionEvent;
import com.example.lighthousecontroller.model.Lamp;

public class LampDetailsFragment extends Fragment 
								 implements ConsumptionObserver
								 			, LampObserver
								 			, OnClickListener
{
	public class BrightControlChanged implements OnSeekBarChangeListener {
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			if(lamp != null){
				LampController.getInstance()
				  .requestChangeBright(lamp, seekBar.getProgress() /((float)seekBar.getMax()));
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) 
		{
			//Do nothing
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			//Do nothing
		}
	}
	private static final float MIN_ICON_ALPHA = 0.3f;

	private Lamp lamp;
	
	private ImageView lampIconView;
	private TextView nameView;
	private ToggleButton powerControl;
	private SeekBar brightControl;
	private OnSeekBarChangeListener userChangeBrightListener;
	
	private ConsumptionGraphFragment consumptionGraphFragment;
	
	private boolean viewsReady;
	
	public LampDetailsFragment() {
		Log.d(getClass().getName(), "Criando fragmento! " + this.toString());
		consumptionGraphFragment = new ConsumptionGraphFragment();
		userChangeBrightListener = new BrightControlChanged();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_lamp_details,
				container, false);
		
		setupViews(rootView);
		updateView();
		
		return rootView;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if(lamp != null && lamp.getId() > 0){
			LampController.getInstance().addConsumptionObserver(this, lamp.getId());
			LampController.getInstance().addLampObserver(this, lamp.getId());
			Lamp readedLamp = LampController.getInstance().getLampStatus(lamp);
			startLampMonitor(lamp);
			if(readedLamp != null){
				this.lamp.set(readedLamp);
			}
			else{
				//TODO: notificar erro (?)
			}
			updateView();
		}
	}
	@Override
	public void onPause() {
		super.onPause();
		LampController.getInstance().removeConsumptionObserver(this, lamp.getId());
		LampController.getInstance().removeLampObserver(this, lamp.getId());
		stopLampMonitor(lamp);
	}
	
	private void startLampMonitor(Lamp lamp) {
		Intent intent = new Intent(getActivity(), LampMonitor.class);
		intent.setAction(LampMonitor.BEGIN_OBSERVE_LAMP);
		intent.putExtra(LampMonitor.LAMPID_DATA, lamp.getId());
		getActivity().startService(intent);
	}
	private void stopLampMonitor(Lamp lamp) {
		Intent intent = new Intent(getActivity(), LampMonitor.class);
		intent.setAction(LampMonitor.STOP_OBSERVE_LAMP);
		intent.putExtra(LampMonitor.LAMPID_DATA, lamp.getId());
		getActivity().startService(intent);
	}

	public Lamp getLamp() {
		return lamp;
	}
	public void setLamp(Lamp lamp) {		
		this.lamp = lamp;
		
		assertValidLocalLamp();
		updateView();
	}

	private void setupViews(View rootView) {
		lampIconView  = (ImageView)rootView.findViewById(R.id.lampDetails_iconView);
		powerControl  = (ToggleButton)rootView.findViewById(R.id.lampDetails_powerControl);
		brightControl = (SeekBar)rootView.findViewById(R.id.lampDetails_brightControl);
		nameView      = (TextView)rootView.findViewById(R.id.lampDetails_lampName);
		
		getChildFragmentManager().beginTransaction()
									.add(R.id.lampDetails_graphContainer, consumptionGraphFragment)
									.commit();

//		brightControl.setOnSeekBarChangeListener(new BrightControlChanged());
		brightControl.setOnSeekBarChangeListener(userChangeBrightListener);
		powerControl.setOnClickListener(this);
		
		viewsReady = true;
	}

	/* *********************************** ConsumptionObserver ************************************************/
	
	@Override
	public void onConsumption(List<ConsumptionEvent> consumptionEvents) {
		Toast.makeText(getActivity(), "Received consumptions: " + consumptionEvents, Toast.LENGTH_SHORT).show();
		for(ConsumptionEvent event : consumptionEvents){
			if(lamp !=null && event.getSourceId() == this.lamp.getId()){
				consumptionGraphFragment.plotConsumption(event);
			}
		}
	}

	/* *********************************** LampObserver ************************************************/
	@Override
	public void lampUpdated(Lamp lamp) {
		assertValidLocalLamp();
		assertValidLamp(lamp);
		this.lamp.set(lamp);
		updateLampStatus();
	}

	private void assertValidLocalLamp() {
		if(this.lamp == null){
			throw new RuntimeException("Local lamp is null!" );
		}
		
		if(lamp.getId() <= 0){
			throw new RuntimeException("Local lamp has an invalid id: " + lamp.getId());
		}
	}
	private void assertValidLamp(Lamp lamp) {
		String message = "Invalid lamp value! ";
		if(this.lamp != null && lamp.getId() != this.lamp.getId()){
			throw new RuntimeException(message + "Lamp is not the expected! This id is " + this.lamp.getId()
					+ " and received is: " + lamp.getId());
		}
	}

	/* ************************************* View ***************************************************/
	public void updateView() {
		if(viewsReady){
			updateLampStatus();
			nameView.setText(lamp != null ? lamp.getName() : "");
			consumptionGraphFragment.addConsumptionHistory(
					lamp != null ? lamp.getConsumptionHistory() : null);
		}
	}
	
	private void updateLampStatus() {
		boolean lampIsOn = (lamp != null && lamp.isOn());
		lampIconView.setImageResource( lampIsOn? R.drawable.ic_lamp_on : R.drawable.ic_lamp_off);
		
		//Desativa listener para evitar que requisição seja feita novamente
		brightControl.setOnSeekBarChangeListener(null);
		
		powerControl.setChecked(lampIsOn);
		if(!lampIsOn){
			brightControl.setProgress(0);
		}
		else{
			brightControl.setProgress((int)(lamp.getBright()*brightControl.getMax()));
			//Define alpha para ícone da imagem baseado no seu brilho
			ViewCompat.setAlpha(lampIconView, (1f - MIN_ICON_ALPHA)*lamp.getBright() + MIN_ICON_ALPHA);
		}

		//Reabilita listener apos alterar interface
		brightControl.setOnSeekBarChangeListener(userChangeBrightListener);
	}
	/* *********************************** OnClickListener **************************************/
	@Override
	public void onClick(View view) {
		if(view == powerControl && lamp != null && lamp.isValid()){
			LampController.getInstance().requestChangePower(lamp, powerControl.isChecked());
		}
	}

}
