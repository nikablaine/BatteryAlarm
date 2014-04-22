package de.kraflapps.apps.batteryalarm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;

public class NumberPickerFragment extends DialogFragment {

	public static final String CUR_NUMBER = "de.kraflapps.apps.batteryalarm.cur_number";
	public static final String MAX_NUMBER = "de.kraflapps.apps.batteryalarm.max_number";
	public static final String MIN_NUMBER = "de.kraflapps.apps.batteryalarm.min_number";
	
	//using interface to send results of a choice
	public AsyncResult delegate;

	private int mCurNumber;
	private int mMaxNumber;
	private int mMinNumber;
	
	public static NumberPickerFragment newInstance(int curNumber,
			int maxNumber, int minNumber) {
		final Bundle args = new Bundle();
		args.putInt(CUR_NUMBER, curNumber);
		args.putInt(MAX_NUMBER, maxNumber);
		args.putInt(MIN_NUMBER, minNumber);

		NumberPickerFragment fragment = new NumberPickerFragment();
		fragment.setArguments(args);

		return fragment;
	}

	private void sendResult() {
		if (getActivity() == null) {
			return;
		}

		setDelegate((MainActivity) getActivity());
		getDelegate().numPickerFragmentAlarmValueChosen(getmCurNumber());
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		mCurNumber = getArguments().getInt(CUR_NUMBER);
		mMaxNumber = getArguments().getInt(MAX_NUMBER);
		mMinNumber = getArguments().getInt(MIN_NUMBER);

		View v = getActivity().getLayoutInflater().inflate(
				R.layout.dialog_number, null);

		NumberPicker numberPicker = (NumberPicker) v
				.findViewById(R.id.dialog_number_numberPicker);
		numberPicker.setMinValue(mMinNumber);
		numberPicker.setMaxValue(mMaxNumber);
		numberPicker.setValue(mCurNumber);
		numberPicker.setOnValueChangedListener(new OnValueChangeListener() {

			@Override
			public void onValueChange(NumberPicker picker, int oldVal,
					int newVal) {
				mCurNumber = picker.getValue();
				getArguments().putInt(CUR_NUMBER, mCurNumber);

			}
		});

		return new AlertDialog.Builder(getActivity())
				.setView(v)
				.setTitle(R.string.string_set_new_critical_level)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								sendResult();

							}
						}).create();
	}

	public int getmCurNumber() {
		return mCurNumber;
	}

	public void setmCurNumber(final int mCurNumber) {
		this.mCurNumber = mCurNumber;
	}

	public int getmMaxNumber() {
		return mMaxNumber;
	}

	public void setmMaxNumber(final int mMaxNumber) {
		this.mMaxNumber = mMaxNumber;
	}

	public int getmMinNumber() {
		return mMinNumber;
	}

	public void setmMinNumber(final int mMinNumber) {
		this.mMinNumber = mMinNumber;
	}

	public AsyncResult getDelegate() {
		return delegate;
	}

	public void setDelegate(AsyncResult delegate) {
		this.delegate = delegate;
	}
	
	

}
