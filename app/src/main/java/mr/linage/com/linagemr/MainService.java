package mr.linage.com.linagemr;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import mr.linage.com.utils.AndroidUtils;
import mr.linage.com.vo.ArgbVo;

public class MainService extends Service {
	private TextView mPopupView;							//항상 보이게 할 뷰
	private WindowManager.LayoutParams mParams;		//layout params 객체. 뷰의 위치 및 크기를 지정하는 객체
	private WindowManager mWindowManager;			//윈도우 매니저
	private SeekBar mSeekBar;								//투명도 조절 seek bar
	private LinearLayout parentPP;
	private LinearLayout parentLL;
	private LinearLayout parentRR;
	private Button btn_cnt;								//단위 이동 버튼
	private Button btn_num;								//단위 이동 버튼
	private Button btn_up;								//위 이동 버튼
	private Button btn_down;								//아래 이동 버튼
	private Button btn_left;								//왼쪽 이동 버튼
	private Button btn_right;								//오른쪽 이동 버튼
	int cnt = 1;
	int rank = 1;
	int x = 54;
	int y = 180;

	private float START_X, START_Y;							//움직이기 위해 터치한 시작 점
	private int PREV_X, PREV_Y;								//움직이기 이전에 뷰가 위치한 점
	private int MAX_X = -1, MAX_Y = -1;					//뷰의 위치 최대 값

	private OnTouchListener mViewTouchListener = new OnTouchListener() {
		@Override public boolean onTouch(View v, MotionEvent event) {
			switch(event.getAction()) {
				case MotionEvent.ACTION_DOWN:				//사용자 터치 다운이면
					if(MAX_X == -1)
						setMaxPosition();
					START_X = event.getRawX();					//터치 시작 점
					START_Y = event.getRawY();					//터치 시작 점
					PREV_X = mParams.x;							//뷰의 시작 점
					PREV_Y = mParams.y;							//뷰의 시작 점
					break;
				case MotionEvent.ACTION_MOVE:
					int x = (int)(event.getRawX() - START_X);	//이동한 거리
					int y = (int)(event.getRawY() - START_Y);	//이동한 거리

					//터치해서 이동한 만큼 이동 시킨다
					mParams.x = PREV_X + x;
					mParams.y = PREV_Y + y;

					optimizePosition();		//뷰의 위치 최적화
					mWindowManager.updateViewLayout(mPopupView, mParams);	//뷰 업데이트
					break;
			}

			return true;
		}
	};

	@Override
	public IBinder onBind(Intent arg0) {
		return mMessenger.getBinder();
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mPopupView = new TextView(this);																//뷰 생성
		mPopupView.setText("이 뷰는 항상 위에 있다.\n갤럭시 & 옵티머스 팝업 뷰와 같음");	//텍스트 설정
		mPopupView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);								//텍스트 크기 18sp
		mPopupView.setTextColor(Color.BLUE);															//글자 색상
		mPopupView.setBackgroundColor(Color.argb(127, 0, 255, 255));								//텍스트뷰 배경 색

		mPopupView.setOnTouchListener(mViewTouchListener);										//팝업뷰에 터치 리스너 등록

		//최상위 윈도우에 넣기 위한 설정
		mParams = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,					//항상 최 상위에 있게. status bar 밑에 있음. 터치 이벤트 받을 수 있음.
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,		//이 속성을 안주면 터치 & 키 이벤트도 먹게 된다.
				//포커스를 안줘서 자기 영역 밖터치는 인식 안하고 키이벤트를 사용하지 않게 설정
				PixelFormat.TRANSLUCENT);										//투명
		mParams.gravity = Gravity.LEFT | Gravity.TOP;						//왼쪽 상단에 위치하게 함.

		mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);	//윈도우 매니저 불러옴.
//		mWindowManager.addView(mPopupView, mParams);		//최상위 윈도우에 뷰 넣기. *중요 : 여기에 permission을 미리 설정해 두어야 한다. 매니페스트에

		addOpacityController();		//팝업 뷰의 투명도 조절하는 컨트롤러 추가
		addMoveController();
	}

	/**
	 * 뷰의 위치가 화면 안에 있게 최대값을 설정한다
	 */
	private void setMaxPosition() {
		DisplayMetrics matrix = new DisplayMetrics();
		mWindowManager.getDefaultDisplay().getMetrics(matrix);		//화면 정보를 가져와서

		MAX_X = matrix.widthPixels - mPopupView.getWidth();			//x 최대값 설정
		MAX_Y = matrix.heightPixels - mPopupView.getHeight();			//y 최대값 설정
	}

	/**
	 * 뷰의 위치가 화면 안에 있게 하기 위해서 검사하고 수정한다.
	 */
	private void optimizePosition() {
		//최대값 넘어가지 않게 설정
		if(mParams.x > MAX_X) mParams.x = MAX_X;
		if(mParams.y > MAX_Y) mParams.y = MAX_Y;
		if(mParams.x < 2) mParams.x = 2;
		if(mParams.y < 2) mParams.y = 2;

//		sendMsgToActivity(mParams.x-1, mParams.y-1);
	}

	/**
	 * 알파값 조절하는 컨트롤러를 추가한다
	 */
	private void addOpacityController() {
		mSeekBar = new SeekBar(this);		//투명도 조절 seek bar
		mSeekBar.setMax(100);					//맥스 값 설정.
		mSeekBar.setProgress(100);			//현재 투명도 설정. 100:불투명, 0은 완전 투명
		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override public void onStopTrackingTouch(SeekBar seekBar) {}
			@Override public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override public void onProgressChanged(SeekBar seekBar, int progress,	boolean fromUser) {
				mParams.alpha = progress / 100.0f;			//알파값 설정
				mWindowManager.updateViewLayout(mPopupView, mParams);	//팝업 뷰 업데이트
			}
		});

		//최상위 윈도우에 넣기 위한 설정
		WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,					//항상 최 상위에 있게. status bar 밑에 있음. 터치 이벤트 받을 수 있음.
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,		//이 속성을 안주면 터치 & 키 이벤트도 먹게 된다.
				//포커스를 안줘서 자기 영역 밖터치는 인식 안하고 키이벤트를 사용하지 않게 설정
				PixelFormat.TRANSLUCENT);										//투명
		params.gravity = Gravity.LEFT | Gravity.TOP;							//왼쪽 상단에 위치하게 함.

