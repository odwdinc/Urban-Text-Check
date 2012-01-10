package com.isitbroken.textcheck;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.Editable;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;

public class TextCheckActivity extends Activity implements OnInitListener {
	private static final int MY_DATA_CHECK_CODE = 00101010;

	private static String lookupsentance(String corectword, View v)
			throws MalformedURLException, IOException {
		String sentence = "http://www.urbandictionary.com/define.php?term="
				+ corectword;

		CleanerProperties props = new CleanerProperties();
		// set some properties to non-default values
		// props.setTranslateSpecialEntities(true);
		// props.setTransResCharsToNCR(true);
		// props.setOmitComments(true);
		// do parsing
		TagNode Node = new HtmlCleaner(props).clean(new URL(sentence));
		String outtext = "";
		List<TagNode> deflist = Node.getElementListByAttValue("class",
				"definition", true, false);

		if (deflist.size() > 0) {
			if (deflist.get(0).hasChildren()) {
				List childrenlist = deflist.get(0).getChildren();
				for (int ch = 0; ch < childrenlist.size(); ch++) {
					Object thischild = deflist.get(0).getChildren().get(ch);
					if (thischild instanceof ContentNode) {
						ContentNode text = (ContentNode) thischild;
						outtext += text.getContent().toString();
					}
				}
				return outtext;
			}
		}

		return "Dont know that one";
	}

	private TextToSpeech mTts;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == MY_DATA_CHECK_CODE) {
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				// success, create the TTS instance
				mTts = new TextToSpeech(this, this);
			} else {
				// missing data, install it
				Intent installIntent = new Intent();
				installIntent
						.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installIntent);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		if (item.getTitle() == "Share") {
			int ID = item.getItemId();
			RadioButton thisbutton = (RadioButton) TextCheckActivity.this
					.findViewById(ID);
			Sharing(thisbutton);
		} else if (item.getTitle() == "Speak") {
			int ID = item.getItemId();
			RadioButton thisbutton = (RadioButton) TextCheckActivity.this
					.findViewById(ID);
			Speeking((String) thisbutton.getText());
		} else {
			return false;
		}
		return true;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		Intent i = new Intent();
		i.setClassName("com.isitbroken.textcheck",
				"com.isitbroken.textcheck.splash");
		startActivity(i);

		Intent checkIntent = new Intent();
		checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);

		final Button spellbutton = (Button) findViewById(R.id.Check);
		final EditText testword = (EditText) findViewById(R.id.word);

		final Button clearbut = (Button) findViewById(R.id.button1);
		final LinearLayout returnlost = (LinearLayout) findViewById(R.id.ReturnList);

		clearbut.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				returnlost.removeAllViewsInLayout();
				testword.setText("");
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(testword, 0);
			}
		});

		testword.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
					return spellbutton.performClick();
				}
				return false;
			}
		});

		spellbutton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View arg0) {
				final ProgressDialog spellpd = ProgressDialog.show(
						TextCheckActivity.this, "Thinking..",
						"Checking the Spelling aspell.net");

				final Handler spellhandler = new Handler() {
					@Override
					public void handleMessage(Message msg) {
						spellpd.dismiss();
					}
				};


				final Editable wordtest = testword.getText();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(testword.getWindowToken(),
						0);
				returnlost.removeAllViewsInLayout();
				arg0.postDelayed(new Runnable() {
					@Override
					public void run() {
					URLtagtostring(wordtest.toString(),
							"aspell-def", returnlost);
							spellhandler.sendEmptyMessage(0);
					}

				},1000);

			}
		});
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle("Menu");
		menu.add(0, v.getId(), 0, "Share");
		if (mTts != null) {
			menu.add(0, v.getId(), 0, "Speak");
		}
	}

	@Override
	public void onInit(int arg0) {

	}

	public void Sharing(RadioButton thisbutton) {
		Intent i = new Intent(android.content.Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_SUBJECT, R.string.app_name);
		i.putExtra(Intent.EXTRA_TEXT, thisbutton.getText());
		startActivity(Intent.createChooser(i, "Sharing Menu"));
		thisbutton.performClick();
	}

	private void Speeking(String text) {
		mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null);

	}

	protected void URLtagtostring(String wordcheck, final String token,
			final LinearLayout returnlost) {
		final CleanerProperties props = new CleanerProperties();
		final String Url = "http://suggest.aspell.net/index.php?spelling=american&dict=normal&sugmode=bad-spellers&word="
				+ wordcheck;

		props.setTranslateSpecialEntities(true);
		props.setTransResCharsToNCR(true);
		props.setOmitComments(true);
		// do parsing
		TagNode Node;
		try {
			Node = new HtmlCleaner(props).clean(new URL(Url));

			List deflist = Node.getElementListByAttValue("target", token, true,
					false);

			for (int i = 0; i < deflist.size(); i++) {

				if (deflist.get(i) instanceof TagNode) {
					TagNode thistag = (TagNode) deflist.get(i);
					if (thistag.hasChildren()) {
						List childrenlist = thistag.getChildren();
						Object thischild = childrenlist.get(0);
						if (thischild instanceof ContentNode) {
							ContentNode thisnode = (ContentNode) thischild;
							final RadioButton spelltest = new RadioButton(
									returnlost.getContext());
							spelltest.setBackgroundColor(R.color.bacgroudtab);
							final String corectword = thisnode.getContent()
									.toString();

							spelltest.setText(corectword);
							spelltest.setId(i);
							registerForContextMenu(spelltest);

							spelltest.setOnClickListener(new OnClickListener() {
								private String thisstring;
								private boolean thisvsl;

								@Override
								public void onClick(final View v) {
									final String thisword = corectword;
									final RadioButton ID = (RadioButton) v;
									if (!thisvsl) {
										if (thisstring == null) {
											final ProgressDialog pd = ProgressDialog
													.show(TextCheckActivity.this,
															"Thinking..",
															"Checking the Urban Dictionary.");
											final Handler handler = new Handler() {
												@Override
												public void handleMessage(
														Message msg) {
													pd.dismiss();
													ID.setText(thisstring);
													thisvsl = true;
												}
											};
											Thread checkUpdate = new Thread() {
												@Override
												public void run() {
													try {
														thisstring = thisword
																+ ":\n"
																+ lookupsentance(
																		thisword,
																		v)
																+ "\n";
													} catch (MalformedURLException e) {
														e.printStackTrace();
													} catch (IOException e) {
														e.printStackTrace();
													}
													handler.sendEmptyMessage(0);
												}
											};
											checkUpdate.start();

										}
										ID.setText(thisstring);
										thisvsl = true;
									} else {
										ID.setText(thisword + ":");
										thisvsl = false;
										ID.setChecked(false);
									}

								}
							});
							returnlost.addView(spelltest);
						}
					}
				}
			}

		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}