//		mWindowManager.addView(mSeekBar, params);
	}

	/**
	 * 뷰이동 버튼 추가
	 */
	private void addMoveController() {

		if(MAX_X == -1)
			setMaxPosition();

		btn_cnt = new Button(this);		//투명도 조절 seek bar
		btn_cnt.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtils.PixelFromDP(100,this),AndroidUtils.PixelFromDP(100,this)));
		btn_cnt.setText(cnt+"");
		btn_cnt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(cnt==1) {
					cnt = 5;
				} else {
					cnt += 5;
				}
				if(cnt>10) {
					cnt = 1;
				}
				btn_cnt.setText(cnt+"");
			}
		});

		btn_num = new Button(this);		//투명도 조절 seek bar
		btn_num.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtils.PixelFromDP(100,this),AndroidUtils.PixelFromDP(100,this)));
		btn_num.setText(rank+"");
		btn_num.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				rank++;
				if(rank>4) {
					rank = 1;
				}
				if(rank==1) {
					x = 54;
					y = 180;
				}
				if(rank==2) {
					x = 54;
					y = 225;
				}
				if(rank==3) {
					x = 54;
					y = 270;
				}
				if(rank==4) {
					x = 54;
					y = 314;
				}
				sendMsgToActivity(x, y);
				btn_num.setText(rank+"");
			}
		});

		btn_up = new Button(this);		//투명도 조절 seek bar
		btn_up.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtils.PixelFromDP(100,this),AndroidUtils.PixelFromDP(100,this)));
		btn_up.setText("위");
		btn_up.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//터치해서 이동한 만큼 이동 시킨다
				mParams.y -= 1;

//				optimizePosition();		//뷰의 위치 최적화
//				mWindowManager.updateViewLayout(mPopupView, mParams);	//뷰 업데이트

				y -= cnt;

				if(y<0) {
					y = 0;
				}

				sendMsgToActivity(x, y);
			}
		});

		btn_down = new Button(this);		//투명도 조절 seek bar
		btn_down.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtils.PixelFromDP(100,this),AndroidUtils.PixelFromDP(100,this)));
		btn_down.setText("아래");
		btn_down.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//터치해서 이동한 만큼 이동 시킨다
				mParams.y += 1;

//				optimizePosition();		//뷰의 위치 최적화
//				mWindowManager.updateViewLayout(mPopupView, mParams);	//뷰 업데이트

				y += cnt;

				sendMsgToActivity(x, y);
			}
		});

		btn_left = new Button(this);		//투명도 조절 seek bar
		btn_left.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtils.PixelFromDP(100,this),AndroidUtils.PixelFromDP(100,this)));
		btn_left.setText("좌");
		btn_left.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//터치해서 이동한 만큼 이동 시킨다
				mParams.x -= 1;

//				optimizePosition();		//뷰의 위치 최적화
//				mWindowManager.updateViewLayout(mPopupView, mParams);	//뷰 업데이트

				x -= cnt;

				if(x<0) {
					x = 0;
				}

				sendMsgToActivity(x, y);
			}
		});

		btn_right = new Button(this);		//투명도 조절 seek bar
		btn_right.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtils.PixelFromDP(100,this),AndroidUtils.PixelFromDP(100,this)));
		btn_right.setText("우");
		btn_right.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//터치해서 이동한 만큼 이동 시킨다
				mParams.x += 1;

//				optimizePosition();		//뷰의 위치 최적화
//				mWindowManager.updateViewLayout(mPopupView, mParams);	//뷰 업데이트

				x += cnt;

				sendMsgToActivity(x, y);
			}
		});

		parentLL = new LinearLayout(this);
		parentLL.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		parentLL.setOrientation(LinearLayout.HORIZONTAL);
		parentLL.addView(btn_up);
		parentLL.addView(btn_down);
		parentLL.addView(btn_left);
		parentLL.addView(btn_right);

		parentRR = new LinearLayout(this);
		parentRR.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		parentRR.setOrientation(LinearLayout.HORIZONTAL);
		parentRR.addView(btn_num);
		parentRR.addView(btn_cnt);

		parentPP = new LinearLayout(this);
		parentPP.setLayoutParams(new LinearLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT));
		parentPP.setOrientation(LinearLayout.VERTICAL);
		parentPP.setGravity(Gravity.CENTER);
		parentPP.addView(parentLL);
		parentPP.addView(parentRR);

		//최상위 윈도우에 넣기 위한 설정
		WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,					//항상 최 상위에 있게. status bar 밑에 있음. 터치 이벤트 받을 수 있음.
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,		//이 속성을 안주면 터치 & 키 이벤트도 먹게 된다.
				//포커스를 안줘서 자기 영역 밖터치는 인식 안하고 키이벤트를 사용하지 않게 설정
				PixelFormat.TRANSLUCENT);										//투명

		params.gravity = Gravity.CENTER;							//우측 하단에 위치하게 함.

		mWindowManager.addView(parentPP, params);
	}

	/**
	 * 가로 / 세로 모드 변경 시 최대값 다시 설정해 주어야 함.
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		setMaxPosition();		//최대값 다시 설정
		optimizePosition();		//뷰 위치 최적화
	}

	@Override
	public void onDestroy() {
		if(mWindowManager != null) {		//서비스 종료시 뷰 제거. *중요 : 뷰를 꼭 제거 해야함.
//			if(mPopupView != null) mWindowManager.removeView(mPopupView);
//			if(mSeekBar != null) mWindowManager.removeView(mSeekBar);
			if(parentPP != null) mWindowManager.removeView(parentPP);
		}
		super.onDestroy();
	}

	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_SEND_TO_SERVICE = 3;
	public static final int MSG_SEND_TO_ACTIVITY = 4;
	private Messenger mClient = null;   // Activity 에서 가져온 Messenger

	/** activity로부터 binding 된 Messenger */
	private final Messenger mMessenger = new Messenger(new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			Log.w("test","ControlService - message what : "+msg.what +" , msg.obj "+ msg.obj);
			switch (msg.what) {
				case MSG_SEND_TO_SERVICE:
					if(msg.obj instanceof ArgbVo) {
						if(msg.arg1==1) {
							ArgbVo argbVo = (ArgbVo)msg.obj;
							Log.w("test","msg.arg1:"+msg.arg1+" "+"argbVo : A:"+ argbVo.getA()+" "+"R:"+argbVo.getR()+" "+"G:"+argbVo.getG()+" "+"B:"+argbVo.getB());
							btn_up.setBackgroundColor(Color.TRANSPARENT);
							btn_up.setBackgroundColor(Color.argb(255,argbVo.getR(),argbVo.getG(),argbVo.getB()));
						} else if(msg.arg1==2) {
							ArgbVo argbVo = (ArgbVo)msg.obj;
							Log.w("test","msg.arg1:"+msg.arg1+" "+"argbVo : A:"+ argbVo.getA()+" "+"R:"+argbVo.getR()+" "+"G:"+argbVo.getG()+" "+"B:"+argbVo.getB());
							btn_down.setBackgroundColor(Color.TRANSPARENT);
							btn_down.setBackgroundColor(Color.argb(255,argbVo.getR(),argbVo.getG(),argbVo.getB()));
						} else if(msg.arg1==3) {
							ArgbVo argbVo = (ArgbVo)msg.obj;
							Log.w("test","msg.arg1:"+msg.arg1+" "+"argbVo : A:"+ argbVo.getA()+" "+"R:"+argbVo.getR()+" "+"G:"+argbVo.getG()+" "+"B:"+argbVo.getB());
							btn_left.setBackgroundColor(Color.TRANSPARENT);
							btn_left.setBackgroundColor(Color.argb(255,argbVo.getR(),argbVo.getG(),argbVo.getB()));
						} else if(msg.arg1==4) {
							ArgbVo argbVo = (ArgbVo)msg.obj;
							Log.w("test","msg.arg1:"+msg.arg1+" "+"argbVo : A:"+ argbVo.getA()+" "+"R:"+argbVo.getR()+" "+"G:"+argbVo.getG()+" "+"B:"+argbVo.getB());
							btn_right.setBackgroundColor(Color.TRANSPARENT);
							btn_right.setBackgroundColor(Color.argb(255,argbVo.getR(),argbVo.getG(),argbVo.getB()));
						}
					}
					mClient = msg.replyTo;  // activity로부터 가져온
					break;
			}
			return false;
		}
	}));

	private void sendMsgToActivity(int x, int y) {
		try {
			Bundle bundle = new Bundle();
			bundle.putInt("x", x);
			bundle.putInt("y", y);
			bundle.putInt("rank", rank);
			Message msg = Message.obtain(null, MSG_SEND_TO_ACTIVITY);
			msg.setData(bundle);
			mClient.send(msg);      // msg 보내기
		} catch (RemoteException e) {
		}
	}
}